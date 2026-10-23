package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Overlay (PIP) import placement contract. Media imported through the Overlay
 * panel's "Import Gallery" button and the MediaImportPanel overlay target both flow through
 * the single shared factory [buildOverlayPipClip] - one placement, one source-window rule,
 * no duplicated clip-building code paths that can drift apart.
 */
class OverlayPipImportTest {

  private fun clip(isVideo: Boolean) = buildOverlayPipClip(
    uri = "/data/user/0/com.example/files/media/pick.mp4",
    name = "pick.mp4",
    isVideo = isVideo,
    hasAudio = isVideo,
    durationMs = 4200L,
    width = 1920,
    height = 1080,
    naturalRotation = 90,
    frameRate = 30f,
    mimeType = "video/mp4"
  )

  @Test
  fun overlayPipPlacementContract() {
    val c = clip(isVideo = true)
    assertEquals(-1L, c.timelineStartMs) // engine resolves the authoritative playhead time
    assertEquals(0.45f, c.cropScale, 1e-6f) // quarter-frame floating layer
    assertEquals(0.25f, c.cropOffsetX, 1e-6f) // top-right quadrant
    assertEquals(-0.25f, c.cropOffsetY, 1e-6f)
    assertEquals(1.0f, c.opacity, 1e-6f)
    assertEquals("Normal", c.blendMode)
  }

  @Test
  fun sourceWindowCoversWholeMedia() {
    val c = clip(isVideo = true)
    assertEquals(0L, c.sourceStartMs)
    assertEquals(4200L, c.sourceEndMs)
    assertEquals(4200L, c.durationMs)
  }

  @Test
  fun totalDurationSemanticsMatchMediaType() {
    assertEquals(4200L, clip(isVideo = true).sourceTotalDurationMs) // video reports real length
    assertEquals(0L, clip(isVideo = false).sourceTotalDurationMs) // image stays a still
  }

  @Test
  fun mediaAttributesPassThrough() {
    val c = clip(isVideo = true)
    assertEquals("/data/user/0/com.example/files/media/pick.mp4", c.uri)
    assertEquals("pick.mp4", c.name)
    assertTrue(c.isVideo)
    assertTrue(c.hasAudio)
    assertEquals(1920, c.width)
    assertEquals(1080, c.height)
    assertEquals(90, c.naturalRotation)
    assertEquals(30f, c.frameRate, 1e-6f)
    assertEquals("video/mp4", c.mimeType)
  }
}
