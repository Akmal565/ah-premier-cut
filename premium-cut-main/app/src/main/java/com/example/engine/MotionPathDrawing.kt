package com.example.engine

import com.example.domain.model.ClipKeyframe
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Pencil-drawing to motion-path conversion for the Advanced Animation path editor.
 *
 * The pad lets the user sketch a path freehand (like writing with a pencil). The raw stroke is
 * hundreds of dense points; feeding those to the keyframe engine would be wasteful and would
 * make travel jittery. This object turns a stroke into a SMALL, faithful set of keyframes:
 *
 *  1. [simplify]       - Douglas-Peucker polyline simplification: drops pencil jitter while
 *                        keeping the drawn shape's corners within [SIMPLIFY_EPSILON].
 *  2. even resampling  - if the simplified shape still has more points than the node budget,
 *                        it is resampled at EQUAL ARC-LENGTH positions along the drawn curve.
 *  3. [strokeToKeyframes] - assigns times from each node's ARC FRACTION along the drawn shape
 *                        (first point t=0, last point t=duration), so the object travels the
 *                        exact drawn path at constant speed.
 *
 * Timing intentionally uses the arc position ALONG THE DRAWN SHAPE - not chord distances of the
 * final node set. Resampled chords cut across corners and would produce visible fast/slow
 * bursts; the carried arc fraction keeps speed perfectly uniform.
 *
 * Pure maths on Pair<Float, Float> (no Android types) so it stays unit-testable on the JVM and
 * allocation-light on the UI thread (called once per gesture, never per frame).
 */
object MotionPathDrawing {

  /** Jitter threshold for simplify(), in normalized [-1..1] frame units (~1.75% of frame width). */
  private const val SIMPLIFY_EPSILON = 0.035f

  /** Node budget: the engine interpolates per segment, keeping this low keeps rendering cheap. */
  const val MAX_PATH_NODES = 26

  /**
   * Douglas-Peucker polyline simplification (iterative, no recursion-depth risk).
   * Always keeps the first and last point; interior points survive only when they stick out
   * more than [epsilon] from the chord they would be removed from.
   */
  fun simplify(points: List<Pair<Float, Float>>, epsilon: Float): List<Pair<Float, Float>> {
    val n = points.size
    if (n <= 2 || epsilon <= 0f) return points.toList()
    val keep = BooleanArray(n)
    keep[0] = true
    keep[n - 1] = true
    val stack = ArrayDeque<IntArray>()
    stack.addLast(intArrayOf(0, n - 1))
    while (stack.isNotEmpty()) {
      val range = stack.removeLast()
      val start = range[0]
      val end = range[1]
      if (end - start < 2) continue
      val ax = points[start].first
      val ay = points[start].second
      val bx = points[end].first
      val by = points[end].second
      var maxD = -1f
      var maxIdx = -1
      for (i in start + 1 until end) {
        val d = lineDistance(points[i].first, points[i].second, ax, ay, bx, by)
        if (d > maxD) {
          maxD = d
          maxIdx = i
        }
      }
      if (maxD > epsilon && maxIdx > 0) {
        keep[maxIdx] = true
        stack.addLast(intArrayOf(start, maxIdx))
        stack.addLast(intArrayOf(maxIdx, end))
      }
    }
    val out = ArrayList<Pair<Float, Float>>(n)
    for (i in 0 until n) if (keep[i]) out.add(points[i])
    return out
  }

  /**
   * Converts a freehand stroke (normalized space) into engine keyframes.
   *
   * Contract:
   *  - result is empty when the stroke is a dot (no travel) or [durationMs] <= 0;
   *  - otherwise first keyframe sits at timeMs = 0 and the last at timeMs = durationMs;
   *  - times are strictly increasing and follow each node's arc fraction (constant speed);
   *  - node count never exceeds [maxNodes] and never exceeds [durationMs] (>= 1 ms per node);
   *  - every returned position is finite and inside the stroke's own bounding box.
   */
  fun strokeToKeyframes(
    stroke: List<Pair<Float, Float>>,
    durationMs: Long,
    maxNodes: Int = MAX_PATH_NODES
  ): List<ClipKeyframe> {
    if (stroke.size < 2 || durationMs <= 0L) return emptyList()

    // 1. Drop pencil jitter but keep the drawn corners.
    val simplified = simplify(stroke, SIMPLIFY_EPSILON)
    if (simplified.size < 2) return emptyList()

    // 2. Node budget: never more nodes than the budget, and never more than 1 per millisecond
    //    (times must stay strictly increasing at 1 ms resolution).
    val budget = maxNodes.coerceIn(2, durationMs.toInt().coerceAtLeast(2))
    var nodes: List<Triple<Float, Float, Float>> = arcAnnotated(simplified) ?: return emptyList()
    if (nodes.size > budget) nodes = evenByArcLength(simplified, budget)
    if (nodes.size > durationMs) {
      nodes = evenByArcLength(nodes.map { it.first to it.second }, durationMs.toInt().coerceAtLeast(2))
    }

    // 3. Merge consecutive coincident nodes (pen-down pause / exact backtrack) so the arc
    //    fractions - and therefore the times - stay strictly increasing.
    val pts = ArrayList<Triple<Float, Float, Float>>(nodes.size)
    for (nd in nodes) {
      val last = pts.lastOrNull()
      if (last == null || hypot(nd.first - last.first, nd.second - last.second) > 1e-5f) pts.add(nd)
    }
    if (pts.size < 2) return emptyList()

    // 4. Times proportional to each node's arc fraction -> constant speed along the drawn path.
    val n = pts.size
    val out = ArrayList<ClipKeyframe>(n)
    var lastT = 0L
    for (i in 0 until n) {
      val t = when (i) {
        0 -> 0L
        n - 1 -> durationMs
        else -> {
          val raw = (pts[i].third * durationMs).toLong().coerceIn(1L, durationMs - 1L)
          maxOf(raw, lastT + 1).coerceAtMost(durationMs - 1L)
        }
      }
      lastT = t
      out.add(ClipKeyframe(timeMs = t, posX = pts[i].first, posY = pts[i].second))
    }
    return if (out.size >= 2 && out.last().timeMs > out.first().timeMs) out else emptyList()
  }

  /**
   * Resamples a polyline to exactly [target] nodes at equal arc-length positions along the
   * ORIGINAL polyline. Each returned Triple carries (x, y, arcFraction 0..1) where the fraction
   * is the EXACT target position k/(target-1) - by construction, never re-derived from chords.
   */
  internal fun evenByArcLength(points: List<Pair<Float, Float>>, target: Int): List<Triple<Float, Float, Float>> {
    if (points.size <= 2) return arcAnnotated(points) ?: emptyList()
    val cum = cumulativeLengths(points)
    val total = cum.last()
    if (total < 1e-6f) {
      return listOf(
        Triple(points.first().first, points.first().second, 0f),
        Triple(points.last().first, points.last().second, 1f)
      )
    }
    val out = ArrayList<Triple<Float, Float, Float>>(target)
    var seg = 0
    for (k in 0 until target) {
      val frac = k.toFloat() / (target - 1).toFloat()
      val d = total * frac
      while (seg < points.size - 2 && cum[seg + 1] < d) seg++
      val segLen = (cum[seg + 1] - cum[seg]).coerceAtLeast(1e-9f)
      val f = ((d - cum[seg]) / segLen).coerceIn(0f, 1f)
      val a = points[seg]
      val b = points[seg + 1]
      out.add(Triple(a.first + (b.first - a.first) * f, a.second + (b.second - a.second) * f, frac))
    }
    return out
  }

  /** Normalized cumulative arc fractions (0..1) of a polyline, or null for a degenerate dot. */
  private fun arcAnnotated(points: List<Pair<Float, Float>>): List<Triple<Float, Float, Float>>? {
    val cum = cumulativeLengths(points)
    val total = cum.last()
    if (total < 1e-6f) return null
    return points.mapIndexed { i, p -> Triple(p.first, p.second, cum[i] / total) }
  }

  private fun cumulativeLengths(points: List<Pair<Float, Float>>): FloatArray {
    val cum = FloatArray(points.size)
    for (i in 1 until points.size) {
      cum[i] = cum[i - 1] + hypot(
        points[i].first - points[i - 1].first,
        points[i].second - points[i - 1].second
      )
    }
    return cum
  }

  /** Perpendicular distance from p to the infinite line through (a, b); exact for degenerate a==b. */
  private fun lineDistance(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = bx - ax
    val dy = by - ay
    val len = hypot(dx, dy)
    if (len < 1e-9f) return hypot(px - ax, py - ay)
    return abs((px - ax) * dy - (py - ay) * dx) / len
  }
}
