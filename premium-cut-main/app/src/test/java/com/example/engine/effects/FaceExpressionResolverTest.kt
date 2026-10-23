package com.example.engine.effects

import com.ahstudio.face.deformation.DeformationCodec
import com.ahstudio.face.deformation.DeformationParams
import com.ahstudio.face.deformation.hasMeshWarp
import com.ahstudio.face.deformation.isActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Expression & Rig Face-tab contracts:
 *  - presets are REAL channel sets on DeformationParams (rendered by the face mesh warp),
 *  - Intensity scales the preset shape; Eyes/Brows/Mouth add signed offsets (50 = neutral),
 *  - resolve -> encode -> decode round-trips exactly (signed channels + autoBlink included),
 *  - Neutral + neutral sliders resolve to an all-zero (truly neutral) face.
 */
class FaceExpressionResolverTest {

  @Test
  fun presetsAreRealDistinctChannelSets() {
    val shapes = FaceExpressionPreset.values().map { FaceExpressionResolver.presetShape(it) }
    assertEquals(6, shapes.size)
    // Neutral is all zero.
    val neutral = FaceExpressionResolver.presetShape(FaceExpressionPreset.NEUTRAL)
    assertEquals(DeformationParams(), FaceExpressionResolver.resolve(FaceExpressionState(preset = FaceExpressionPreset.NEUTRAL)))
    assertTrue(neutral == FaceExpressionResolver.PresetShape())
    // Every other preset actually deforms something.
    shapes.drop(1).forEach { shape ->
      val active = shape.eyeEnlarge != 0f || shape.chubby != 0f || shape.wink != 0f ||
        shape.jawSharp != 0f || shape.faceSlim != 0f || shape.smileAdjust != 0f || shape.browAdjust != 0f
      assertTrue("preset must deform something", active)
    }
    // Happy smiles, Sad frowns (signed mouth curve), Angry furrows, Surprised opens eyes.
    assertTrue(FaceExpressionResolver.presetShape(FaceExpressionPreset.HAPPY).smileAdjust > 0f)
    assertTrue(FaceExpressionResolver.presetShape(FaceExpressionPreset.SAD).smileAdjust < 0f)
    assertTrue(FaceExpressionResolver.presetShape(FaceExpressionPreset.ANGRY).browAdjust < 0f)
    assertTrue(FaceExpressionResolver.presetShape(FaceExpressionPreset.SURPRISED).eyeEnlarge > 0f)
  }

  @Test
  fun intensityScalesThePresetShape() {
    val base = FaceExpressionState(preset = FaceExpressionPreset.HAPPY, eyes = 50, brows = 50, mouth = 50)
    val full = FaceExpressionResolver.resolve(base.copy(intensity = 100))
    val half = FaceExpressionResolver.resolve(base.copy(intensity = 50))
    val zero = FaceExpressionResolver.resolve(base.copy(intensity = 0))
    assertEquals(0.55f, full.smileAdjust, 1e-4f)
    assertEquals(0.275f, half.smileAdjust, 1e-4f)
    assertEquals(0f, zero.smileAdjust, 1e-4f)
    assertEquals(0f, zero.chubby, 1e-4f)
  }

  @Test
  fun slidersAddSignedOffsetsOnTopOfThePreset() {
    val happy = FaceExpressionResolver.resolve(
      FaceExpressionState(preset = FaceExpressionPreset.HAPPY, intensity = 100, eyes = 50, brows = 50, mouth = 50)
    )
    // Mouth slider 80 -> +0.3 on top of the happy smile 0.55.
    val happier = FaceExpressionResolver.resolve(
      FaceExpressionState(preset = FaceExpressionPreset.HAPPY, intensity = 100, eyes = 50, brows = 50, mouth = 80)
    )
    assertEquals(0.85f, happier.smileAdjust, 1e-4f)
    assertEquals(happy.eyeEnlarge, happier.eyeEnlarge, 1e-6f)
    // Mouth slider 10 -> -0.4 offset flips the smile into a frown.
    val frowning = FaceExpressionResolver.resolve(
      FaceExpressionState(preset = FaceExpressionPreset.HAPPY, intensity = 100, eyes = 50, brows = 50, mouth = 10)
    )
    assertEquals(0.15f, frowning.smileAdjust, 1e-4f)
    // Eyes 0 squints (signed negative eye channel), Brows 100 raises.
    val squint = FaceExpressionResolver.resolve(FaceExpressionState(eyes = 0, brows = 50, mouth = 50))
    assertEquals(-0.5f, squint.eyeEnlarge, 1e-4f)
    val raised = FaceExpressionResolver.resolve(FaceExpressionState(eyes = 50, brows = 100, mouth = 50))
    assertEquals(0.5f, raised.browAdjust, 1e-4f)
  }

  @Test
  fun neutralStateResolvesToAllZeroFace() {
    val p = FaceExpressionResolver.resolve(FaceExpressionState.DEFAULT)
    assertEquals(DeformationParams(), p)
  }

  @Test
  fun autoBlinkSurvivesTheRoundTrip() {
    val on = FaceExpressionResolver.resolve(FaceExpressionState(autoBlink = true))
    assertEquals(1f, on.autoBlink, 1e-6f)
    val encoded = DeformationCodec.encode(on)
    assertTrue(encoded != null)
    assertEquals(1f, DeformationCodec.decode(encoded).autoBlink, 1e-6f)
    // autoBlink alone counts as active (must persist and render).
    assertTrue(on.isActive())
    assertTrue(on.hasMeshWarp())
    val off = FaceExpressionResolver.resolve(FaceExpressionState.DEFAULT)
    assertNull(DeformationCodec.encode(off))
  }

  @Test
  fun signedChannelsRoundTripThroughTheCodec() {
    val p = DeformationParams(eyeEnlarge = -0.5f, smileAdjust = -0.7f, browAdjust = -0.9f, autoBlink = 1f)
    val decoded = DeformationCodec.decode(DeformationCodec.encode(p))
    assertEquals(-0.5f, decoded.eyeEnlarge, 1e-5f)
    assertEquals(-0.7f, decoded.smileAdjust, 1e-5f)
    assertEquals(-0.9f, decoded.browAdjust, 1e-5f)
    assertEquals(1f, decoded.autoBlink, 1e-5f)
  }

  @Test
  fun legacyCodecStringsStillDecodeIdentically() {
    // An old project string with only the 16 legacy fields (all 0..1).
    val legacy = listOf(0.2f, 0.3f, 0f, 0f, 0f, 0.4f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.5f, 0f, 0f)
      .joinToString(",")
    val d = DeformationCodec.decode(legacy)
    assertEquals(0.2f, d.eyeEnlarge, 1e-5f)
    assertEquals(0.3f, d.faceSlim, 1e-5f)
    assertEquals(0.4f, d.smileAdjust, 1e-5f)
    assertEquals(0.5f, d.chubby, 1e-5f)
    assertEquals(0f, d.browAdjust, 1e-5f)
    assertEquals(0f, d.autoBlink, 1e-5f)
  }

  @Test
  fun toStateRecoversSlidersAndFlagsHonestly() {
    val state = FaceExpressionState(intensity = 100, eyes = 75, brows = 25, mouth = 85, autoBlink = true)
    val recovered = FaceExpressionResolver.toState(FaceExpressionResolver.resolve(state))
    assertEquals(75, recovered.eyes)
    assertEquals(25, recovered.brows)
    assertEquals(85, recovered.mouth)
    assertTrue(recovered.autoBlink)
    // A clean preset apply (full intensity, neutral sliders) re-highlights the preset.
    val presetState = FaceExpressionState(preset = FaceExpressionPreset.HAPPY, intensity = 100)
    val back = FaceExpressionResolver.toState(FaceExpressionResolver.resolve(presetState))
    assertEquals(FaceExpressionPreset.HAPPY, back.preset)
    // A custom blend is honestly NOT highlighted as a preset.
    val custom = FaceExpressionResolver.toState(
      FaceExpressionResolver.resolve(FaceExpressionState(preset = FaceExpressionPreset.HAPPY, intensity = 40))
    )
    assertNull(custom.preset)
    assertFalse(custom.autoBlink)
  }
}
