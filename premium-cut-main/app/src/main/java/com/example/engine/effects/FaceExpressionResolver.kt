package com.example.engine.effects

import com.ahstudio.face.deformation.DeformationParams
import kotlin.math.abs

/**
 * Face expression model for the redesigned Expression & Rig panel (Face tab).
 *
 * Every control maps onto REAL channels of [DeformationParams], which the existing face
 * mesh-warp stage renders in preview AND export (same path as the Face effects). Nothing here
 * is a mock: the resolved params are persisted on the clip via DeformationCodec and published
 * to FaceWarpRegistry by StudioViewModel.setFaceDeform.
 *
 * Channel semantics (all sliders 0..100):
 *  - Intensity scales the selected preset's channel set ("how strongly the preset is applied").
 *  - Eyes    50 = neutral, > 50 opens/enlarges, < 50 squints (signed eyeEnlarge).
 *  - Brows   50 = neutral, > 50 raises,        < 50 furrows (signed browAdjust).
 *  - Mouth   50 = neutral, > 50 smiles,        < 50 frowns  (signed smileAdjust).
 *  - autoBlink switches the natural periodic blink (pure function of clip-local time).
 */
enum class FaceExpressionPreset(val label: String) {
  NEUTRAL("Neutral"),
  HAPPY("Happy"),
  SAD("Sad"),
  ANGRY("Angry"),
  SURPRISED("Surprised"),
  WINK("Wink"),
}

/** The panel's Face-tab state. All slider fields are 0..100. */
data class FaceExpressionState(
  val preset: FaceExpressionPreset? = null,
  val intensity: Int = 70,
  val eyes: Int = 50,
  val brows: Int = 50,
  val mouth: Int = 50,
  val autoBlink: Boolean = false,
) {
  companion object {
    val DEFAULT = FaceExpressionState()
  }
}

object FaceExpressionResolver {

  /** The preset's base channel set, scaled by the Intensity slider at resolve time. */
  data class PresetShape(
    val eyeEnlarge: Float = 0f,
    val faceSlim: Float = 0f,
    val jawSharp: Float = 0f,
    val chubby: Float = 0f,
    val wink: Float = 0f,
    val smileAdjust: Float = 0f,
    val browAdjust: Float = 0f,
  )

  fun presetShape(preset: FaceExpressionPreset): PresetShape = when (preset) {
    FaceExpressionPreset.NEUTRAL -> PresetShape()
    // Cheek lift + lifted mouth corners: the recognizable smile geometry.
    FaceExpressionPreset.HAPPY -> PresetShape(smileAdjust = 0.55f, chubby = 0.28f, browAdjust = 0.12f)
    // Drooping mouth corners + inner-brow frown.
    FaceExpressionPreset.SAD -> PresetShape(smileAdjust = -0.48f, browAdjust = -0.32f)
    // Furrowed brows + tightened jaw.
    FaceExpressionPreset.ANGRY -> PresetShape(browAdjust = -0.55f, jawSharp = 0.30f, faceSlim = 0.15f)
    // Wide eyes + raised brows.
    FaceExpressionPreset.SURPRISED -> PresetShape(eyeEnlarge = 0.50f, browAdjust = 0.60f)
    // The classic alternating squint + a hint of a smirk.
    FaceExpressionPreset.WINK -> PresetShape(wink = 0.80f, smileAdjust = 0.20f)
  }

  /**
   * Resolves the panel state into REAL warp parameters.
   * preset channels x intensity + signed slider offsets, then clamped to channel ranges.
   */
  fun resolve(state: FaceExpressionState): DeformationParams {
    val shape = state.preset?.let { presetShape(it) } ?: PresetShape()
    val k = (state.intensity.coerceIn(0, 100)) / 100f
    val eyes = ((state.eyes.coerceIn(0, 100)) - 50) / 100f      // -0.5..+0.5
    val brows = ((state.brows.coerceIn(0, 100)) - 50) / 100f    // -0.5..+0.5
    val mouth = ((state.mouth.coerceIn(0, 100)) - 50) / 100f    // -0.5..+0.5
    return DeformationParams(
      eyeEnlarge = (shape.eyeEnlarge * k + eyes).coerceIn(-1f, 1f),
      faceSlim = (shape.faceSlim * k).coerceIn(0f, 1f),
      jawSharp = (shape.jawSharp * k).coerceIn(0f, 1f),
      chubby = (shape.chubby * k).coerceIn(0f, 1f),
      wink = (shape.wink * k).coerceIn(0f, 1f),
      smileAdjust = (shape.smileAdjust * k + mouth).coerceIn(-1f, 1f),
      browAdjust = (shape.browAdjust * k + brows).coerceIn(-1f, 1f),
      autoBlink = if (state.autoBlink) 1f else 0f,
    )
  }

  /**
   * Best-effort inverse: recovers slider values from rendered params so reopening the panel
   * (or undo/redo/project load) shows the effective state instead of stale defaults.
   * A preset is highlighted only when the params equal its shape at full intensity with
   * neutral sliders (within tolerance); anything else honestly reads as custom.
   */
  fun toState(params: DeformationParams): FaceExpressionState {
    fun close(a: Float, b: Float) = abs(a - b) <= 0.02f
    val preset = FaceExpressionPreset.values().firstOrNull { p ->
      val s = presetShape(p)
      close(s.eyeEnlarge, params.eyeEnlarge) && close(s.chubby, params.chubby) &&
        close(s.wink, params.wink) && close(s.jawSharp, params.jawSharp) &&
        close(s.faceSlim, params.faceSlim) && close(s.smileAdjust, params.smileAdjust) &&
        close(s.browAdjust, params.browAdjust)
    }
    val matched = preset != null
    return FaceExpressionState(
      preset = preset,
      intensity = if (matched) 100 else 0,
      eyes = ((params.eyeEnlarge * 100f).toInt() + 50).coerceIn(0, 100),
      brows = ((params.browAdjust * 100f).toInt() + 50).coerceIn(0, 100),
      mouth = ((params.smileAdjust * 100f).toInt() + 50).coerceIn(0, 100),
      autoBlink = params.autoBlink > 0.001f,
    )
  }
}
