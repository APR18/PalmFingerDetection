package com.example.palmfingerdetection.detection

import android.graphics.Bitmap
import android.graphics.PointF
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.example.palmfingerdetection.data.model.Handedness
import com.example.palmfingerdetection.data.model.MinutiaeRecord
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
class MinutiaeExtractor {

    companion object {
        // MediaPipe landmark indices for each fingertip
        val FINGER_TIP_INDICES = listOf(4, 8, 12, 16, 20)
        val FINGER_NAMES = listOf("Thumb", "Index", "Middle", "Ring", "Little")
    }

    /**
     * Extract minutiae records for all 5 fingers from a palm image.
     *
     * @param bitmap     The captured palm image.
     * @param landmarks  21 hand landmarks from MediaPipe.
     * @param handedness Which hand (left/right).
     * @return List of 5 MinutiaeRecords, one per finger.
     */
    fun extractFromPalm(
        bitmap: Bitmap,
        landmarks: List<NormalizedLandmark>,
        handedness: Handedness
    ): List<MinutiaeRecord> {
        val hand = if (handedness == Handedness.LEFT) "Left" else "Right"
        val records = mutableListOf<MinutiaeRecord>()

        for (i in FINGER_NAMES.indices) {
            val tipIdx = FINGER_TIP_INDICES[i]

            // Convert normalized coordinates (0.0-1.0) to pixel coordinates
            val cx = (landmarks[tipIdx].x() * bitmap.width).toInt()
            val cy = (landmarks[tipIdx].y() * bitmap.height).toInt()

            // Crop a small region around the fingertip
            val cropSize = 80  // pixels
            val left = (cx - cropSize).coerceAtLeast(0)
            val top = (cy - cropSize).coerceAtLeast(0)
            val right = (cx + cropSize).coerceAtMost(bitmap.width)
            val bottom = (cy + cropSize).coerceAtMost(bitmap.height)

            // Safety check: make sure crop area is valid
            if (right <= left || bottom <= top) continue

            val fingerCrop = Bitmap.createBitmap(
                bitmap, left, top, right - left, bottom - top
            )

            // Extract key landmark points for this finger
            val keyPoints = extractKeyPoints(landmarks, tipIdx)

            // Hash the fingertip image for later comparison
            val hash = computeImageHash(fingerCrop)

            records.add(
                MinutiaeRecord(
                    fingerId = "${hand}_Hand_${FINGER_NAMES[i]}_Finger",
                    landmarks = keyPoints,
                    ridgePatternHash = hash
                )
            )
        }

        return records
    }


    /**
     * Extract minutiae from a single close-up finger image.
     * Used during the finger scanning phase.
     */
    fun extractFromFinger(
        bitmap: Bitmap,
        landmarks: List<NormalizedLandmark>,
        handedness: Handedness,
        fingerIndex: Int   // 0=Thumb, 1=Index, 2=Middle, 3=Ring, 4=Little
    ): MinutiaeRecord {
        val hand = if (handedness == Handedness.LEFT) "Left" else "Right"
        val tipIdx = FINGER_TIP_INDICES[fingerIndex]
        val keyPoints = extractKeyPoints(landmarks, tipIdx)
        val hash = computeImageHash(bitmap)

        return MinutiaeRecord(
            fingerId = "${hand}_Hand_${FINGER_NAMES[fingerIndex]}_Finger",
            landmarks = keyPoints,
            ridgePatternHash = hash
        )
    }
    /**
     * Extract 3 key reference points for a finger:
     * TIP, DIP (joint near tip), PIP (middle joint).
     */
    private fun extractKeyPoints(
        landmarks: List<NormalizedLandmark>,
        tipIdx: Int
    ): List<PointF> {
        return listOf(
            PointF(landmarks[tipIdx].x(), landmarks[tipIdx].y()),       // Tip
            PointF(landmarks[tipIdx - 1].x(), landmarks[tipIdx - 1].y()), // DIP
            PointF(landmarks[tipIdx - 2].x(), landmarks[tipIdx - 2].y())  // PIP
        )
    }
    /**
     * Compute a SHA-256 hash of a bitmap image.
     * Two images of the same finger under similar conditions should produce
     * similar (but not identical) hashes. This is a simplified approach.
     */
    private fun computeImageHash(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}