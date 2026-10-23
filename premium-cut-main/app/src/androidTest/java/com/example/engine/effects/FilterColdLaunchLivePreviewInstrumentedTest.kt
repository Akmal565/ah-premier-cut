package com.example.engine.effects

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.TextureView
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.domain.model.ChromaKeySettings
import com.example.domain.model.FilterSettings
import com.example.domain.model.FilterType
import com.example.domain.model.Timeline
import com.example.domain.model.VideoAdjustments
import com.example.domain.model.VideoClip
import com.example.engine.composition.gpu.EglCore
import com.example.engine.composition.gpu.HardwareVideoTextureSource
import com.example.engine.composition.gpu.OffscreenSurface
import com.example.engine.effects.media3.LivePreviewLook
import com.example.engine.export.TestMp4Factory
import com.example.engine.playback.VideoPlaybackEngine
import com.example.ui.components.player.PreviewVideoSurfaceRegistry
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.max

/**
 * The reported bug, pinned on a real device/emulator with real GLES:
 *
 *   1. launch the editor and load a video (cold - no other tool is ever opened),
 *   2. open Filters directly and select a strong filter,
 *   3. the preview MUST change before any other tool is touched.
 *
 * The root fix routes explicitly set looks through the GPU compositor (the export renderer,
 * the same path Chroma Key / Effects activate), because the Media3 player path's only success
 * signal is a non-throwing `setVideoEffects` - it never proved a graded frame was presented.
 * So the production contract this test enforces is:
 *
 *   - the FIRST filter tap activates the compositor without any other tool,
 *   - the compositor presents REAL frames (renderedFrames advances on the GL thread),
 *   - changing the selection / a slider re-presents immediately,
 *   - the rendered PIXELS actually change with the selection (API >= 26 capture),
 *   - the effect survives a tool switch and a seek.
 *
 * Every stage is timestamped into `look-verification.txt` so the seven required diagnostics
 * can be compared between a failing and a working trace:
 *
 *   Filters panel opened → Renderer initialized → First valid preview frame available →
 *   Filter state submitted → Effect registered in the active render graph →
 *   GL-thread application completed → Filtered frame presented
 *
 * The test fails at the NAMED stage instead of passing vacuously.
 */
@RunWith(AndroidJUnit4::class)
class FilterColdLaunchLivePreviewInstrumentedTest {

  private lateinit var instrumentation: androidx.test.platform.app.Instrumentation
  private lateinit var appContext: android.content.Context
  private var engine: VideoPlaybackEngine? = null
  private var videoFile: File? = null
  private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

  /** Timestamped diagnostics (elapsedRealtime ms) for the seven required stages. */
  private val stamps = LinkedHashMap<String, Long>()

  private fun stamp(name: String) {
    stamps[name] = SystemClock.elapsedRealtime()
    LookVerificationReport.line("  [diag] $name @ ${stamps[name]}ms")
  }

  @Before
  fun setUp() {
    instrumentation = InstrumentationRegistry.getInstrumentation()
    appContext = instrumentation.targetContext.applicationContext
    AndroidTestContextHolder.context = appContext
    LookVerificationReport.section("Cold-launch filter preview (real device, compositor look routing)")
    LookVerificationReport.line("stamp=${LookVerificationReport.stamp()}")
    LookVerificationReport.line("device GL = ${LookGpuHarness.glVersionString()}")
  }

  @After
  fun tearDown() {
    engine?.let { e -> runOnMain { runCatching { e.release() } } }
    engine = null
    LookVerificationReport.file(appContext)?.let { f ->
      LookVerificationReport.line("report written to ${f.absolutePath} (${f.length()} bytes)")
    }
  }

  // ---------------------------------------------------------------------------------------
  // The exact reported sequence: frame-presentation proof (runs on every API level).
  // ---------------------------------------------------------------------------------------

  @Test
  fun coldLaunch_filterTap_activatesCompositorAndPresentsFrames_withoutAnyOtherTool() {
    val file = writeTestVideo()
    stamp("Filters panel opened (cold editor, no other tool opened)")

    // 1. Launch the editor: create the engine and load the video (the production cold order).
    val baseTimeline = Timeline(videoClips = listOf(newClip(file)))
    val engineRef = AtomicReference<VideoPlaybackEngine>()
    runOnMain {
      val e = VideoPlaybackEngine(
        context = appContext,
        onTimelinePositionChanged = {},
        onPlaybackEnded = {},
        onPlaybackInterrupted = {}
      )
      engineRef.set(e)
      e.updateTimeline(baseTimeline)
    }
    engine = engineRef.get()
    stamp("Renderer initialized (engine + ExoPlayer created, timeline loaded)")

    // 2. The user's preview surface: bind a real TextureView the same way EditorScreen does,
    //    and prove the player presents the unfiltered video first.
    val frameCount = AtomicInteger(0)
    val surfaceReady = bindPreviewSurface(engine!!, frameCount)
    assertTrue(
      "Failing stage: Renderer initialized — the preview TextureView could not be bound",
      surfaceReady
    )
    val firstFrame = awaitCondition(timeoutMs = 8_000) { frameCount.get() > 0 }
    stamp("First valid preview frame available (player frame callbacks=$firstFrame)")
    assertTrue(
      "Failing stage: First valid preview frame available — no frame was presented while paused " +
        "(error=${readOnMain { it.playerError.value }})",
      firstFrame
    )
    awaitReady(engine!!, timeoutMs = 10_000)
    assertTrue(
      "Failing stage: Renderer initialized — the compositor must be OFF on a cold look-free project",
      readOnMain { it.compositorActive.value } == false
    )

    // 3. Open Filters directly (NO Chroma Key / Effects / Adjust first) and select a strong
    //    filter. The look alone must activate the GPU compositor - the production renderer
    //    Chroma Key / Effects use - without any other tool.
    runOnMain {
      engine!!.publishLiveLook(
        LivePreviewLook(
          clipId = null,
          adjustments = VideoAdjustments(saturation = 2.6f, brightness = 0.3f),
          filter = FilterSettings(type = FilterType.ENHANCE, intensity = 1f, enabled = true)
        )
      )
    }
    stamp("Filter state submitted (publishLiveLook, no other tool opened before)")

    val compositorOn = awaitCondition(timeoutMs = 8_000) { readOnMain { it.compositorActive.value } == true }
    stamp("Effect registered in the active render graph (compositorActive=$compositorOn, no other tool)")
    assertTrue(
      "Failing stage: Effect registered in the active render graph — the first filter tap did not " +
        "activate the GPU compositor; the look stayed on the Media3 path whose APPLIED result does " +
        "not prove presentation (compositorActive=${readOnMain { it.compositorActive.value }})",
      compositorOn
    )

    // 4. Give the compositor a real output surface (PreviewCompositorView's production role)
    //    and require REAL GL frames on it - not UI state.
    val compositorFrames = AtomicInteger(0)
    attachCompositorSurface(engine!!, compositorFrames)
    val glApplied = awaitCondition(timeoutMs = 10_000) {
      val s = readOnMain { it.compositorStats() }
      s != null && s.renderedFrames > 0
    }
    stamp("GL-thread application completed (compositor frames=${readOnMain { it.compositorStats() }?.renderedFrames})")
    assertTrue(
      "Failing stage: GL-thread application completed — the compositor is active but presented no " +
        "decoded+graded frame on its surface",
      glApplied
    )
    val framesAfterTap = readOnMain { it.compositorStats() }?.renderedFrames ?: 0L

    // 5. Selecting a DIFFERENT filter must re-present immediately (still no other tool).
    runOnMain {
      engine!!.publishLiveLook(
        LivePreviewLook(
          clipId = null,
          adjustments = VideoAdjustments(saturation = 0.3f, temperature = -0.6f),
          filter = FilterSettings(type = FilterType.GLOW, intensity = 1f, enabled = true)
        )
      )
    }
    val rePresented = awaitCondition(timeoutMs = 6_000) {
      (readOnMain { it.compositorStats() }?.renderedFrames ?: 0L) > framesAfterTap
    }
    stamp("Filtered frame presented (selection change re-presented=$rePresented)")
    assertTrue(
      "Failing stage: Filtered frame presented — the second selection produced no new compositor " +
        "frame (frames=$framesAfterTap, compositorActive=${readOnMain { it.compositorActive.value }})",
      rePresented
    )

    // 6. The reported workaround sequence must be a NO-OP now: "open Chroma Key and return".
    //    The compositor is already active; the key edit keeps it rendering.
    runOnMain {
      engine!!.updateTimeline(
        baseTimeline.copy(
          videoClips = baseTimeline.videoClips.map { it.copy(chromaKey = ChromaKeySettings(enabled = true)) }
        )
      )
    }
    val stillOnAfterChroma = awaitCondition(timeoutMs = 6_000) { readOnMain { it.compositorActive.value } == true }
    stamp("Chroma Key applied structurally (compositorActive=$stillOnAfterChroma — already active before)")
    assertTrue(stillOnAfterChroma)

    // 7. Seek: the compositor must re-present the filtered frame at the new position.
    val beforeSeek = readOnMain { it.compositorStats() }?.renderedFrames ?: 0L
    runOnMain { engine!!.seekTo(1_000L) }
    val seekRepainted = awaitCondition(timeoutMs = 8_000) {
      val s = readOnMain { it.compositorStats() } ?: return@awaitCondition false
      s.renderedFrames > beforeSeek && abs(s.lastPositionMs - 1_000L) <= 150L
    }
    stamp("Seek re-presented filtered frame (seekRepainted=$seekRepainted, stats=${readOnMain { it.compositorStats() }})")
    assertTrue(
      "Failing stage: Filtered frame presented after seek — the compositor did not re-decode and " +
        "re-draw the filtered frame at the new position",
      seekRepainted
    )

    LookVerificationReport.section("Stage timeline (elapsedRealtime ms)")
    var previous = -1L
    for ((name, at) in stamps) {
      val delta = if (previous < 0) 0L else at - previous
      LookVerificationReport.line("  ${at}ms (+${delta}ms) $name")
      previous = at
    }
  }

  // ---------------------------------------------------------------------------------------
  // Rendered-pixel proof: the selection changes the actual presented pixels (API >= 26).
  // ---------------------------------------------------------------------------------------

  @Test
  fun coldLaunch_filterTap_renderedPixelsChangeWithTheSelection() {
    assumeTrue("Pixel capture needs SurfaceTexture.getBitmap() (API 26+)", Build.VERSION.SDK_INT >= 26)
    val file = writeTestVideo()
    stamp("Filters panel opened (cold editor, pixel-proof run)")

    val baseTimeline = Timeline(videoClips = listOf(newClip(file)))
    val engineRef = AtomicReference<VideoPlaybackEngine>()
    runOnMain {
      val e = VideoPlaybackEngine(
        context = appContext,
        onTimelinePositionChanged = {},
        onPlaybackEnded = {},
        onPlaybackInterrupted = {}
      )
      engineRef.set(e)
      e.updateTimeline(baseTimeline)
    }
    engine = engineRef.get()

    val frameCount = AtomicInteger(0)
    assertTrue(bindPreviewSurface(engine!!, frameCount))
    assertTrue(awaitCondition(timeoutMs = 8_000) { frameCount.get() > 0 })
    awaitReady(engine!!, timeoutMs = 10_000)

    runOnMain {
      engine!!.publishLiveLook(
        LivePreviewLook(
          clipId = null,
          adjustments = VideoAdjustments(saturation = 2.6f, brightness = 0.3f),
          filter = FilterSettings(type = FilterType.ENHANCE, intensity = 1f, enabled = true)
        )
      )
    }
    stamp("Filter state submitted (pixel run)")
    assertTrue(awaitCondition(timeoutMs = 8_000) { readOnMain { it.compositorActive.value } == true })

    val compositorFrames = AtomicInteger(0)
    val surfaceRef = attachCompositorSurface(engine!!, compositorFrames)
    assertTrue(awaitCondition(timeoutMs = 10_000) { compositorFrames.get() > 0 })
    Thread.sleep(300) // let the queue settle so the capture is the freshly presented frame

    val filteredA = captureBitmap(surfaceRef)
    assertNotNull("Failing stage: Filtered frame presented — no bitmap could be captured", filteredA)
    stamp("Filtered frame presented (capture A: ${filteredA!!.width}x${filteredA.height})")

    // A different look must change the PRESENTED PIXELS, not just state.
    runOnMain {
      engine!!.publishLiveLook(
        LivePreviewLook(
          clipId = null,
          adjustments = VideoAdjustments(saturation = 0.15f, temperature = -0.8f),
          filter = FilterSettings(type = FilterType.GLOW, intensity = 1f, enabled = true)
        )
      )
    }
    assertTrue(awaitCondition(timeoutMs = 6_000) { compositorFrames.get() >= 2 })
    Thread.sleep(300)
    val filteredB = captureBitmap(surfaceRef)
    assertNotNull("Failing stage: GL-thread application completed — second capture failed", filteredB)
    val pixelsChanged = bitmapsDiffer(filteredA!!, filteredB!!)
    stamp("GL-thread application completed (pixels changed with the selection=$pixelsChanged)")
    assertTrue(
      "Failing stage: Filtered frame presented — the compositor presented frames but the rendered " +
        "pixels did NOT change with the selection (identity output?). " +
        "captureA=${filteredA.width}x${filteredA.height} captureB=${filteredB.width}x${filteredB.height}",
      pixelsChanged
    )
  }

  // ---------------------------------------------------------------------------------------
  // Device-side contention repro: the reported failure had the compositor's FIRST decode race
  // the codec slot the live player still held for the same file. One strike then tore the GPU
  // preview down and every look tap stayed invisible until another tool was used. The retry
  // ladder (PreviewDecodeRetryPolicy) must hold the activation open instead.
  // NOTE: an emulator's software codec pool can absorb the held slot (no real starvation), but
  // the INVARIANT is device-independent either way: with a second codec slot held, the first
  // filter tap still activates AND presents AND never tears down.
  // ---------------------------------------------------------------------------------------

  @Test
  fun coldLaunch_filterTap_survivesASecondCodecSlotHeldForTheSameFile() {
    val file = writeTestVideo()
    stamp("Filters panel opened (contention run)")

    val baseTimeline = Timeline(videoClips = listOf(newClip(file)))
    val engineRef = AtomicReference<VideoPlaybackEngine>()
    runOnMain {
      val e = VideoPlaybackEngine(
        context = appContext,
        onTimelinePositionChanged = {},
        onPlaybackEnded = {},
        onPlaybackInterrupted = {}
      )
      engineRef.set(e)
      e.updateTimeline(baseTimeline)
    }
    engine = engineRef.get()
    val frameCount = AtomicInteger(0)
    assertTrue(bindPreviewSurface(engine!!, frameCount))
    assertTrue(awaitCondition(timeoutMs = 8_000) { frameCount.get() > 0 })
    awaitReady(engine!!, timeoutMs = 10_000)

    // Hold a second decoder slot for the SAME file, exactly the state the live player's codec
    // is in on a constrained device when the compositor's first decode runs. A current EGL
    // context is required because the OES texture is created on the calling thread here.
    val core = EglCore(null, EglCore.FLAG_TRY_GLES3)
    val offscreen = OffscreenSurface(core, 1, 1).also { it.makeCurrent() }
    val stray = HardwareVideoTextureSource()
    val slotHeld = stray.initialize(appContext, newClip(file), glHandler = null, decoderHandler = null)
    LookVerificationReport.line("  [diag] stray decoder slot held=$slotHeld (${stray.lastError})")

    runOnMain {
      engine!!.publishLiveLook(
        LivePreviewLook(
          clipId = null,
          adjustments = VideoAdjustments(saturation = 2.6f, brightness = 0.3f),
          filter = FilterSettings(type = FilterType.ENHANCE, intensity = 1f, enabled = true)
        )
      )
    }
    stamp("Filter state submitted (contention: second codec slot held)")
    assertTrue(
      "Failing stage: Effect registered in the active render graph — the first tap did not " +
        "activate the compositor under decoder contention",
      awaitCondition(timeoutMs = 8_000) { readOnMain { it.compositorActive.value } == true }
    )

    val compositorFrames = AtomicInteger(0)
    attachCompositorSurface(engine!!, compositorFrames)
    val presented = awaitCondition(timeoutMs = 20_000) {
      val s = readOnMain { it.compositorStats() }
      s != null && s.renderedFrames > 0
    }
    stamp("GL-thread application completed (frames=${readOnMain { it.compositorStats() }?.renderedFrames})")
    assertTrue(
      "Failing stage: GL-thread application completed — the retry ladder did not deliver a " +
        "graded frame while a second codec slot was held",
      presented
    )
    assertTrue(
      "A transient decode failure must NOT tear the GPU preview down (compositorActive=" +
        "${readOnMain { it.compositorActive.value }})",
      readOnMain { it.compositorActive.value } == true
    )

    runCatching { stray.release() }
    offscreen.release()
    core.release()
  }

  // ---------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------

  private fun writeTestVideo(): File {
    val dir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
    val file = File(dir, "cold_launch_filter_preview.mp4")
    TestMp4Factory.write(
      file = file,
      videoMs = 1_500L,
      audioMs = null,
      picture = TestMp4Factory.Picture.COLORFUL
    )
    videoFile = file
    return file
  }

  private fun newClip(file: File): VideoClip = VideoClip(
    id = "cold_launch_clip",
    name = file.name,
    uri = file.toURI().toString(),
    isVideo = true,
    timelineStartMs = 0L,
    durationMs = 1_500L,
    sourceStartMs = 0L,
    sourceEndMs = 1_500L
  )

  /**
   * Binds a real TextureView (manual SurfaceTexture, exactly the registry contract) to [e].
   * The binding itself is proven by the frame callbacks that arrive on the SurfaceTexture
   * (asserted by the caller) - TextureView.isAvailable is meaningless for a view that is
   * not attached to a window, so it is deliberately not consulted here.
   */
  private fun bindPreviewSurface(e: VideoPlaybackEngine, frameCount: AtomicInteger): Boolean {
    val bound = AtomicReference<Boolean?>(null)
    runOnMain {
      try {
        val tv = TextureView(appContext)
        val st = android.graphics.SurfaceTexture(false)
        st.setOnFrameAvailableListener({ frameCount.incrementAndGet() }, mainHandler)
        tv.setSurfaceTexture(st)
        PreviewVideoSurfaceRegistry.bindPrimary(e.player, tv)
        bound.set(true)
      } catch (t: Throwable) {
        LookVerificationReport.line("  [error] surface bind failed: $t")
        bound.set(false)
      }
    }
    return bound.get() == true
  }

  /** Attaches a real SurfaceTexture as the compositor output (PreviewCompositorView's role). */
  private fun attachCompositorSurface(
    e: VideoPlaybackEngine,
    frameCount: AtomicInteger
  ): android.graphics.SurfaceTexture {
    val st = android.graphics.SurfaceTexture(false)
    st.setOnFrameAvailableListener({ frameCount.incrementAndGet() }, mainHandler)
    runOnMain { e.attachCompositorSurface(st, 480, 854) }
    return st
  }

  /** Non-GL consumer capture: this test is the only consumer of the compositor SurfaceTexture. */
  private fun captureBitmap(st: android.graphics.SurfaceTexture): Bitmap? {
    repeat(10) {
      val bmp = runCatching { st.getBitmap() }.getOrNull()
      if (bmp != null && bmp.width > 0) return bmp
      Thread.sleep(60)
    }
    return null
  }

  /** True when more than 2% of the sampled pixels differ beyond a small tolerance. */
  private fun bitmapsDiffer(a: Bitmap, b: Bitmap): Boolean {
    if (a.width != b.width || a.height != b.height) return true
    val pa = IntArray(a.width * a.height)
    a.getPixels(pa, 0, a.width, 0, 0, a.width, a.height)
    val pb = IntArray(b.width * b.height)
    b.getPixels(pb, 0, b.width, 0, 0, b.width, b.height)
    val stride = max(1, a.width / 32)
    var sampled = 0
    var differing = 0
    var y = 0
    while (y < a.height) {
      var x = 0
      while (x < a.width) {
        val idx = y * a.width + x
        val dr = abs(((pa[idx] shr 16) and 0xFF) - ((pb[idx] shr 16) and 0xFF))
        val dg = abs(((pa[idx] shr 8) and 0xFF) - ((pb[idx] shr 8) and 0xFF))
        val db = abs((pa[idx] and 0xFF) - (pb[idx] and 0xFF))
        if ((dr + dg + db) / 3 > 24) differing++
        sampled++
        x += stride
      }
      y += stride
    }
    return differing * 100 > sampled * 2
  }

  private fun awaitReady(e: VideoPlaybackEngine, timeoutMs: Long): Boolean =
    awaitCondition(timeoutMs) { readOnMain { it.player.playbackState } == Player.STATE_READY }

  private fun awaitCondition(timeoutMs: Long, condition: () -> Boolean): Boolean {
    val deadline = SystemClock.elapsedRealtime() + timeoutMs
    while (SystemClock.elapsedRealtime() < deadline) {
      if (condition()) return true
      Thread.sleep(40)
    }
    return condition()
  }

  private fun <T> readOnMain(read: (VideoPlaybackEngine) -> T): T? {
    val engineNow = engine ?: return null
    val out = AtomicReference<T?>()
    runOnMain { runCatching { out.set(read(engineNow)) } }
    return out.get()
  }

  private fun runOnMain(block: () -> Unit) {
    instrumentation.runOnMainSync(block)
  }
}
