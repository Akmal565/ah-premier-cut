package com.example.engine.motion

import com.example.domain.model.ClipKeyframe
import com.example.domain.model.KeyframeInterpolation
import kotlin.math.roundToLong

/**
 * Body motion for the redesigned Expression & Rig panel (Body tab).
 *
 * Each preset is a REAL procedural keyframe generator: applying a motion writes actual
 * [ClipKeyframe]s on the selected clip (position / scale / rotation), so the motion is
 * rendered by the same keyframe interpolator that drives preview AND export. No mock
 * animation, no hard-coded playback: what this object generates is what the renderer traces.
 *
 * Controls (per the panel spec):
 *  - Speed 0..100 maps to the cycle duration (slow 2.6 s .. fast 0.6 s).
 *  - Smooth 0..100 picks keyframe interpolation: < 50 Linear (mechanical), >= 50 Ease-In-Out
 *    (poses flow into each other, no stiff corners).
 *  - Rig focus weights which transform channels move and by how much (Head/Arms/Spine/Legs
 *    emphasize their region of the shared pose table).
 *  - Loop tiles the motion across the whole clip; when off, ONE cycle plays from the playhead.
 *  - Mirror negates horizontal travel and rotation (left/right symmetry).
 *  - IK balance damps horizontal wander, clamps vertical excursion and slightly damps
 *    rotation, so the pose stays anchored ("feet planted, body balanced").
 */
enum class BodyMotionPreset(val label: String) {
  IDLE("Idle"),
  WAVE("Wave"),
  WALK("Walk"),
  NOD("Nod"),
  POINT("Point"),
  DANCE("Dance"),
}

enum class RigFocus(val label: String) {
  FULL_BODY("Full body"),
  HEAD("Head"),
  ARMS("Arms"),
  SPINE("Spine"),
  LEGS("Legs"),
}

/** Panel state for the Body tab (sliders 0..100). */
data class BodyMotionState(
  val preset: BodyMotionPreset? = null,
  val focus: RigFocus = RigFocus.FULL_BODY,
  val speed: Int = 50,
  val smooth: Int = 65,
  val loop: Boolean = true,
  val mirror: Boolean = false,
  val ikBalance: Boolean = true,
) {
  companion object {
    val DEFAULT = BodyMotionState()
  }
}

object BodyMotionGenerator {

  /** Total keyframe budget per clip - keeps runtime interpolation cheap on long clips. */
  const val MAX_KEYFRAMES = 240

  /** One pose inside a cycle: [phase] 0..1 plus offsets from the clip's base transform. */
  data class Pose(
    val phase: Float,
    val dx: Float,
    val dy: Float,
    val rotDeg: Float,
    val sx: Float = 1f,
    val sy: Float = 1f,
  )

  data class FocusWeights(val dx: Float, val dy: Float, val rot: Float, val scale: Float)

  /**
   * The motion tables. First and last pose share the SAME offsets so tiles join seamlessly.
   * Amplitudes stay inside the normalized frame (|dx| <= 0.1, |dy| <= 0.06, |rot| <= 10deg).
   */
  fun cyclePoses(preset: BodyMotionPreset): List<Pose> = when (preset) {
    // Breathing: a barely-there chest rise + micro sway.
    BodyMotionPreset.IDLE -> listOf(
      Pose(0f, 0f, 0f, 0f),
      Pose(0.5f, 0.004f, -0.008f, 0.5f, 1.012f, 1.006f),
      Pose(1f, 0f, 0f, 0f),
    )
    // Arm-wave proxy: side lean with a repeated swing at the top.
    BodyMotionPreset.WAVE -> listOf(
      Pose(0f, 0f, 0f, 0f),
      Pose(0.25f, 0.02f, -0.005f, -8f, 1.008f, 1.0f),
      Pose(0.5f, 0.04f, 0f, -2f, 1.0f, 1.0f),
      Pose(0.75f, 0.02f, -0.005f, -8f, 1.008f, 1.0f),
      Pose(1f, 0f, 0f, 0f),
    )
    // Step cycle: weight shifts left/right with a body bob between the steps.
    BodyMotionPreset.WALK -> listOf(
      Pose(0f, -0.06f, 0f, -2f, 0.995f, 1f),
      Pose(0.25f, 0f, -0.03f, 0f, 1f, 0.99f),
      Pose(0.5f, 0.06f, 0f, 2f, 0.995f, 1f),
      Pose(0.75f, 0f, -0.03f, 0f, 1f, 0.99f),
      Pose(1f, -0.06f, 0f, -2f, 0.995f, 1f),
    )
    // Nod: quick down-dip and recover, twice per cycle.
    BodyMotionPreset.NOD -> listOf(
      Pose(0f, 0f, 0f, 0f),
      Pose(0.3f, 0f, 0.035f, 1.5f),
      Pose(0.55f, 0f, 0.05f, 2f),
      Pose(0.8f, 0f, 0.02f, 1f),
      Pose(1f, 0f, 0f, 0f),
    )
    // Point: lean out to one side and hold, then come back.
    BodyMotionPreset.POINT -> listOf(
      Pose(0f, 0f, 0f, 0f),
      Pose(0.3f, 0.05f, -0.01f, -6f, 1.004f, 1f),
      Pose(0.6f, 0.07f, -0.012f, -8f, 1.006f, 1f),
      Pose(0.8f, 0.06f, -0.008f, -4f, 1.003f, 1f),
      Pose(1f, 0f, 0f, 0f),
    )
    // Dance: everything moves - side to side, bounce, arm swings, squash & stretch.
    BodyMotionPreset.DANCE -> listOf(
      Pose(0f, 0f, 0f, 0f),
      Pose(0.25f, 0.07f, -0.04f, -9f, 0.99f, 1.02f),
      Pose(0.5f, 0f, 0.02f, 0f, 1.01f, 0.98f),
      Pose(0.75f, -0.07f, -0.04f, 9f, 0.99f, 1.02f),
      Pose(1f, 0f, 0f, 0f),
    )
  }

  fun focusWeights(focus: RigFocus): FocusWeights = when (focus) {
    RigFocus.FULL_BODY -> FocusWeights(1f, 1f, 1f, 1f)
    RigFocus.HEAD -> FocusWeights(0.25f, 0.5f, 1.25f, 0f)
    RigFocus.ARMS -> FocusWeights(0.6f, 0.25f, 1.1f, 1.15f)
    RigFocus.SPINE -> FocusWeights(0.7f, 0.6f, 0.8f, 0.5f)
    RigFocus.LEGS -> FocusWeights(0.5f, 1.15f, 0.2f, 0.7f)
  }

  /** Speed 0..100 -> cycle duration 2600ms..600ms (fast movements = shorter cycles). */
  fun cycleDurationMs(speed: Int): Long {
    val s = (speed.coerceIn(0, 100)) / 100f
    return (2600f + (600f - 2600f) * s).roundToLong()
  }

  fun interpolationFor(smooth: Int): KeyframeInterpolation =
    if (smooth >= 50) KeyframeInterpolation.EASE_IN_OUT else KeyframeInterpolation.LINEAR

  /**
   * Generates the motion's real keyframes.
   *
   * @param clipDurationMs total clip length; with [BodyMotionState.loop] the motion tiles the
   *   whole span, otherwise ONE cycle plays from [playheadRelMs].
   * @param baseX/baseY the anchor pose (the clip's position at the apply time) - offsets land
   *   around it, so applying a motion never teleports the clip.
   */
  fun generate(
    state: BodyMotionState,
    clipDurationMs: Long,
    playheadRelMs: Long,
    baseX: Float = 0f,
    baseY: Float = 0f,
  ): List<ClipKeyframe> {
    val preset = state.preset ?: return emptyList()
    if (clipDurationMs <= 0L) return emptyList()
    val poses = cyclePoses(preset)
    val w = focusWeights(state.focus)
    val interp = interpolationFor(state.smooth)

    // Horizontal / vertical / rotation deltas after mirror + IK.
    fun shaped(pose: Pose): Pose {
      var dx = pose.dx * w.dx
      var dy = pose.dy * w.dy
      var rot = pose.rotDeg * w.rot
      val sx = 1f + (pose.sx - 1f) * w.scale
      val sy = 1f + (pose.sy - 1f) * w.scale
      if (state.mirror) {
        dx = -dx
        rot = -rot
      }
      if (state.ikBalance) {
        // Feet planted: kill most horizontal wander, bound the vertical excursion, calm rotation.
        dx *= 0.45f
        dy = dy.coerceIn(-0.05f, 0.05f)
        rot *= 0.9f
      }
      return Pose(pose.phase, dx.coerceIn(-0.45f, 0.45f), dy.coerceIn(-0.3f, 0.3f), rot.coerceIn(-14f, 14f), sx, sy)
    }

    val windowStart = if (state.loop) 0L else playheadRelMs.coerceIn(0L, clipDurationMs)
    val windowEnd = if (state.loop) clipDurationMs else minOf(windowStart + cycleDurationMs(state.speed), clipDurationMs)
    val span = (windowEnd - windowStart).coerceAtLeast(1L)
    val cycle = cycleDurationMs(state.speed).coerceIn(120L, span)

    // Keyframe budget: shorten the effective cycle (fewer tiles) instead of spamming keyframes.
    val posesPerCycle = poses.size - 1 // first pose of cycle N+1 == last pose of cycle N
    val maxCycles = (MAX_KEYFRAMES / posesPerCycle).coerceAtLeast(1)
    val effCycle = if (span / cycle > maxCycles) span / maxCycles else cycle

    val out = ArrayList<ClipKeyframe>(MAX_KEYFRAMES)
    var lastT = -1L
    var t = windowStart
    while (t < windowEnd && out.size < MAX_KEYFRAMES) {
      for (pose in poses) {
        if (out.size >= MAX_KEYFRAMES) break
        if (out.isNotEmpty() && pose.phase == 0f) continue // seam pose == previous cycle end
        val time = t + (pose.phase * effCycle).roundToLong()
        if (time > windowEnd) break
        val p = shaped(pose)
        val clampedT = time.coerceIn(0L, clipDurationMs)
        if (clampedT <= lastT) continue
        lastT = clampedT
        out.add(
          ClipKeyframe(
            timeMs = clampedT,
            posX = baseX + p.dx,
            posY = baseY + p.dy,
            scaleX = p.sx.coerceIn(0.5f, 1.5f),
            scaleY = p.sy.coerceIn(0.5f, 1.5f),
            rotation = p.rotDeg,
            interpolation = interp,
          )
        )
      }
      t += effCycle
      if (!state.loop) break
    }

    // The motion ALWAYS lands home at the window end (loop continuity): append the landing
    // when the window is still open, or REPLACE a pose that coincides with the end.
    // When the keyframe budget is already full, the last key reuses its slot.
    if (out.isNotEmpty()) {
      val landing = ClipKeyframe(
        timeMs = windowEnd,
        posX = baseX,
        posY = baseY,
        scaleX = 1f,
        scaleY = 1f,
        rotation = 0f,
        interpolation = interp,
      )
      val last = out.last()
      when {
        last.timeMs == windowEnd -> out[out.size - 1] = landing
        last.timeMs < windowEnd -> if (out.size < MAX_KEYFRAMES) out.add(landing) else out[out.size - 1] = landing
      }
    }
    return if (out.size >= 2) out else emptyList()
  }

  /** Evaluates the generated pattern at [relTimeMs] (linear scan is fine for <= 240 keys). */
  fun poseAt(keyframes: List<ClipKeyframe>, relTimeMs: Long): ClipKeyframe? {
    if (keyframes.isEmpty()) return null
    val sorted = keyframes.sortedBy { it.timeMs }
    if (relTimeMs <= sorted.first().timeMs) return sorted.first()
    if (relTimeMs >= sorted.last().timeMs) return sorted.last()
    var before = sorted.first()
    var after = sorted.last()
    for (i in 0 until sorted.size - 1) {
      if (relTimeMs >= sorted[i].timeMs && relTimeMs <= sorted[i + 1].timeMs) {
        before = sorted[i]
        after = sorted[i + 1]
        break
      }
    }
    val range = (after.timeMs - before.timeMs).coerceAtLeast(1L)
    val f = ((relTimeMs - before.timeMs).toFloat() / range).coerceIn(0f, 1f)
    return before.copy(
      posX = before.posX + (after.posX - before.posX) * f,
      posY = before.posY + (after.posY - before.posY) * f,
      rotation = before.rotation + (after.rotation - before.rotation) * f,
    )
  }
}
