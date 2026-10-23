package com.ahstudio.face.core

import com.ahstudio.face.detection.DetectionFrame
import com.ahstudio.face.detection.FaceDetector
import com.ahstudio.face.detection.FrameProvider
import com.ahstudio.face.performance.TrackingCache
import com.ahstudio.face.tracking.MultiFaceTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class FaceTrackingEngine(
    private val detector: FaceDetector,
    private val frames: FrameProvider,
    private val cfg: FaceTrackingConfig,
) {
    private val detectDispatcher: ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor { r -> Thread(r, "face-detect").apply { isDaemon = true } }
            .asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + detectDispatcher)
    private val tracker = MultiFaceTracker(cfg)
    val cache = TrackingCache()

    /**
     * Latest-wins pending request slot for the non-blocking render path. Playback asks for a new
     * timestamp on every compositor frame / preview poll tick; only the newest request matters, so
     * it replaces whatever was queued instead of stacking a backlog of stale detections on the
     * single detect thread (which used to make results arrive seconds late and the filter flicker).
     */
    private class Pending(
        val clipId: String,
        val sourceTimeUs: Long,
        val transformHash: Int,
        val mirrored: Boolean,
    )

    private val pending = AtomicReference<Pending?>()
    private val workerLock = Any()
    private var workerRunning = false

    /**
     * Full pipeline: cache -> detect -> track -> smooth -> cache. Deterministic per timestamp.
     * Always hops to the single detect thread, so the preview worker, [prewarm] and the blocking
     * export paths share the frame provider (plain HashMaps + a non-thread-safe
     * MediaMetadataRetriever) and the ML Kit client safely, never concurrently.
     */
    suspend fun resultAt(clipId: String, sourceTimeUs: Long, transformHash: Int, mirrored: Boolean): FaceTrackingResult =
        withContext(detectDispatcher) {
            cache.get(clipId, transformHash, sourceTimeUs)?.let { return@withContext it }
            val dims = frames.dimensionsOf(clipId) ?: return@withContext FaceTrackingResult.EMPTY
            val frame = frames.frame(clipId, sourceTimeUs) ?: return@withContext FaceTrackingResult.EMPTY
            // ML Kit reports pixel coordinates in the frame it actually received - the downscaled
            // detection bitmap - not in the raw video dimensions. getScaledFrameAtTime preserves
            // aspect (a 1920x1080 video yields a 640x360 bitmap), so normalizing against the raw
            // dims put every face a scale factor too small and off toward the top-left. Normalize
            // against the size of the frame that was really measured.
            var detW = 0
            var detH = 0
            when (frame) {
                is DetectionFrame.BitmapFrame -> { detW = frame.bitmap.width; detH = frame.bitmap.height }
                is DetectionFrame.YuvImageFrame -> { detW = frame.image.width; detH = frame.image.height }
            }
            val frameW = if (detW > 0) detW else dims.first
            val frameH = if (detH > 0) detH else dims.second
            val raw = try { detector.detect(frame, cfg.maxFaces) } catch (t: Throwable) { emptyList() }
            val faces = tracker.update(raw, frameW, frameH, sourceTimeUs, mirrored)
            val result = FaceTrackingResult(sourceTimeUs, frameW, frameH, mirrored, faces, DetectionSource.DETECTOR)
            cache.put(clipId, transformHash, result)
            result
        }

    /**
     * Non-suspending path for the render thread: cache hit, else queue the newest request and let
     * the detect thread pick it up. Never blocks the caller and never returns a fake result.
     */
    fun resultAtRenderTime(clipId: String, sourceTimeUs: Long, transformHash: Int, mirrored: Boolean): FaceTrackingResult {
        // Snap to the detection-interval grid (one slot per video frame at the default 33ms) so
        // render ticks that land inside the same interval reuse one cached detection instead of
        // decoding a fresh frame every tick.
        val t = quantizeToInterval(sourceTimeUs, cfg.detectionIntervalUs)
        cache.get(clipId, transformHash, t)?.let { return it }
        pending.set(Pending(clipId, t, transformHash, mirrored))
        ensureWorker()
        return FaceTrackingResult.EMPTY
    }

    /** One persistent worker on the detect thread; processes only the newest queued request. */
    private fun ensureWorker() {
        synchronized(workerLock) {
            if (workerRunning) return
            workerRunning = true
        }
        scope.launch {
            try {
                while (true) {
                    val req = pending.getAndSet(null) ?: break
                    runCatching { resultAt(req.clipId, req.sourceTimeUs, req.transformHash, req.mirrored) }
                }
            } finally {
                synchronized(workerLock) { workerRunning = false }
                // A request may have arrived between the last getAndSet and the flag reset.
                if (pending.get() != null) ensureWorker()
            }
        }
    }

    /** Blocking path for scrubbing / frame-stepping. */
    fun resultAtBlocking(clipId: String, sourceTimeUs: Long, transformHash: Int,
                         mirrored: Boolean, timeoutMs: Long = 80): FaceTrackingResult = runBlocking {
        withTimeoutOrNull(timeoutMs) { resultAt(clipId, sourceTimeUs, transformHash, mirrored) }
            ?: cache.get(clipId, transformHash, sourceTimeUs, toleranceUs = Long.MAX_VALUE / 2)
            ?: FaceTrackingResult.EMPTY
    }

    /**
     * Export path: waits until the real tracking result for exactly this timestamp exists. Never substitutes
     * a stale/nearest result (that would put the overlay on a wrong position) and never returns a fake one;
     * if tracking cannot deliver in [timeoutMs] it throws so the export fails clearly instead of stalling
     * or drifting.
     */
    fun resultAtStrict(clipId: String, sourceTimeUs: Long, transformHash: Int,
                       mirrored: Boolean, timeoutMs: Long): FaceTrackingResult = runBlocking {
        withTimeoutOrNull(timeoutMs) { resultAt(clipId, sourceTimeUs, transformHash, mirrored) }
            ?: throw IllegalStateException("Face tracking timed out after ${timeoutMs}ms at ${sourceTimeUs / 1000}ms of clip $clipId")
    }

    /** Pre-warm a range. */
    fun prewarm(clipId: String, fromUs: Long, toUs: Long, stepUs: Long, transformHash: Int, mirrored: Boolean) {
        scope.launch {
            var t = fromUs
            while (t <= toUs) { resultAt(clipId, t, transformHash, mirrored); t += stepUs }
        }
    }

    fun onConfigChanged() { tracker.reset(); cache.bumpGeneration() }
    fun onSeek() { tracker.reset() }
    fun shutdown() {
        pending.set(null)
        scope.cancel()
        detectDispatcher.close()
        frames.close()
    }

    companion object {
        /**
         * Floors [timeUs] onto the [intervalUs] grid. Preview positions move continuously while
         * video frames are discrete; snapping requests to the frame grid makes consecutive ticks
         * inside one interval collapse onto the same cache entry. An interval <= 1 us disables it.
         */
        fun quantizeToInterval(timeUs: Long, intervalUs: Long): Long =
            if (intervalUs <= 1L || timeUs <= 0L) timeUs else (timeUs / intervalUs) * intervalUs
    }
}
