package com.ahstudio.face

import com.ahstudio.face.core.FaceBounds
import com.ahstudio.face.core.FaceLandmarkType
import com.ahstudio.face.core.FaceLandmarks
import com.ahstudio.face.core.FaceRotation
import com.ahstudio.face.core.FaceTrackId
import com.ahstudio.face.core.FaceTrackState
import com.ahstudio.face.core.TrackedFace
import com.ahstudio.face.core.Vec2
import com.ahstudio.face.deformation.DeformationParams
import com.ahstudio.face.deformation.WarpOp
import com.ahstudio.face.deformation.WarpOps
import com.ahstudio.face.deformation.WarpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Expression & Rig face channels on the REAL warp-op pipeline:
 *  - Brows emit brow-line pushes whose direction flips with the sign (raise vs furrow),
 *  - the signed mouth channel flips between smile-lift and frown-droop,
 *  - the signed eye channel shrinks (squint) when negative,
 *  - Auto blink emits a BOTH-eye close pulse that is a pure function of clip time
 *    (identical on preview and export) and emits nothing between pulses.
 */
class WarpOpsExpressionTest {

    private fun face(): TrackedFace {
        val lm = FaceLandmarks(
            mapOf(
                FaceLandmarkType.LEFT_EYE to Vec2(0.60f, 0.40f),
                FaceLandmarkType.RIGHT_EYE to Vec2(0.40f, 0.40f),
                FaceLandmarkType.NOSE_BASE to Vec2(0.50f, 0.50f),
                FaceLandmarkType.MOUTH_LEFT to Vec2(0.56f, 0.60f),
                FaceLandmarkType.MOUTH_RIGHT to Vec2(0.44f, 0.60f),
                FaceLandmarkType.MOUTH_BOTTOM to Vec2(0.50f, 0.64f),
            )
        )
        return TrackedFace(
            trackId = FaceTrackId(1L), detectorTrackId = 1, timestampUs = 0,
            bounds = FaceBounds(0.35f, 0.25f, 0.65f, 0.75f),
            rotation = FaceRotation(0f, 0f, 0f), landmarks = lm, classification = null,
            confidence = 1f, state = FaceTrackState.DETECTED, interOcular = 0.20f,
        )
    }

    private fun eyeOps(params: DeformationParams, t: Long = 0L): List<WarpOp> =
        WarpOps.forFace(face(), params, t).filter { it.center.y < 0.45f && it.type == WarpType.MAGNIFY_EYE }

    @Test
    fun browsEmitRealPushesWithDirectionFlippingBySign() {
        val raise = WarpOps.forFace(face(), DeformationParams(browAdjust = 0.8f))
        val furrow = WarpOps.forFace(face(), DeformationParams(browAdjust = -0.8f))
        // Two brow pushes in each case (one per eye line).
        val raiseBrow = raise.filter { it.type == WarpType.PUSH && it.center.y < 0.38f }
        val furrowBrow = furrow.filter { it.type == WarpType.PUSH && it.center.y < 0.38f }
        assertEquals(2, raiseBrow.size)
        assertEquals(2, furrowBrow.size)
        // In this space content moving UP is dir.y < 0: raise pushes up, furrow pushes down.
        assertTrue(raiseBrow.all { it.dir.y < 0f })
        assertTrue(furrowBrow.all { it.dir.y > 0f })
        assertEquals(raiseBrow[0].strength, furrowBrow[0].strength, 1e-6f)
        // Neutral emits nothing.
        assertTrue(WarpOps.forFace(face(), DeformationParams()).isEmpty())
    }

    @Test
    fun signedMouthFlipsBetweenSmileLiftAndFrownDroop() {
        val smile = WarpOps.forFace(face(), DeformationParams(smileAdjust = 0.6f))
            .filter { it.type == WarpType.PUSH && it.center.y > 0.55f }
        val frown = WarpOps.forFace(face(), DeformationParams(smileAdjust = -0.6f))
            .filter { it.type == WarpType.PUSH && it.center.y > 0.55f }
        assertEquals(2, smile.size)
        assertEquals(2, frown.size)
        assertTrue(smile.all { it.dir.y < 0f })   // corners lift
        assertTrue(frown.all { it.dir.y > 0f })   // corners droop
        assertEquals(smile[0].strength, frown[0].strength, 1e-6f)
    }

    @Test
    fun signedEyeShrinksWhenNegative() {
        val open = eyeOps(DeformationParams(eyeEnlarge = 0.5f))
        val squint = eyeOps(DeformationParams(eyeEnlarge = -0.5f))
        assertEquals(2, open.size)
        assertEquals(2, squint.size)
        assertTrue(open.all { it.strength > 0f })
        assertTrue(squint.all { it.strength < 0f })
        assertEquals(open[0].strength, -squint[0].strength, 1e-6f)
    }

    @Test
    fun autoBlinkPulsesBothEyesAsAPureFunctionOfTime() {
        val period = WarpOps.BLINK_PERIOD_MS
        val atPulse = eyeOps(DeformationParams(autoBlink = 1f), t = period / 2)
        assertEquals("both eyes at the pulse centre", 2, atPulse.size)
        assertTrue(atPulse.all { it.strength < 0f }) // close = shrink
        // Between pulses: no blink ops at all.
        val between = eyeOps(DeformationParams(autoBlink = 1f), t = period / 4)
        assertTrue(between.isEmpty())
        // Deterministic: the same clip time produces the identical ops (preview == export).
        val a = WarpOps.forFace(face(), DeformationParams(autoBlink = 1f), clipTimeMs = period / 2)
        val b = WarpOps.forFace(face(), DeformationParams(autoBlink = 1f), clipTimeMs = period / 2)
        assertEquals(a, b)
        // autoBlink off: nothing at any time.
        assertTrue(eyeOps(DeformationParams(autoBlink = 0f), t = period / 2).isEmpty())
    }

    @Test
    fun blinkAndWinkCoexistWithoutBreakingExistingCounts() {
        // Existing channels keep their op counts (regression guard for the signed rework).
        assertEquals(1, WarpOps.forFace(face(), DeformationParams(bigHead = 1f)).size)
        assertEquals(2, WarpOps.forFace(face(), DeformationParams(chubby = 1f)).size)
        assertEquals(2, WarpOps.forFace(face(), DeformationParams(smileAdjust = 1f)).size)
        assertEquals(2, WarpOps.forFace(face(), DeformationParams(eyeEnlarge = 1f)).size)
    }
}
