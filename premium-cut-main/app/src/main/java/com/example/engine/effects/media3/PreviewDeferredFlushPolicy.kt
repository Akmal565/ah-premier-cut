package com.example.engine.effects.media3

/**
 * Bounded retry policy for a Media3 look apply that had to be deferred because no preview
 * surface was bound at the moment of the edit ([PreviewEffectApplyPolicy.shouldDeferLookApply]).
 *
 * ## Why this exists (the root cause it removes)
 *
 * `VideoPlaybackEngine.applyActiveLookToPlayers` used to park `pendingMedia3Effects` and return.
 * The parked graph was flushed from exactly two places: the surface-bind callback (which the
 * surface registry never fired while the SAME TextureView stayed bound but reported
 * `isAvailable == false` for a moment — every tool-panel transition does that) and `play()`.
 * A paused editor therefore never flushed: every Filters / Adjust / Color-Grading tick re-deferred,
 * the resident grading node never installed, and the change stayed invisible until some other tool
 * (Chroma Key, Effects) forced a renderer/mode change. Retrying on a short cadence makes the apply
 * land the moment the TextureView is available again — no other tool needed.
 *
 * Pure Kotlin (no `android.*` / `androidx.*`) so the JVM test harness can execute it, the same way
 * it executes [PreviewGraphDecision].
 */
object PreviewDeferredFlushPolicy {

  /**
   * One retry step. Must stay above one display frame at 60 fps (16.6 ms) so a healthy surface
   * is never re-checked more than a frame early, and small enough that a slider drag never
   * perceives a delay: the first retry lands within ~4 frames of the edit.
   */
  const val RETRY_DELAY_MS = 64L

  /**
   * Bounded attempt budget for ONE deferral episode (~1.9 s at [RETRY_DELAY_MS]). Layout
   * transitions around the preview (tool panels, fullscreen dialog churn) settle well inside
   * this window; beyond it the surface is genuinely gone and the next look edit starts a fresh
   * episode instead of spinning a job forever.
   */
  const val MAX_ATTEMPTS = 30

  /**
   * True when the parked look graph must be re-checked after another [RETRY_DELAY_MS] step.
   * A bound surface is NOT retried — the caller flushes immediately in that case.
   */
  fun shouldRetry(attemptsSoFar: Int, surfaceBound: Boolean): Boolean =
    !surfaceBound && attemptsSoFar < MAX_ATTEMPTS

  /** Total worst-case window one episode can cover, for logs and tests. */
  val maxWindowMs: Long get() = RETRY_DELAY_MS * MAX_ATTEMPTS
}
