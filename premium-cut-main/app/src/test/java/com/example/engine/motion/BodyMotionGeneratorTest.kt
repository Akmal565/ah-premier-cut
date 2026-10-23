package com.example.engine.motion

import com.example.domain.model.KeyframeInterpolation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Pins the Expression & Rig Body-tab contracts - the motion is REAL keyframes:
 *  - generated keyframes span the clip (loop) or one cycle from the playhead (no loop),
 *  - times are strictly increasing, the pattern lands back on the base pose,
 *  - Speed changes the cycle density, Smooth picks the interpolation,
 *  - Mirror negates horizontal travel and rotation, IK balance damps wander and bounds
 *    the vertical excursion,
 *  - the keyframe budget holds on long clips.
 */
class BodyMotionGeneratorTest {

  private val spec = BodyMotionState(preset = BodyMotionPreset.WALK)

  @Test
  fun loopMotionSpansTheWholeClipAndReturnsHome() {
    val kfs = BodyMotionGenerator.generate(spec, clipDurationMs = 5000L, playheadRelMs = 0L)
    assertTrue(kfs.isNotEmpty())
    assertEquals(0L, kfs.minOf { it.timeMs })
    val last = kfs.maxBy { it.timeMs }
    assertEquals(5000L, last.timeMs)
    // The pattern ends at the base pose (feet back under the body).
    assertEquals(0f, last.posX, 1e-4f)
    assertEquals(0f, last.posY, 1e-4f)
    // Strictly increasing times.
    val sorted = kfs.sortedBy { it.timeMs }
    assertTrue((1 until sorted.size).all { sorted[it].timeMs > sorted[it - 1].timeMs })
  }

  @Test
  fun noLoopPlaysOneCycleFromThePlayhead() {
    val kfs = BodyMotionGenerator.generate(
      spec.copy(loop = false), clipDurationMs = 8000L, playheadRelMs = 3000L
    )
    assertTrue(kfs.isNotEmpty())
    val cycle = BodyMotionGenerator.cycleDurationMs(spec.speed)
    assertEquals(3000L, kfs.minOf { it.timeMs })
    assertTrue("single cycle must not tile", kfs.maxOf { it.timeMs } <= 3000L + cycle + 1L)
  }

  @Test
  fun speedChangesCycleDensity() {
    val slow = BodyMotionGenerator.generate(spec.copy(speed = 0), 6000L, 0L)
    val fast = BodyMotionGenerator.generate(spec.copy(speed = 100), 6000L, 0L)
    assertTrue(BodyMotionGenerator.cycleDurationMs(0) > BodyMotionGenerator.cycleDurationMs(100))
    assertTrue("faster speed must produce more keyframes in the same span", fast.size > slow.size)
  }

  @Test
  fun smoothPicksTheInterpolation() {
    val linear = BodyMotionGenerator.generate(spec.copy(smooth = 10), 3000L, 0L)
    val eased = BodyMotionGenerator.generate(spec.copy(smooth = 80), 3000L, 0L)
    assertTrue(linear.all { it.interpolation == KeyframeInterpolation.LINEAR })
    assertTrue(eased.all { it.interpolation == KeyframeInterpolation.EASE_IN_OUT })
  }

  @Test
  fun mirrorNegatesHorizontalTravelAndRotation() {
    val normal = BodyMotionGenerator.generate(spec, 4000L, 0L)
    val mirrored = BodyMotionGenerator.generate(spec.copy(mirror = true), 4000L, 0L)
    val normalTilt = normal.maxOf { abs(it.rotation) }
    val mirroredTilt = mirrored.maxOf { abs(it.rotation) }
    assertEquals(normalTilt, mirroredTilt, 1e-3f)
    val normalReach = normal.maxOf { it.posX } - normal.minOf { it.posX }
    val mirroredReach = mirrored.maxOf { it.posX } - mirrored.minOf { it.posX }
    assertEquals(normalReach, mirroredReach, 1e-3f)
  }

  @Test
  fun ikBalanceDampsWanderAndBoundsTheVerticalExcursion() {
    val free = BodyMotionGenerator.generate(spec.copy(ikBalance = false), 4000L, 0L)
    val ik = BodyMotionGenerator.generate(spec.copy(ikBalance = true), 4000L, 0L)
    val freeReach = free.maxOf { it.posX } - free.minOf { it.posX }
    val ikReach = ik.maxOf { it.posX } - ik.minOf { it.posX }
    assertTrue("IK must damp horizontal wander", ikReach < freeReach)
    assertTrue(
      "IK bounds the vertical excursion",
      ik.maxOf { it.posY } - ik.minOf { it.posY } <= 0.11f,
    )
  }

  @Test
  fun rigFocusWeightsChangeWhichChannelsDominate() {
    val full = BodyMotionGenerator.generate(spec.copy(focus = RigFocus.FULL_BODY), 4000L, 0L)
    val legs = BodyMotionGenerator.generate(spec.copy(focus = RigFocus.LEGS), 4000L, 0L)
    val head = BodyMotionGenerator.generate(spec.copy(focus = RigFocus.HEAD), 4000L, 0L)
    val fullBounce = full.maxOf { it.posY } - full.minOf { it.posY }
    val legsBounce = legs.maxOf { it.posY } - legs.minOf { it.posY }
    val fullTilt = full.maxOf { abs(it.rotation) }
    val headTilt = head.maxOf { abs(it.rotation) }
    assertTrue("LEGS emphasis boosts the bounce", legsBounce > fullBounce)
    assertTrue("HEAD emphasis boosts the tilt", headTilt > fullTilt)
  }

  @Test
  fun keyframeBudgetHoldsOnLongClips() {
    // 10 minutes at the fastest speed would tile ~1000 cycles; the budget caps the keys.
    val kfs = BodyMotionGenerator.generate(spec.copy(speed = 100, loop = true), 600_000L, 0L)
    assertTrue(kfs.size <= BodyMotionGenerator.MAX_KEYFRAMES)
    assertTrue(kfs.isNotEmpty())
    assertEquals(600_000L, kfs.maxBy { it.timeMs }.timeMs)
  }

  @Test
  fun baseAnchorOffsetsNeverTeleportTheClip() {
    val kfs = BodyMotionGenerator.generate(spec, 4000L, 0L, baseX = 0.3f, baseY = -0.2f)
    assertTrue(kfs.all { abs(it.posX - 0.3f) <= 0.45f })
    assertTrue(kfs.all { abs(it.posY + 0.2f) <= 0.3f })
    val last = kfs.maxBy { it.timeMs }
    assertEquals(0.3f, last.posX, 1e-4f)
    assertEquals(-0.2f, last.posY, 1e-4f)
  }

  @Test
  fun midPhasePoseCoincidingWithTheEndStillLandsHome() {
    // At 4000ms / speed 50 the WALK phase-0.5 pose coincides exactly with the window end;
    // the landing must REPLACE it so the motion still ends at the base pose.
    val kfs = BodyMotionGenerator.generate(spec, 4000L, 0L)
    val last = kfs.maxBy { it.timeMs }
    assertEquals(4000L, last.timeMs)
    assertEquals(0f, last.posX, 1e-6f)
    assertEquals(0f, last.posY, 1e-6f)
  }

  @Test
  fun everyPresetGeneratesValidMotion() {
    BodyMotionPreset.values().forEach { preset ->
      val kfs = BodyMotionGenerator.generate(BodyMotionState(preset = preset), 4000L, 0L)
      assertTrue("${preset.name} must generate", kfs.size >= 2)
      assertTrue(
        "${preset.name} values must be finite and bounded",
        kfs.all {
          it.posX.isFinite() && it.posY.isFinite() && it.rotation.isFinite() &&
            abs(it.posX) <= 1.5f && abs(it.posY) <= 1.5f && abs(it.rotation) <= 14.5f
        }
      )
    }
  }

  @Test
  fun poseAtInterpolatesBetweenKeys() {
    val kfs = BodyMotionGenerator.generate(spec, 4000L, 0L)
    val mid = BodyMotionGenerator.poseAt(kfs, 2000L)
    assertTrue(mid != null)
    val minX = kfs.minOf { it.posX }
    val maxX = kfs.maxOf { it.posX }
    assertTrue(mid!!.posX in minX..maxX)
    assertEquals(null, BodyMotionGenerator.poseAt(emptyList(), 10L))
  }

  @Test
  fun emptyPresetGeneratesNothing() {
    assertTrue(BodyMotionGenerator.generate(BodyMotionState.DEFAULT, 4000L, 0L).isEmpty())
    assertTrue(BodyMotionGenerator.generate(spec, 0L, 0L).isEmpty())
  }
}
