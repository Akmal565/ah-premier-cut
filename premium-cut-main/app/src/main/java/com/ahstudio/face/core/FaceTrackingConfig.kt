package com.ahstudio.face.core

data class FaceTrackingConfig(
  val maxFaces: Int = 5,
  val minFaceFraction: Float = 0.04f,
  val useLandmarks: Boolean = true,
  val useContours: Boolean = false,
  val useClassification: Boolean = true,
  val mlKitTracking: Boolean = true,
  /**
   * ML Kit performance mode. ACCURATE is the default because AR face filters demand
   * precise landmark positions — the user's reported bug ("cyan markers on the head /
   * blank areas instead of the face") is the exact failure mode FAST produces on
   * non-frontal, occluded, tilted, or low-light faces. ACCURATE on a 640px bitmap is
   * ~50ms, well within the 30fps preview budget. Override to FAST only for known
   * constrained front-camera scenarios.
   */
  val performanceMode: PerformanceMode = PerformanceMode.ACCURATE,
  val detectionIntervalUs: Long = 33_333,
  val smoothing: SmoothingConfig = SmoothingConfig(),
  val association: AssociationConfig = AssociationConfig(),
)

/**
 * Mirrors ML Kit's FaceDetectorOptions performance modes. Kept as a pure-Kotlin enum
 * (no androidx import) so FaceTrackingConfig stays JVM-testable.
 */
enum class PerformanceMode {
  FAST,
  ACCURATE,
}

/** User-facing "trackingSmoothness" 0..1 maps here via [SmoothingConfig.forSmoothness]. */
data class SmoothingConfig(
    val positionMinCutoff: Double = 1.7,
    val positionBeta: Double = 0.35,
    val rotationMinCutoff: Double = 1.2,
    val rotationBeta: Double = 0.20,
    val scaleMinCutoff: Double = 1.0,
    val scaleBeta: Double = 0.10,
    val predictionStrength: Float = 0.35f,
    val maxCoastTimeUs: Long = 400_000,
) {
    companion object {
        fun forSmoothness(s: Float): SmoothingConfig {
            val t = s.toDouble()
            return SmoothingConfig(
                positionMinCutoff = lerp(4.0, 0.7, t),
                positionBeta = lerp(0.8, 0.15, t),
                rotationMinCutoff = lerp(3.0, 0.6, t),
                scaleMinCutoff = lerp(2.5, 0.5, t),
            )
        }
        private fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
    }
}

data class AssociationConfig(
    val iouGate: Float = 0.08f,
    val centroidGate: Float = 0.30f,
    val sizeGate: Float = 1.8f,
    val velocityWeight: Float = 0.35f,
)
