package com.example.engine.effects.media3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the bounded retry policy that flushes a deferred Media3 look apply.
 *
 * The bug this locks down: a look edited while the preview TextureView reported
 * `isAvailable == false` (every tool-panel transition) was parked forever — the flush only ran on
 * a NEW bind event or on play(). Filters / Adjust / Color-Grading changes stayed invisible until
 * another tool (Chroma Key, Effects) forced a renderer change.
 */
class PreviewDeferredFlushPolicyTest {

  @Test
  fun deferredLookIsRetriedWhileTheSurfaceIsUnavailable() {
    assertTrue(PreviewDeferredFlushPolicy.shouldRetry(attemptsSoFar = 0, surfaceBound = false))
    assertTrue(PreviewDeferredFlushPolicy.shouldRetry(attemptsSoFar = 15, surfaceBound = false))
  }

  @Test
  fun aBoundSurfaceIsFlushedImmediatelyInsteadOfRetried() {
    assertFalse(PreviewDeferredFlushPolicy.shouldRetry(attemptsSoFar = 0, surfaceBound = true))
    assertFalse(PreviewDeferredFlushPolicy.shouldRetry(attemptsSoFar = 29, surfaceBound = true))
  }

  @Test
  fun retriesAreBoundedSoAGoneSurfaceCannotSpinTheJobForever() {
    assertTrue(PreviewDeferredFlushPolicy.shouldRetry(attemptsSoFar = PreviewDeferredFlushPolicy.MAX_ATTEMPTS - 1, surfaceBound = false))
    assertFalse(PreviewDeferredFlushPolicy.shouldRetry(attemptsSoFar = PreviewDeferredFlushPolicy.MAX_ATTEMPTS, surfaceBound = false))
    assertFalse(PreviewDeferredFlushPolicy.shouldRetry(attemptsSoFar = PreviewDeferredFlushPolicy.MAX_ATTEMPTS + 5, surfaceBound = false))
  }

  @Test
  fun retryStepIsImperceptibleButNeverFasterThanADisplayFrame() {
    // At 60 fps one display frame is ~16.6ms: the retry must not spin faster than that ...
    assertTrue(
      "RETRY_DELAY_MS must stay above one 60fps display frame",
      PreviewDeferredFlushPolicy.RETRY_DELAY_MS >= 32L
    )
    // ... and small enough that the first retry lands within ~4 frames of the edit.
    assertTrue(
      "RETRY_DELAY_MS must not introduce a perceptible delay after a slider tick",
      PreviewDeferredFlushPolicy.RETRY_DELAY_MS <= 100L
    )
  }

  @Test
  fun episodeWindowCoversRealLayoutTransitions() {
    // Tool-panel transitions and fullscreen dialog churn settle in well under 2s; the episode
    // budget must cover them, otherwise the defer bug returns on slow devices.
    assertTrue(
      "One defer episode must cover at least one second of transient unavailability",
      PreviewDeferredFlushPolicy.maxWindowMs >= 1_000L
    )
    assertEquals(
      PreviewDeferredFlushPolicy.RETRY_DELAY_MS * PreviewDeferredFlushPolicy.MAX_ATTEMPTS,
      PreviewDeferredFlushPolicy.maxWindowMs
    )
  }
}
