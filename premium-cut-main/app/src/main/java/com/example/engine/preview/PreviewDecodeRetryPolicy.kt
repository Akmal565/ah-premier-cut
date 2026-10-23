package com.example.engine.preview

/**
 * Recovery rules for the GPU preview compositor's activation on real hardware.
 *
 * The reported device bug: the FIRST filter tap activated the compositor, its first decode
 * failed transiently (the main player still held the codec slot for the same file, or the
 * first picture missed the deadline on a cold media stack), and one strike tore the whole
 * compositor down, latched `compositorFailed` for a cooldown, and left the retry to the NEXT
 * unrelated user action. On the fallback (player) path that device class draws no look at all,
 * so every tap stayed invisible until Chroma Key / Effects happened to be used later.
 *
 * Two bounded, event-driven recoveries replace the one-strike teardown - both are real
 * failure signals, not refresh loops:
 *
 *  1. [PreviewDecodeRetryPolicy] (inside the compositor): a failed main-layer decode/open is
 *     retried on the following frames up to [MAX_CONSECUTIVE_FAILURES] attempts within
 *     [RETRY_WINDOW_MS]. Only a source that keeps failing is blacklisted and handed to the
 *     engine's player fallback. A paused preview is re-pumped by a single delayed generation
 *     pulse per failure ([RETRY_PULSE_MS]) because a paused playhead alone would never
 *     schedule the next render attempt.
 *
 *  2. [CompositorActivationPolicy] (in the engine): an activation that presented NO frame on
 *     its attached surface within [PRESENTATION_PROBE_MS] is treated as a failed activation
 *     (real rendered-frame evidence, not the "active" flag), falls back once, and self-retries
 *     after the cooldown at most [MAX_SELF_RETRIES] times. The counter resets whenever a
 *     compositor that had actually presented frames is torn down, so a later genuine breakage
 *     gets fresh retries.
 *
 * Pure Kotlin so the JVM test harness pins every bound.
 */
object PreviewDecodeRetryPolicy {

  /** Consecutive failed attempts before a clip is declared undecodable (player fallback). */
  const val MAX_CONSECUTIVE_FAILURES = 4

  /** Wall-clock window the retry ladder may span; expiry blacklists like exhaustion does. */
  const val RETRY_WINDOW_MS = 6_500L

  /**
   * Delay of the single generation pulse that re-pumps a PAUSED preview after a retryable
   * failure. Playing previews retry on the next vsync anyway; the pulse only exists so a
   * paused playhead is not stuck waiting for a user interaction.
   */
  const val RETRY_PULSE_MS = 120L

  /** True when attempt number [failures] (1-based) still deserves another attempt. */
  fun shouldRetry(failures: Int, elapsedMsSinceFirstFailure: Long): Boolean =
    failures in 1 until MAX_CONSECUTIVE_FAILURES && elapsedMsSinceFirstFailure < RETRY_WINDOW_MS
}

/** Bounds of the engine-side, presentation-verified activation watchdog (see the class kdoc). */
object CompositorActivationPolicy {

  /**
   * How long an activation may stay silent (no presented frame on its attached surface) before
   * it is treated as a failed activation. Must exceed [PreviewDecodeRetryPolicy.RETRY_WINDOW_MS]
   * so the compositor's own retry ladder finishes first.
   */
  const val PRESENTATION_PROBE_MS = 9_000L

  /** Maximum engine-driven re-activations per failure streak (no refresh loop). */
  const val MAX_SELF_RETRIES = 2

  fun canSelfRetry(retries: Int): Boolean = retries < MAX_SELF_RETRIES

  /** Real presentation evidence: at least one frame beyond [baseline] was rendered. */
  fun presentedSince(baseline: Long, renderedFrames: Long): Boolean = renderedFrames > baseline
}
