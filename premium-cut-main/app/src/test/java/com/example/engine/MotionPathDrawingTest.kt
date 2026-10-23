package com.example.engine

import com.example.domain.model.ClipKeyframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Pins the pencil-drawing -> motion-path contract for the Advanced Animation path pad:
 *  - simplify() keeps the drawn shape (endpoints + corners) while dropping jitter,
 *  - strokeToKeyframes() spans the whole clip (first node t=0, last node t=duration),
 *  - node budget is respected, times are strictly increasing,
 *  - constant travel speed (equal arc spacing -> equal time spacing),
 *  - a dot (no travel) converts to nothing so a stray tap can never destroy the path.
 */
class MotionPathDrawingTest {

  private fun line(x0: Float, y0: Float, x1: Float, y1: Float, count: Int): List<Pair<Float, Float>> {
    return (0 until count).map { i ->
      val f = i.toFloat() / (count - 1).toFloat()
      (x0 + (x1 - x0) * f) to (y0 + (y1 - y0) * f)
    }
  }

  @Test
  fun simplifyKeepsEndpointsAndDropsCollinearPoints() {
    val straight = line(0f, 0f, 1f, 1f, 100)
    val simplified = MotionPathDrawing.simplify(straight, 0.01f)
    assertEquals(2, simplified.size)
    assertEquals(0f to 0f, simplified.first())
    assertEquals(1f to 1f, simplified.last())
  }

  @Test
  fun simplifyKeepsCorners() {
    // L-shape: 60 points along the bottom edge, then 60 up the right edge.
    val pts = ArrayList<Pair<Float, Float>>()
    pts.addAll(line(0f, 0f, 1f, 0f, 60))
    pts.addAll(line(1f, 0f, 1f, 1f, 60).drop(1))
    val simplified = MotionPathDrawing.simplify(pts, 0.01f)
    assertTrue("corner must survive, got ${simplified.size} nodes", simplified.size >= 3)
    val hasCorner = simplified.any { abs(it.first - 1f) < 0.05f && abs(it.second - 0f) < 0.05f }
    assertTrue("the (1,0) corner point must be kept", hasCorner)
  }

  @Test
  fun simplifyRemovesPencilJitterOnAStraightStroke() {
    // Straight line with +/-0.005 perpendicular noise (a hand-drawn almost-straight stroke).
    val noisy = line(0f, 0f, 1f, 0f, 120).mapIndexed { i, p ->
      p.first to p.second + if (i % 2 == 0) 0.005f else -0.005f
    }
    val simplified = MotionPathDrawing.simplify(noisy, 0.02f)
    assertTrue("jitter must collapse, got ${simplified.size}", simplified.size <= 3)
    assertTrue(MotionPathDrawing.simplify(noisy, 0.02f).all { it.first.isFinite() && it.second.isFinite() })
  }

  @Test
  fun strokeSpansTheWholeClip() {
    val stroke = line(-0.6f, 0.4f, 0.7f, -0.3f, 200)
    val keys = MotionPathDrawing.strokeToKeyframes(stroke, 4000L)
    assertTrue(keys.size >= 2)
    assertEquals(0L, keys.first().timeMs)
    assertEquals(4000L, keys.last().timeMs)
    for (i in 1 until keys.size) {
      assertTrue("times must strictly increase", keys[i].timeMs > keys[i - 1].timeMs)
    }
    // Positions stay inside the stroke's own bounding box.
    keys.forEach { kf ->
      assertTrue(kf.posX in -0.61f..0.71f)
      assertTrue(kf.posY in -0.31f..0.41f)
    }
  }

  @Test
  fun strokeRespectsNodeBudgetOnWildScribbles() {
    // Dense zig-zag: 900 points, sharp direction changes -> simplify keeps many corners.
    val pts = ArrayList<Pair<Float, Float>>()
    for (i in 0 until 900) {
      val x = i.toFloat() / 899f * 2f - 1f
      val y = if (i % 2 == 0) -0.8f else 0.8f
      pts.add(x to y)
    }
    val keys = MotionPathDrawing.strokeToKeyframes(pts, 6000L, maxNodes = 26)
    assertTrue("budget must hold, got ${keys.size}", keys.size <= 26)
    assertTrue(keys.size >= 2)
    for (i in 1 until keys.size) assertTrue(keys[i].timeMs > keys[i - 1].timeMs)
  }

  @Test
  fun strokeFollowsTheDrawnArcShape() {
    // Parabola y = x^2 from (-1, 1) to (1, 1): the drawn dip must survive conversion.
    val arc = (0 until 240).map { i ->
      val x = -1f + 2f * i / 239f
      x to x * x
    }
    val keys = MotionPathDrawing.strokeToKeyframes(arc, 5000L)
    assertTrue(keys.size >= 2)
    val minY = keys.minOf { it.posY }
    assertTrue("dip must survive, min y was $minY", minY < 0.2f)
    val nearBottom = keys.any { abs(it.posX) < 0.2f && it.posY < 0.3f }
    assertTrue("a node must sit near the parabola bottom (0,0)", nearBottom)
  }

  @Test
  fun constantSpeedEqualArcSpacingGetsEqualTimeSpacing() {
    // Zig-zag with tiny epsilon keeps nearly every corner; after arc-length resampling to a
    // small node count the times must be equally spaced (constant travel speed).
    val pts = ArrayList<Pair<Float, Float>>()
    for (i in 0 until 600) {
      val x = -1f + 2f * i / 599f
      val y = if ((i / 10) % 2 == 0) -0.5f else 0.5f
      pts.add(x to y)
    }
    val keys = MotionPathDrawing.strokeToKeyframes(pts, 9000L, maxNodes = 10)
    assertEquals(10, keys.size)
    val timeGaps = (1 until keys.size).map { keys[it].timeMs - keys[it - 1].timeMs }
    assertTrue("time gaps must be near-equal, was $timeGaps", timeGaps.max() - timeGaps.min() <= 2L)
    assertTrue(keys.all { it.posX.isFinite() && it.posY.isFinite() })
  }

  @Test
  fun evenResamplingHitsExactArcPositions() {
    // L-shape: (0,0) -> (3,0) -> (3,4), total arc = 7. Resampling to 5 must place nodes at
    // arc positions 0, 1.75, 3.5, 5.25, 7 -> (0,0),(1.75,0),(3,0.5),(3,2.25),(3,4).
    val shape = listOf(0f to 0f, 1f to 0f, 2f to 0f, 3f to 0f, 3f to 1f, 3f to 2f, 3f to 3f, 3f to 4f)
    val out = MotionPathDrawing.evenByArcLength(shape, 5)
    assertEquals(5, out.size)
    val expected = listOf(
      0f to 0f,
      1.75f to 0f,
      3f to 0.5f,
      3f to 2.25f,
      3f to 4f
    )
    expected.forEachIndexed { i, (ex, ey) ->
      assertEquals(ex, out[i].first, 1e-4f)
      assertEquals(ey, out[i].second, 1e-4f)
    }
  }

  @Test
  fun dotStrokeConvertsToNothingSoATapCannotDestroyThePath() {
    val dot = List(50) { 0.3f to -0.2f }
    assertTrue(MotionPathDrawing.strokeToKeyframes(dot, 4000L).isEmpty())
    assertTrue(MotionPathDrawing.strokeToKeyframes(emptyList(), 4000L).isEmpty())
    assertTrue(MotionPathDrawing.strokeToKeyframes(line(0f, 0f, 1f, 1f, 5), 0L).isEmpty())
  }

  @Test
  fun twoPointStrokeBecomesExactlyTwoKeyframes() {
    val keys = MotionPathDrawing.strokeToKeyframes(listOf(-0.5f to 0.5f, 0.5f to -0.5f), 3000L)
    assertEquals(2, keys.size)
    assertEquals(0L, keys[0].timeMs)
    assertEquals(3000L, keys[1].timeMs)
    assertEquals(-0.5f, keys[0].posX, 1e-5f)
    assertEquals(0.5f, keys[0].posY, 1e-5f)
  }

  @Test
  fun convertedPathIsRenderableByTheEngine() {
    val arc = (0 until 200).map { i ->
      val x = -1f + 2f * i / 199f
      x to 0.6f * kotlin.math.sin(x * Math.PI).toFloat()
    }
    val keys: List<ClipKeyframe> = MotionPathDrawing.strokeToKeyframes(arc, 4000L)
    val mid = KeyframeInterpolator.evaluatePathPosition(keys, 2000L)
    assertTrue(mid.first.isFinite() && mid.second.isFinite())
    val samples = KeyframeInterpolator.sampleMotionPath(keys, 8)
    assertTrue(samples.size >= keys.size)
    assertTrue(samples.all { it.first.isFinite() && it.second.isFinite() })
  }
}
