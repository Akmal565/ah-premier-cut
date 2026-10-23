package com.example.engine.effects.media3

import com.example.domain.model.FilterSettings
import com.example.domain.model.FilterType
import com.example.domain.model.Timeline
import com.example.domain.model.VideoAdjustments
import com.example.domain.model.VideoClip
import com.example.domain.model.effectiveAdjustments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the routing + decision contracts that make filter / adjust / colour edits appear on the
 * preview WITHOUT opening another tool:
 *
 * 1. every Filters / Adjust / Color-Grading edit is a LOOK-ONLY timeline change, so the
 *    StudioViewModel collector routes it to the live path (`replaceTimelineLooks`), never to the
 *    structural `updateTimeline` (which reloads/rewinds the player);
 * 2. a value-only look edit never rebuilds the ExoPlayer effect graph while a grading node is
 *    resident (no decoder churn on slider ticks);
 * 3. a missing resident grading node ALWAYS forces the install, even with a stable graph
 *    signature — this is the recovery path the deferred-apply starvation used to cut off;
 * 4. structural edits still rebuild playback.
 */
class PreviewLiveInvalidationRoutingTest {

  private fun clip(
    filter: FilterSettings? = null,
    adjustments: VideoAdjustments? = null,
    colorGradeJson: String? = null
  ) = VideoClip(
    id = "v1",
    name = "v1",
    uri = "file:///v1.mp4",
    durationMs = 4000L,
    filter = filter,
    adjustments = adjustments,
    colorGradeJson = colorGradeJson
  )

  private val plain = Timeline(videoClips = listOf(clip()))

  @Test
  fun selectingAFilterWhilePausedIsALookOnlyChange() {
    val filtered = plain.copy(
      videoClips = listOf(clip(filter = FilterSettings(type = FilterType.ENHANCE, intensity = 0.8f)))
    )
    assertTrue(
      "A filter selection must route to the live look path, not reload the player",
      PreviewLookPolicy.isLookOnlyChange(plain, filtered)
    )
  }

  @Test
  fun changingColorSlidersIsALookOnlyChange() {
    val brighter = plain.copy(
      videoClips = listOf(clip(adjustments = VideoAdjustments(exposure = 0.4f, contrast = 1.15f, saturation = 1.2f, temperature = 0.25f, tint = -0.1f)))
    )
    assertTrue(
      "Exposure/contrast/saturation/temperature/tint ticks must publish live, never rebuild playback",
      PreviewLookPolicy.isLookOnlyChange(plain, brighter)
    )
  }

  @Test
  fun colorGradingWheelsAndHslEditsAreLookOnlyChanges() {
    val graded = plain.copy(
      videoClips = listOf(clip(colorGradeJson = "{\"wheels\":{\"lift\":{\"x\":0.1}},\"version\":1}"))
    )
    assertTrue(
      "Colour-engine grades ride the same live path (the compositor re-reads the timeline per frame)",
      PreviewLookPolicy.isLookOnlyChange(plain, graded)
    )
  }

  @Test
  fun switchingFilterPresetsIsALookOnlyChange() {
    val first = plain.copy(
      videoClips = listOf(clip(filter = FilterSettings(type = FilterType.ENHANCE, intensity = 0.8f)))
    )
    val second = first.copy(
      videoClips = listOf(clip(filter = FilterSettings(type = FilterType.BLACK_AND_WHITE, intensity = 0.6f)))
    )
    assertTrue(PreviewLookPolicy.isLookOnlyChange(first, second))
  }

  @Test
  fun resettingALookIsALookOnlyChangeToo() {
    val edited = plain.copy(
      videoClips = listOf(
        clip(
          filter = FilterSettings(type = FilterType.ENHANCE, intensity = 0.8f),
          adjustments = VideoAdjustments(exposure = 0.3f)
        )
      )
    )
    assertTrue(
      "A reset must restore the neutral preview through the same live path",
      PreviewLookPolicy.isLookOnlyChange(edited, plain)
    )
  }

  @Test
  fun structuralEditsStillRebuildPlayback() {
    val trimmed = plain.copy(videoClips = listOf(clip().copy(durationMs = 2500L)))
    assertFalse("A trim is structural and must go through updateTimeline", PreviewLookPolicy.isLookOnlyChange(plain, trimmed))
    val added = plain.copy(videoClips = listOf(clip(), clip().copy(id = "v2")))
    assertFalse("Adding a clip is structural", PreviewLookPolicy.isLookOnlyChange(plain, added))
    val speedChanged = plain.copy(videoClips = listOf(clip().copy(speed = 2f)))
    assertFalse("A speed change is structural", PreviewLookPolicy.isLookOnlyChange(plain, speedChanged))
  }

  @Test
  fun sliderTickWithResidentGradingNodeMustNotCallSetVideoEffects() {
    // The value signature changes (brightness moved) but the structural graph signature must not.
    val before = plain.copy(videoClips = listOf(clip(adjustments = VideoAdjustments(brightness = 0.2f))))
    val after = before.copy(videoClips = listOf(clip(adjustments = VideoAdjustments(brightness = 0.45f))))
    assertFalse(
      "A value-only tick must not rebuild the Media3 graph",
      PreviewEffectApplyPolicy.shouldRebuildGraph(
        PreviewFilterEffects.media3GraphSignature(before.videoClips.first(), before),
        PreviewFilterEffects.media3GraphSignature(after.videoClips.first(), after),
        force = false
      )
    )
    val decision = PreviewGraphDecision.outcomeFor(
      graphChanged = false,
      signatureChanged = false,
      residentGradingNode = true,
      newEffectCount = 1,
      previousHadEffects = true,
      force = false
    )
    assertFalse(
      "Publishing live uniforms must not swing setVideoEffects",
      decision.callSetVideoEffects
    )
  }

  @Test
  fun missingResidentGradingNodeAlwaysForcesTheInstall() {
    // Stable signature + no resident node = the exact state a sticky deferral leaves behind.
    // The decision must still install the graph, otherwise every recovery path is dead.
    val sig = PreviewFilterEffects.media3GraphSignature(plain.videoClips.first(), plain)
    val decision = PreviewGraphDecision.outcomeFor(
      graphChanged = PreviewGraphDecision.graphChanged(
        previousGraphSignature = sig,
        nextGraphSignature = sig,
        residentGradingNode = false,
        force = false
      ),
      signatureChanged = false,
      residentGradingNode = false,
      newEffectCount = 1,
      previousHadEffects = false,
      force = false
    )
    assertTrue(
      "A look with no resident grading node must be installed even when the signature is unchanged",
      decision.callSetVideoEffects
    )
    assertTrue(
      PreviewGraphDecision.needsResidentGradingInstall(residentGradingNode = false, identityFallbackActive = false)
    )
  }

  @Test
  fun deferredApplyKeepsTheLookInstallableOnceTheSurfaceReturns() {
    // The defer gate itself: defer only when the surface is missing, and the recovery (flush)
    // path the retry drives runs a FORCED apply that installs even a signature-stable graph.
    assertTrue(PreviewEffectApplyPolicy.shouldDeferLookApply(surfaceBound = false))
    assertFalse(PreviewEffectApplyPolicy.shouldDeferLookApply(surfaceBound = true))
    assertTrue(
      PreviewGraphDecision.graphChangedOrForced(
        previousGraphSignature = "G||",
        nextGraphSignature = "G||",
        residentGradingNode = true,
        force = true
      )
    )
  }

  @Test
  fun resolvedSnapshotMatchesTheEditedClipValues() {
    // The snapshot the live publish writes must be built from the SAME clip the look targets,
    // otherwise the shader shows the previous tick's values (the stale-frame class of bug).
    val edited = clip(adjustments = VideoAdjustments(exposure = 0.5f, whites = 0.25f))
    val timeline = Timeline(videoClips = listOf(edited))
    val snapshot = ColorGradingSnapshot.from(edited.effectiveAdjustments(timeline), PreviewFilterEffects.effectiveFilter(edited, timeline))
    assertEquals(0.5f, snapshot.exposure, 1e-4f)
    assertEquals(0.25f, snapshot.whites, 1e-4f)
    assertEquals(1f, snapshot.contrast, 1e-4f)
    // And it renders through the shared CPU reference (preview == export maths).
    val neutral = ColorGradingSnapshot.from(VideoAdjustments(), FilterSettings()).evaluatePreviewPixel(0.5f, 0.5f, 0.5f)
    val graded = snapshot.evaluatePreviewPixel(0.5f, 0.5f, 0.5f)
    assertTrue(
      "An exposure lift must visibly move the same pixel",
      graded[0] != neutral[0] || graded[1] != neutral[1] || graded[2] != neutral[2]
    )
  }
}
