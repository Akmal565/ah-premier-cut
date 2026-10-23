package com.example.engine.preview

import com.ahstudio.color.core.ColorState
import com.ahstudio.face.deformation.DeformationCodec
import com.ahstudio.face.deformation.isActive as isFaceDeformActive
import com.example.domain.model.FilterSettings
import com.example.domain.model.Timeline
import com.example.domain.model.VideoAdjustments
import com.example.domain.model.VideoClip
import com.example.engine.color.ColorEngineHost
import com.example.engine.effects.BodyReshapeCodec
import com.example.engine.effects.media3.PreviewLookPolicy
import com.example.engine.effects.media3.PreviewVfxCoverage

/**
 * The other half of the preview/export parity rule: effects the plain player path cannot draw
 * must switch the live preview to the GPU compositor, exactly like chroma key / background
 * removal already do. Without this, an effect landed on the clip (and showed up in the export)
 * while the preview silently kept showing the un-effected picture.
 *
 * Everything checked here renders only in the compositor (`GpuCompositionRenderer` /
 * `VfxStackStage` / `ColorGradeStage`), never in the Media3 player graph:
 *  - registry VFX without a Media3 preview shader ([PreviewVfxCoverage]),
 *  - face / body reshape (mesh warps),
 *  - AR overlays,
 *  - colour-engine grades beyond a plain LUT (`ColorGradeJson` lift/gamma/curves/HSL/...),
 *  - explicitly set looks (filter / adjustments): main-track clips, overlay clips and the
 *    project-level fallback. The player path's ONLY success signal for a look graph is a
 *    non-throwing `setVideoEffects` - it never proved a graded frame was actually presented
 *    (the Media3 GL node can silently degrade to identity / drop presentation on some devices,
 *    which is the reported "filter preview only works after switching to another tool" bug),
 *    so looks render with the export shaders in the compositor instead,
 *  - tracked blur / mosaic effect clips.
 *
 * Pure Kotlin (domain models + pure codecs only) so the JVM test harness can execute it.
 */
object PreviewCoveragePolicy {

  fun requiresComposition(timeline: Timeline): Boolean {
    val clips = timeline.videoClips + timeline.overlayClips
    // Real-device fix: only force the compositor for VFX that the Media3 player path CANNOT
    // render (subject.* segmentation-driven effects). Effects with a Media3 preview shader
    // (the bulk of the catalog: trend.*, blur.motion, video.vhs, etc. — see
    // PreviewVfxCoverage.MEDIA3_PREVIEW_EFFECT_IDS) render via ExoPlayer's own video-effect
    // graph, which has no compositor-activation race and no codec-conflict first-decode
    // failure. The previous "any active VFX forces the compositor" rule routed every effect
    // tap through the GPU compositor, but on real hardware the compositor's first activation
    // (cold launch / fresh clip) races the new TextureView's surface-attach AND its own first
    // MediaCodec decode against ExoPlayer's just-disabled video track - and when that race
    // loses the user sees no effect at all. Media3 takes the effect straight onto the
    // already-rendering ExoPlayer preview, so the user sees the effect immediately.
    // Disabled / zero-intensity stacks never count (no visual change).
    if (clips.any { PreviewVfxCoverage.hasUnrepresentableEffects(it.vfxStackJson) }) return true
    // Mesh-warp retouch (face / body) and AR face overlays.
    if (clips.any { faceReshapeActive(it) || bodyReshapeActive(it) }) return true
    if (clips.any { !it.arOverlay.isNullOrBlank() }) return true
    // A colour-engine grade that is more than a LUT (wheels / curves / HSL / mixer / ...).
    if (clips.any { hasNonLutGrade(it) }) return true
    // An explicitly set look (main-track clip, project-level fallback, or overlay clip) renders
    // in the compositor. Overlay clips always needed this (the overlay lane has no grading node);
    // main-track / project-level looks joined them after the cold-launch filter-preview bug: the
    // Media3 graph reported APPLIED while the device silently presented no graded frame, so the
    // look became visible only when another tool (Chroma Key / Effects) activated the compositor.
    // There is no reliable failure signal to react to on that path - the compositor is the one
    // renderer whose presentation chain is verifiable (generation bump -> decode -> draw ->
    // swapBuffers), and it grades every layer with the export shaders.
    if (hasActiveLook(timeline)) return true
    // Tracked blur / mosaic regions are compositor-only.
    if (timeline.effectClips.isNotEmpty()) return true
    return false
  }

  private fun faceReshapeActive(clip: VideoClip): Boolean {
    val json = clip.faceReshape ?: return false
    return runCatching { DeformationCodec.decode(json).isFaceDeformActive() }.getOrDefault(false)
  }

  private fun bodyReshapeActive(clip: VideoClip): Boolean {
    val json = clip.bodyReshape ?: return false
    return runCatching { BodyReshapeCodec.decode(json).isActive() }.getOrDefault(false)
  }

  /** True when the grade changes the picture without its LUT (LUT-only grades stay on the player path). */
  private fun hasNonLutGrade(clip: VideoClip): Boolean {
    val json = clip.colorGradeJson ?: return false
    if (json.isBlank()) return false
    val state = runCatching { ColorEngineHost.decodeOrDefault(json) }.getOrDefault(ColorState())
    return !state.copy(lut = null).isIdentity()
  }

  /**
   * True when a filter / colour look is explicitly set anywhere the preview must draw it: a
   * main-track clip, the project-level fallback look ([Timeline.filter] / [Timeline.adjustments],
   * written by the Filters tools when no clip is targeted and consumed by
   * `CompositionLookResolver.timelineFallback`), or an overlay clip.
   *
   * Disabled / zero-intensity / reset-to-default looks never count - a reset must release the
   * compositor again (same rule the overlay term has always used).
   */
  fun hasActiveLook(timeline: Timeline): Boolean =
    timeline.videoClips.any { hasClipLocalLook(it) } ||
      PreviewLookPolicy.hasAnyLook(timeline.adjustments, timeline.filter) ||
      timeline.overlayClips.any { hasClipLocalLook(it) }

  /** Explicit, non-identity look written on the clip itself (not inherited from the project default). */
  private fun hasClipLocalLook(clip: VideoClip): Boolean {
    val adj = clip.adjustments
    val flt = clip.filter
    if (adj == null && flt == null) return false
    // Same "is anything actually set" rule the Filters tools use, so a clip that carries the
    // defaults written back by a reset (type NONE, identity adjustments) stays on the player path.
    return PreviewLookPolicy.hasAnyLook(adj ?: VideoAdjustments(), flt ?: FilterSettings())
  }
}
