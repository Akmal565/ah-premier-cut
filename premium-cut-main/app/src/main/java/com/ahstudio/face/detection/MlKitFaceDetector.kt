package com.ahstudio.face.detection

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.ahstudio.face.core.FaceLandmarkType
import com.ahstudio.face.core.FaceTrackingConfig
import com.ahstudio.face.core.Vec2
import kotlinx.coroutines.tasks.await

/** The ONLY file allowed to import ML Kit types. */
class MlKitFaceDetector(private val cfg: FaceTrackingConfig) : FaceDetector {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(
            when (cfg.performanceMode) {
                com.ahstudio.face.core.PerformanceMode.FAST -> FaceDetectorOptions.PERFORMANCE_MODE_FAST
                com.ahstudio.face.core.PerformanceMode.ACCURATE -> FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
            }
        )
        .setLandmarkMode(if (cfg.useLandmarks) FaceDetectorOptions.LANDMARK_MODE_ALL else FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setContourMode(if (cfg.useContours) FaceDetectorOptions.CONTOUR_MODE_ALL else FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(if (cfg.useClassification) FaceDetectorOptions.CLASSIFICATION_MODE_ALL else FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(cfg.minFaceFraction)
        .apply {
            if (cfg.mlKitTracking) enableTracking()
        }
        .build()

    private val client by lazy { FaceDetection.getClient(options) }

    /** Failure telemetry: a persistent detector failure must not be indistinguishable from "no face". */
    private var consecutiveFailures = 0L

    override suspend fun detect(frame: DetectionFrame, maxFaces: Int): List<RawFaceDetection> {
        val image = when (frame) {
            is DetectionFrame.BitmapFrame -> InputImage.fromBitmap(frame.bitmap, 0)
            is DetectionFrame.YuvImageFrame -> InputImage.fromMediaImage(frame.image, frame.rotationDegrees)
        }
        return try {
            val faces = client.process(image).await().take(maxFaces).map { f ->
                val r = f.boundingBox
                RawFaceDetection(
                    boundsPx = BoundsPx(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat()),
                    eulerX = f.headEulerAngleX,
                    eulerY = f.headEulerAngleY,
                    eulerZ = f.headEulerAngleZ,
                    landmarksPx = buildMap {
                        f.allLandmarks.forEach {
                            val type = mlType(it.landmarkType) ?: return@forEach
                            put(type, Vec2(it.position.x.toFloat(), it.position.y.toFloat()))
                        }
                    },
                    smiling = f.smilingProbability?.takeUnless { p -> p.isNaN() },
                    leftEyeOpen = f.leftEyeOpenProbability?.takeUnless { p -> p.isNaN() },
                    rightEyeOpen = f.rightEyeOpenProbability?.takeUnless { p -> p.isNaN() },
                    trackingId = if (cfg.mlKitTracking) f.trackingId else null,
                )
            }
            consecutiveFailures = 0
            faces
        } catch (t: Throwable) {
            consecutiveFailures++
            // Log the first failure of a burst and then every 30th, so a persistently broken
            // detector (model not downloaded yet, decode error) shows up in logcat without spam.
            if (consecutiveFailures == 1L || consecutiveFailures % 30L == 0L) {
                android.util.Log.w(
                    TAG,
                    "Face detection failed (x$consecutiveFailures): " +
                        "${t.javaClass.simpleName}: ${t.message ?: "unknown error"}"
                )
            }
            emptyList()
        }
    }

    override fun close() {
        runCatching { client.close() }
    }

    /**
     * Maps ML Kit landmark type ids to our [FaceLandmarkType]. Returns null for unknown
     * types so the caller can filter them out — previously unknown types were aliased to
     * NOSE_BASE, which made nose-anchored stickers (halo, visor) briefly snap to the ear
     * on profile turns.
     */
    private fun mlType(t: Int): FaceLandmarkType? = when (t) {
        FaceLandmark.LEFT_EYE -> FaceLandmarkType.LEFT_EYE
        FaceLandmark.RIGHT_EYE -> FaceLandmarkType.RIGHT_EYE
        FaceLandmark.NOSE_BASE -> FaceLandmarkType.NOSE_BASE
        FaceLandmark.MOUTH_LEFT -> FaceLandmarkType.MOUTH_LEFT
        FaceLandmark.MOUTH_BOTTOM -> FaceLandmarkType.MOUTH_BOTTOM
        FaceLandmark.MOUTH_RIGHT -> FaceLandmarkType.MOUTH_RIGHT
        FaceLandmark.LEFT_CHEEK -> FaceLandmarkType.LEFT_CHEEK
        FaceLandmark.RIGHT_CHEEK -> FaceLandmarkType.RIGHT_CHEEK
        FaceLandmark.LEFT_EAR -> FaceLandmarkType.LEFT_EAR_LOBE
        FaceLandmark.RIGHT_EAR -> FaceLandmarkType.RIGHT_EAR_LOBE
        // LEFT_EAR_TIP / RIGHT_EAR_TIP exist in ML Kit's newer SDK but not in the version
        // this project ships with — the compile error 'Unresolved reference' was caught by CI.
        // They are handled by the `else -> null` branch (filtered out, not aliased to NOSE_BASE).
        else -> null
    }

    private companion object {
        const val TAG = "MlKitFaceDetector"
    }
}
