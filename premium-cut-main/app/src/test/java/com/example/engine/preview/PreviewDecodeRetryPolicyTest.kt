package com.example.engine.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the bounded recovery bounds that replaced the one-strike compositor teardown (the
 * reported "filter preview only works after switching tools" device bug):
 *
 *  - a TRANSIENT first-decode failure must be retried, never blacklisted on attempt 1;
 *  - the retry ladder must terminate (attempt bound + wall-clock window), so a genuinely
 *    broken source still reaches the player fallback;
 *  - the engine's activation watchdog must be strictly longer than the retry ladder, must
 *    verify REAL presented frames, and its self-retry budget must be bounded and resettable.
 */
class PreviewDecodeRetryPolicyTest {

  // ------------------------------------------------------------------
  // PreviewDecodeRetryPolicy: the compositor's decode retry ladder
  // ------------------------------------------------------------------

  @Test
  fun firstTransientFailureIsRetriedNotBlacklisted() {
    // The exact device scenario: the compositor's very first decode fails while the player's
    // codec slot is still held. Attempt 1 (and every attempt inside the bound) retries.
    assertTrue(PreviewDecodeRetryPolicy.shouldRetry(failures = 1, elapsedMsSinceFirstFailure = 0L))
    assertTrue(PreviewDecodeRetryPolicy.shouldRetry(failures = 1, elapsedMsSinceFirstFailure = 3_000L))
    assertTrue(PreviewDecodeRetryPolicy.shouldRetry(failures = 2, elapsedMsSinceFirstFailure = 4_000L))
    assertTrue(PreviewDecodeRetryPolicy.shouldRetry(failures = 3, elapsedMsSinceFirstFailure = 6_000L))
  }

  @Test
  fun ladderTerminatesAtTheAttemptBound() {
    assertFalse(PreviewDecodeRetryPolicy.shouldRetry(failures = 4, elapsedMsSinceFirstFailure = 0L))
    assertFalse(PreviewDecodeRetryPolicy.shouldRetry(failures = 5, elapsedMsSinceFirstFailure = 0L))
    assertFalse(PreviewDecodeRetryPolicy.shouldRetry(failures = 40, elapsedMsSinceFirstFailure = 0L))
  }

  @Test
  fun ladderTerminatesAtTheWallClockWindow() {
    // A slow-fail source (4s first-picture deadline per attempt) must not retry forever either:
    // past the window the source is declared undecodable even inside the attempt bound.
    assertFalse(PreviewDecodeRetryPolicy.shouldRetry(failures = 2, elapsedMsSinceFirstFailure = 6_500L))
    assertFalse(PreviewDecodeRetryPolicy.shouldRetry(failures = 2, elapsedMsSinceFirstFailure = 60_000L))
  }

  @Test
  fun zeroAndNegativeAttemptsNeverRetry() {
    // The ladder counts real attempts (1-based); a zero/negative count is a caller bug and
    // must never be treated as "still inside the bound".
    assertFalse(PreviewDecodeRetryPolicy.shouldRetry(failures = 0, elapsedMsSinceFirstFailure = 0L))
    assertFalse(PreviewDecodeRetryPolicy.shouldRetry(failures = -1, elapsedMsSinceFirstFailure = 0L))
  }

  @Test
  fun retryPulseStaysSmallEnoughForAPausedPreview() {
    // The paused-preview re-pump pulse must stay far below a perceivable lag; it exists only
    // so a paused playhead does not wait for the next user interaction to retry.
    assertTrue(PreviewDecodeRetryPolicy.RETRY_PULSE_MS in 1..250L)
  }

  // ------------------------------------------------------------------
  // CompositorActivationPolicy: the engine's presentation watchdog
  // ------------------------------------------------------------------

  @Test
  fun watchdogIsStrictlyLongerThanTheRetryLadder() {
    // The compositor's own ladder must finish before the engine declares the activation dead,
    // otherwise the watchdog would cancel a recovery that was still in flight.
    assertTrue(
      "PRESENTATION_PROBE_MS (${CompositorActivationPolicy.PRESENTATION_PROBE_MS}ms) must exceed " +
        "RETRY_WINDOW_MS (${PreviewDecodeRetryPolicy.RETRY_WINDOW_MS}ms)",
      CompositorActivationPolicy.PRESENTATION_PROBE_MS > PreviewDecodeRetryPolicy.RETRY_WINDOW_MS
    )
  }

  @Test
  fun presentationEvidenceIsRealRenderedFramesBeyondTheBaseline() {
    assertFalse(CompositorActivationPolicy.presentedSince(baseline = 0L, renderedFrames = 0L))
    assertTrue(CompositorActivationPolicy.presentedSince(baseline = 0L, renderedFrames = 1L))
    assertTrue(CompositorActivationPolicy.presentedSince(baseline = 41L, renderedFrames = 42L))
    assertFalse(CompositorActivationPolicy.presentedSince(baseline = 42L, renderedFrames = 42L))
  }

  @Test
  fun selfRetryBudgetIsBounded() {
    assertTrue(CompositorActivationPolicy.canSelfRetry(0))
    assertTrue(CompositorActivationPolicy.canSelfRetry(1))
    assertFalse(CompositorActivationPolicy.canSelfRetry(2))
    assertFalse(CompositorActivationPolicy.canSelfRetry(3))
    assertEquals(2, CompositorActivationPolicy.MAX_SELF_RETRIES)
  }
}
