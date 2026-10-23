package com.example.engine.playback

import androidx.media3.common.PlaybackException
import com.example.engine.effects.media3.PreviewEffectApplyPolicy

/**
 * Recovery / session-reset decisions for the live preview pipeline.
 *
 * Kept as a pure policy so unit tests can lock the fallback order without constructing
 * ExoPlayer or a VideoFrameProcessor:
 *   1. restore the last graph that rendered a frame
 *   2. install the identity / passthrough transform
 *   3. recreate ExoPlayer + MediaCodec + VideoFrameProcessor when the processor is dead
 */
object PreviewPipelinePolicy {

  enum class RecoveryStep {
    RESTORE_LAST_GOOD,
    APPLY_IDENTITY,
    RECREATE_PLAYER
  }

  fun nextRecoveryStep(
    lastGoodEffectCount: Int,
    lastRestoreSucceeded: Boolean,
    identityApplySucceeded: Boolean,
    playerHasError: Boolean
  ): RecoveryStep {
    if (lastGoodEffectCount > 0 && !lastRestoreSucceeded && !playerHasError) {
      return RecoveryStep.RESTORE_LAST_GOOD
    }
    if (!identityApplySucceeded && !playerHasError) {
      return RecoveryStep.APPLY_IDENTITY
    }
    if (playerHasError || !identityApplySucceeded) {
      return RecoveryStep.RECREATE_PLAYER
    }
    return RecoveryStep.APPLY_IDENTITY
  }

  fun shouldRecreatePlayer(error: PlaybackException?, applyFailed: Boolean): Boolean {
    if (applyFailed) return true
    val e = error ?: return false
    return PreviewEffectApplyPolicy.isVideoProcessingFailure(e) ||
      e.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
      e.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
  }

  /**
   * What the preview must do after the player's video pipeline reported a processing failure
   * for a look graph (VideoFrameProcessor init / frame processing failed).
   *
   * The synchronous fallback path (`applyActiveLookToPlayers` FALLBACK_IDENTITY branch) already
   * hands look rendering to the GPU compositor. The asynchronous path — `onPlayerError` arriving
   * AFTER `setVideoEffects` returned APPLIED — must do the same, or a device whose Media3 frame
   * processor cannot run the grading graph rolls back to the identity passthrough on EVERY
   * look tap: filters stay invisible until Chroma Key / Effects activate the compositor, which
   * is exactly the "filter preview only works after switching tools" report.
   *
   * `compositorPreviouslyFailed` keeps a device with TWO broken renderers from flip-flopping:
   * once the compositor has failed for this project, stay on the player path and report.
   */
  enum class ProcessingFailureAction { COMPOSITOR_TAKEOVER, KEEP_PLAYER_PATH }

  fun actionAfterProcessingFailure(
    isVideoProcessingFailure: Boolean,
    compositorAlreadyActive: Boolean,
    compositorPreviouslyFailed: Boolean
  ): ProcessingFailureAction =
    if (isVideoProcessingFailure && !compositorAlreadyActive && !compositorPreviouslyFailed) {
      ProcessingFailureAction.COMPOSITOR_TAKEOVER
    } else {
      ProcessingFailureAction.KEEP_PLAYER_PATH
    }

  fun shouldResetOnProjectChange(): Boolean = true
}
