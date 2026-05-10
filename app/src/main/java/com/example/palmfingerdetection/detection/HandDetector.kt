package com.example.palmfingerdetection.detection
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.example.palmfingerdetection.data.model.HandResult
import com.example.palmfingerdetection.data.model.Handedness
class HandDetector (private val context: Context){
    companion object {
        private const val TAG = "HandDetector"
        private const val MODEL_PATH = "hand_landmarker.task"
    }
    private var handLandmarker: HandLandmarker? = null
    private var lastRawLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> = emptyList()
    fun initialize() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_PATH)
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.d(TAG, "✅ HandLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize HandLandmarker: ${e.message}", e)
        }
    }


    fun detect(bitmap: Bitmap): HandResult {
        val landmarker = handLandmarker
        if (landmarker == null) {
            Log.e(TAG, "❌ handLandmarker is NULL — initialize() failed or wasn't called")
            return HandResult(isDetected = false)
        }

        try {
            Log.d(TAG, "🔍 Running detection on bitmap: ${bitmap.width}x${bitmap.height}")

            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detect(mpImage)

            Log.d(TAG, "🔍 Landmarks found: ${result.landmarks().size}")
            Log.d(TAG, "🔍 Handedness found: ${result.handednesses().size}")

            if (result.landmarks().isEmpty()) {
                Log.d(TAG, "⚠️ No hand detected in this frame")
                return HandResult(isDetected = false)
            }

            val landmarks = result.landmarks()[0]
            lastRawLandmarks = landmarks

            val pointFList = landmarks.map { lm ->
                PointF(lm.x(), lm.y())
            }

            val handedness = determineHandedness(result.handednesses()[0])
            val fingerCount = countFingers(landmarks)
            val isPalmSide = isPalmFacingCamera(landmarks)

            Log.d(TAG, "✅ Hand detected! Handedness=$handedness, Fingers=$fingerCount, Palm=$isPalmSide")

            return HandResult(
                isDetected = true,
                landmarks = pointFList,
                handedness = handedness,
                fingerCount = fingerCount,
                isPalmSide = isPalmSide
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Hand detection crashed: ${e.message}", e)
            return HandResult(isDetected = false)
        }
    }
    /**
     * Determine if the hand is left or right.
     *
     * MediaPipe's label is from the camera's perspective (mirrored).
     * For rear camera: "Left" = user's left hand.
     */
    private fun determineHandedness(
        categories: List<com.google.mediapipe.tasks.components.containers.Category>
    ): Handedness {
        val label = categories.firstOrNull()?.categoryName() ?: return Handedness.UNKNOWN
        return when {
            label.equals("Left", ignoreCase = true) -> Handedness.LEFT
            label.equals("Right", ignoreCase = true) -> Handedness.RIGHT
            else -> Handedness.UNKNOWN
        }
    }

    /**
     * Count extended (open) fingers.
     *
     * Logic for each finger:
     * - For index/middle/ring/pinky: if the FINGERTIP is ABOVE (lower y value)
     *   the PIP joint, the finger is extended.
     * - For thumb: compare horizontal distance (x) between TIP and IP joint.
     *
     * Why y comparison? In image coordinates, y=0 is the TOP of the image.
     * So a fingertip that is "up" has a LOWER y value than its knuckle.
     */
    private fun countFingers(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>
    ): Int {
        var count = 0

        // ─── THUMB ───
        // Simply check if thumb TIP (4) is above thumb MCP (2)
        // OR if thumb TIP is far enough horizontally from index MCP (5)
        val thumbTip = landmarks[4]
        val thumbMCP = landmarks[2]
        val indexMCP = landmarks[5]

        val horizontalDist = Math.abs(thumbTip.x() - indexMCP.x())
        val verticalCheck = thumbTip.y() < thumbMCP.y()

        // Thumb is extended if EITHER condition is true
        if (horizontalDist > 0.01f || verticalCheck) {
            count++
        }

        // ─── INDEX FINGER ───
        if (landmarks[8].y() < landmarks[6].y()) count++

        // ─── MIDDLE FINGER ───
        if (landmarks[12].y() < landmarks[10].y()) count++

        // ─── RING FINGER ───
        if (landmarks[16].y() < landmarks[14].y()) count++

        // ─── PINKY ───
        if (landmarks[20].y() < landmarks[18].y()) count++

        return count
    }
    /**
     * Detect whether the PALM side or DORSAL (back) side is facing the camera.
     *
     * Method: Use the cross product of vectors from wrist to index MCP
     * and wrist to pinky MCP. The sign of the z-component tells us
     * the hand orientation.
     *
     * This is a heuristic and may need tuning based on camera orientation.
     */
    private fun isPalmFacingCamera(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>
    ): Boolean {
        val wrist = landmarks[0]
        val indexMCP = landmarks[5]
        val pinkyMCP = landmarks[17]

        // 2D cross product z-component
        val cross = (indexMCP.x() - wrist.x()) * (pinkyMCP.y() - wrist.y()) -
                (indexMCP.y() - wrist.y()) * (pinkyMCP.x() - wrist.x())

        // Positive = palm side (for rear camera)
        // You may need to flip this for front camera
        return cross > 0
    }
    fun getLastRawLandmarks(): List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> {
        return lastRawLandmarks
    }
    /**
     * Release the ML model from memory. Call when done.
     */
    fun release() {
        handLandmarker?.close()
        handLandmarker = null
    }

}