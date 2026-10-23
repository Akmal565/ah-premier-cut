package com.example.engine.effects

import com.example.domain.model.Timeline
import com.example.domain.model.VideoClip
import com.example.engine.effects.registry.EffectCategory
import com.example.engine.effects.media3.PreviewVfxCoverage
import com.example.engine.preview.PreviewCompositionPolicy
import com.example.engine.vfx.VfxEffectsHost
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins the real-device fix for the "Effects tools tap does not reach the video preview" bug.
 *
 * The fix routes VFX previews through the Media3 ExoPlayer effect graph whenever the effect has
 * a Media3 preview shader (the bulk of the catalog: trend.*, blur.motion, video.vhs, etc.).
 * Only effects the Media3 path CANNOT draw (subject.* segmentation-driven effects, face
 * reshape, body reshape, AR overlays, non-LUT colour grades) force the GPU compositor on.
 *
 * Why: the GPU compositor's first activation on a fresh clip races its TextureView's
 * surface-attach AND its own first MediaCodec decode against ExoPlayer's just-disabled
 * video track - and when that race loses on real hardware, the user sees no effect at all.
 * Media3 takes the effect straight onto the already-rendering ExoPlayer preview, so the
 * effect shows up the moment the user taps the tile.
 *
 * This test pins the data-side half of the contract: a tap on a catalog effect writes a
 * vfxStackJson / faceReshape / bodyReshape / isBackgroundRemoved that
 * PreviewCompositionPolicy.requiresCompositor routes correctly - Media3-eligible effects stay
 * on the player path, Media3-ineligible effects force the compositor on.
 */
class EffectsTapActivatesCompositorTest {

  @Before
  fun setUp() {
    ProductionEffectCatalog.install()
  }

  private fun bareClip(): VideoClip =
    VideoClip(id = "v", name = "v", uri = "file:///v.mp4", timelineStartMs = 0L, durationMs = 10_000L, sourceEndMs = 10_000L)

  @Test
  fun aProjectWithNoEffectsStaysOnThePlayerPath() {
    val timeline = Timeline(videoClips = listOf(bareClip()))
    assertFalse(
      "An empty project must not require the compositor (player path is fine)",
      PreviewCompositionPolicy.requiresCompositor(timeline)
    )
  }

  @Test
  fun media3EligibleVideoEffectsStayOnThePlayerPath() {
    // VIDEO_EFFECTS all have Media3 preview shaders (trend.*, blur.motion) - they render via
    // ExoPlayer's own video-effect graph, no compositor activation needed.
    ProductionEffectCatalog.effects
      .filter { it.category == EffectCategory.VIDEO_EFFECTS }
      .forEach { effect ->
        val clip = bareClip()
        val playWindow = ProductionEffectApplicator.videoEffectWindow(clip.durationMs, playheadRelMs = 0L)
        val applied = ProductionEffectApplicator.apply(clip, effect, intensity = 1f, playWindowMs = playWindow)
        val timeline = Timeline(videoClips = listOf(applied))
        assertFalse(
          "Effect ${effect.id} (${effect.name}) has a Media3 preview shader and must stay on the " +
            "player path (no compositor activation race). vfxStackJson=${applied.vfxStackJson}",
          PreviewCompositionPolicy.requiresCompositor(timeline)
        )
      }
  }

  @Test
  fun media3EligiblePhotoEffectsStayOnThePlayerPath() {
    // PHOTO_EFFECTS with vfx:trend.* shaderKey have Media3 shaders; they stay on the player path.
    ProductionEffectCatalog.effects
      .filter { it.category == EffectCategory.PHOTO_EFFECTS }
      .filter { it.shaderKey?.startsWith("vfx:") == true }
      .forEach { effect ->
        val clip = bareClip()
        val applied = ProductionEffectApplicator.apply(clip, effect, intensity = 1f, playWindowMs = null)
        val timeline = Timeline(videoClips = listOf(applied))
        assertFalse(
          "Photo effect ${effect.id} (${effect.name}) has a Media3 preview shader and must stay on the player path",
          PreviewCompositionPolicy.requiresCompositor(timeline)
        )
      }
  }

  @Test
  fun subjectDrivenEffectsForceTheCompositor() {
    // BODY_EFFECTS + AI_EFFECTS use vfx:subject.* shaderKeys - the Media3 path cannot render
    // them (no preview shader in MEDIA3_PREVIEW_EFFECT_IDS), so they MUST force the compositor.
    val subjectEffects = ProductionEffectCatalog.effects.filter {
      (it.category == EffectCategory.BODY_EFFECTS || it.category == EffectCategory.AI_EFFECTS) &&
        it.shaderKey?.startsWith("vfx:subject") == true
    }
    assertTrue("Catalog must have subject-driven effects to test", subjectEffects.isNotEmpty())
    subjectEffects.forEach { effect ->
      val clip = bareClip()
      val applied = ProductionEffectApplicator.apply(clip, effect, intensity = 1f, playWindowMs = null)
      val timeline = Timeline(videoClips = listOf(applied))
      assertTrue(
        "Subject effect ${effect.id} (${effect.name}) has no Media3 shader and MUST force the compositor",
        PreviewCompositionPolicy.requiresCompositor(timeline)
      )
    }
  }

  @Test
  fun faceReshapeEffectsForceTheCompositor() {
    // FACE_EFFECTS (face:*) write faceReshape on the clip; the compositor is the only path
    // that runs the face mesh warp stage.
    ProductionEffectCatalog.effects
      .filter { it.shaderKey?.startsWith("face:") == true }
      .forEach { effect ->
        val clip = bareClip()
        val applied = ProductionEffectApplicator.apply(clip, effect, intensity = 1f, playWindowMs = null)
        val timeline = Timeline(videoClips = listOf(applied))
        assertTrue(
          "Face effect ${effect.id} (${effect.name}) writes faceReshape and must force the compositor",
          PreviewCompositionPolicy.requiresCompositor(timeline)
        )
      }
  }

  @Test
  fun clearingAVideoEffectKeepsThePlayerPath() {
    // A "None" tap (clearCategory) must leave the project on the player path.
    val clip = bareClip()
    val explosion = ProductionEffectCatalog.byId("video.explosion")!!
    val applied = ProductionEffectApplicator.apply(clip, explosion, 1f, playWindowMs = 3_000L to 3_000L)
    val cleared = ProductionEffectApplicator.clearCategory(applied, EffectCategory.VIDEO_EFFECTS)
    val timeline = Timeline(videoClips = listOf(cleared))
    assertFalse(
      "Clearing the last VFX must keep the player path (no active effects left)",
      PreviewCompositionPolicy.requiresCompositor(timeline)
    )
  }

  @Test
  fun aZeroIntensityEffectDoesNotFlipTheCompositor() {
    // Disabled / zero-intensity effects never count - the reset path must release the
    // compositor the same way a clear does, otherwise a reset effect would leave the
    // project stuck on the GPU path even though the picture is unchanged.
    val clip = bareClip()
    val explosion = ProductionEffectCatalog.byId("video.explosion")!!
    val applied = ProductionEffectApplicator.apply(clip, explosion, 0f, playWindowMs = 3_000L to 3_000L)
    val stack = VfxEffectsHost.decode(applied.vfxStackJson)
    assertNotNull(applied.vfxStackJson)
    assertTrue("An applied effect must land on the stack", stack.size == 1)
    assertTrue(
      "A zero-intensity effect must not require the compositor (no visual change)",
      !PreviewCompositionPolicy.requiresCompositor(Timeline(videoClips = listOf(applied)))
    )
  }

  @Test
  fun mixingAMedia3EffectAndASubjectEffectForcesTheCompositor() {
    // A subject effect mixed with a Media3-eligible effect: the subject effect can only
    // render in the compositor, so the project must take the compositor path even though
    // some of the stack would be fine on the player path.
    val clip = bareClip()
    val withExplosion = ProductionEffectApplicator.apply(clip, ProductionEffectCatalog.byId("video.explosion")!!, 1f, playWindowMs = null)
    val withOpticalScan = ProductionEffectApplicator.apply(withExplosion, ProductionEffectCatalog.byId("body.optical_scan")!!, 1f, playWindowMs = null)
    val timeline = Timeline(videoClips = listOf(withOpticalScan))
    assertTrue(
      "A subject effect on the stack forces the compositor even with Media3-eligible siblings",
      PreviewCompositionPolicy.requiresCompositor(timeline)
    )
  }

  @Test
  fun media3PreviewCoverageSetMatchesTheCatalog() {
    // Guard against drift: every non-subject VFX in the catalog must be in the Media3 preview
    // set, so the policy's split never silently routes a renderable effect onto the compositor.
    ProductionEffectCatalog.effects
      .filter { it.shaderKey?.startsWith("vfx:") == true }
      .filter { !it.shaderKey!!.startsWith("vfx:subject") }
      .forEach { effect ->
        val effectId = effect.shaderKey!!.removePrefix("vfx:")
        assertTrue(
          "VFX effect ${effect.id} (shaderKey=${effect.shaderKey}) is not subject-driven but is " +
            "missing from PreviewVfxCoverage.MEDIA3_PREVIEW_EFFECT_IDS - it would silently land on " +
            "the compositor path even though it has a Media3 preview shader implementation",
          effectId in PreviewVfxCoverage.MEDIA3_PREVIEW_EFFECT_IDS
        )
      }
  }
}
