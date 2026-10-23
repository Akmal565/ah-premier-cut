package com.ahstudio.face

import android.graphics.Bitmap
import com.ahstudio.face.core.DetectionSource
import com.ahstudio.face.core.FaceTrackingConfig
import com.ahstudio.face.core.FaceTrackingEngine
import com.ahstudio.face.core.FaceTrackingResult
import com.ahstudio.face.detection.BoundsPx
import com.ahstudio.face.detection.DetectionFrame
import com.ahstudio.face.detection.FaceDetector
import com.ahstudio.face.detection.FrameProvider
import com.ahstudio.face.detection.RawFaceDetection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

/** Reports a face centered in whatever bitmap it was given, exactly like ML Kit would. */
private open class FakeDetector(private val sleepMs: Long = 0) : FaceDetector {
    val calls = AtomicInteger(0)
    val seenFrameSizes = java.util.concurrent.ConcurrentLinkedQueue<Pair<Int, Int>>()

    override suspend fun detect(frame: DetectionFrame, maxFaces: Int): List<RawFaceDetection> {
        calls.incrementAndGet()
        if (sleepMs > 0) Thread.sleep(sleepMs)
        val bmp = (frame as DetectionFrame.BitmapFrame).bitmap
        seenFrameSizes.add(bmp.width to bmp.height)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        return listOf(
            RawFaceDetection(
                boundsPx = BoundsPx(w * 0.4f, h * 0.3f, w * 0.6f, h * 0.7f),
                eulerX = 0f, eulerY = 0f, eulerZ = 0f,
                landmarksPx = emptyMap(),
                smiling = null, leftEyeOpen = null, rightEyeOpen = null,
                trackingId = null,
            )
        )
    }

    override fun close() {}
}

/**
 * Mimics RetrieverFrameProvider + the platform getScaledFrameAtTime semantics: the detection
 * bitmap is an aspect-fit downscale of the upright frame (a 1920x1080 video yields 640x360,
 * per the AOSP MediaMetadataRetriever JNI implementation).
 */
private class FakeFrames(private val w: Int, private val h: Int) : FrameProvider {
    private val scale = 640f / maxOf(w, h)

    override suspend fun frame(clipId: String, sourceTimeUs: Long): DetectionFrame? =
        DetectionFrame.BitmapFrame(
            Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888),
            sourceTimeUs,
        )

    override fun dimensionsOf(clipId: String): Pair<Int, Int> = w to h

    override fun close() {}
}

@RunWith(RobolectricTestRunner::class)
class FaceDetectionPipelineTest {

    private fun waitUntil(timeoutMs: Long = 5000, cond: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!cond() && System.currentTimeMillis() - start < timeoutMs) Thread.sleep(10)
        assertTrue("condition not met within ${timeoutMs}ms", cond())
    }

    /**
     * Regression for the AR-filters positioning bug: detection pixels live in the downscaled
     * bitmap space (640x360 for a 1920x1080 video). Normalizing them against the RAW video dims
     * put a centered face at ~(0.17, 0.17) with a ~3x too small box, so AR art never sat on the
     * face. The engine must normalize against the frame it actually measured.
     */
    @Test
    fun detectionsAreNormalizedInTheBitmapSpaceTheyWereMeasuredIn() = runBlocking {
        val engine = FaceTrackingEngine(FakeDetector(), FakeFrames(1920, 1080), FaceTrackingConfig())
        val result: FaceTrackingResult = engine.resultAt("clip", 0L, 0, false)
        assertEquals(1, result.faces.size)
        assertEquals(640, result.frameWidth)
        assertEquals(360, result.frameHeight)
        val b = result.faces[0].bounds
        assertEquals(0.5f, b.centerX, 0.02f)
        assertEquals(0.5f, b.centerY, 0.02f)
        assertEquals(0.2f, b.width, 0.03f)
        assertEquals(0.4f, b.height, 0.03f)
        engine.shutdown()
    }

    /** Render ticks inside one detection interval must collapse onto one real detection. */
    @Test
    fun renderTimeRequestsInsideOneDetectionIntervalShareOneDetection() = runBlocking {
        val detector = FakeDetector()
        val engine = FaceTrackingEngine(detector, FakeFrames(1280, 720), FaceTrackingConfig())
        // Ten ticks within the same 33ms interval -> exactly one detection.
        repeat(10) { engine.resultAtRenderTime("clip", 10_000L, 0, false) }
        waitUntil { detector.calls.get() >= 1 }
        // A later interval detects again...
        engine.resultAtRenderTime("clip", 33_333L + 5_000L, 0, false)
        waitUntil { detector.calls.get() >= 2 }
        // ...and re-asking the first interval afterwards is a pure cache hit: no third call.
        engine.resultAtRenderTime("clip", 10_001L, 0, false)
        Thread.sleep(200)
        assertEquals(2, detector.calls.get())
        engine.shutdown()
    }

    /**
     * The latest-wins queue must drop stale intermediate requests instead of stacking them:
     * while the detect thread is busy inside one detection, a playback burst across several
     * intervals collapses onto only the newest request.
     */
    @Test
    fun burstOfPlaybackTicksProducesAtMostOneQueuedDetectionPerInterval() = runBlocking {
        val detector = FakeDetector(sleepMs = 60)
        val engine = FaceTrackingEngine(detector, FakeFrames(1280, 720), FaceTrackingConfig())
        // The first tick lands the worker inside a slow detection...
        engine.resultAtRenderTime("clip", 0L, 0, false)
        waitUntil { detector.calls.get() == 1 }
        // ...and while it is busy, a burst straddling three buckets collapses to the newest one.
        for (t in longArrayOf(40_000L, 60_000L, 80_000L, 100_000L)) {
            engine.resultAtRenderTime("clip", t, 0, false)
        }
        waitUntil { detector.calls.get() >= 2 }
        Thread.sleep(250)
        // Bucket 0 (in-flight) + bucket 66_666 (the only survivor of the burst) were detected;
        // the stale 33_333-bucket requests were dropped, never detected.
        assertEquals(2, detector.calls.get())
        // The dropped interval still detects once it is actually asked for afterwards.
        engine.resultAtRenderTime("clip", 34_000L, 0, false)
        waitUntil { detector.calls.get() >= 3 }
        assertEquals(3, detector.calls.get())
        engine.shutdown()
    }

    @Test
    fun quantizeToIntervalSnapsToTheGrid() {
        assertEquals(0L, FaceTrackingEngine.quantizeToInterval(16_000L, 33_333L))
        assertEquals(33_333L, FaceTrackingEngine.quantizeToInterval(33_333L, 33_333L))
        assertEquals(33_333L, FaceTrackingEngine.quantizeToInterval(66_000L, 33_333L))
        assertEquals(66_666L, FaceTrackingEngine.quantizeToInterval(66_666L, 33_333L))
        assertEquals(5L, FaceTrackingEngine.quantizeToInterval(5L, 0L))
        assertEquals(5L, FaceTrackingEngine.quantizeToInterval(5L, -1L))
        assertEquals(0L, FaceTrackingEngine.quantizeToInterval(0L, 33_333L))
    }

    @Test
    fun detectionSourceIsRealDetectorNotSynthetic() = runBlocking {
        val engine = FaceTrackingEngine(FakeDetector(), FakeFrames(640, 640), FaceTrackingConfig())
        val result = engine.resultAt("clip", 0L, 0, false)
        assertEquals(DetectionSource.DETECTOR, result.source)
        assertTrue(result.faces.isNotEmpty())
        engine.shutdown()
    }
}
