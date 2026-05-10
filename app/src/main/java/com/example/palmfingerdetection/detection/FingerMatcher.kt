package com.example.palmfingerdetection.detection
import com.example.palmfingerdetection.data.model.MatchResult
import com.example.palmfingerdetection.data.model.MinutiaeRecord
import kotlin.math.sqrt
class FingerMatcher {

    companion object {
        // Minimum confidence score to consider a match valid
        private const val MATCH_THRESHOLD = 0.4f
    }
    /**
     * Compare a captured finger record against all palm records.
     *
     * @param capturedRecord  The record from the finger being scanned now.
     * @param palmRecords     The records from the palm scan (5 records, one per finger).
     * @return MatchResult with match status, matched finger ID, and confidence.
     */
    fun matchFinger(
        capturedRecord: MinutiaeRecord,
        palmRecords: List<MinutiaeRecord>
    ): MatchResult {
        if (palmRecords.isEmpty()) {
            return MatchResult(isMatch = false, confidence = 0f)
        }

        var bestScore = 0f
        var bestMatch: MinutiaeRecord? = null

        for (record in palmRecords) {
            val score = calculateSimilarity(capturedRecord, record)
            if (score > bestScore) {
                bestScore = score
                bestMatch = record
            }
        }

        return MatchResult(
            isMatch = bestScore >= MATCH_THRESHOLD,
            matchedFingerId = if (bestScore >= MATCH_THRESHOLD) bestMatch?.fingerId else null,
            confidence = bestScore
        )
    }
    /**
     * Calculate similarity between two minutiae records.
     *
     * Uses two signals:
     * 1. Landmark distance: How close are the finger's key points?
     *    Lower distance = more similar = higher score.
     * 2. Hash comparison: Do the fingertip images hash to the same value?
     *    (This is a rough check — realistically they won't be identical,
     *    but partial hash matching could be implemented for better accuracy.)
     */
    private fun calculateSimilarity(a: MinutiaeRecord, b: MinutiaeRecord): Float {
        // ─── Landmark Distance Score (0.0 to 0.5) ───
        if (a.landmarks.size != b.landmarks.size || a.landmarks.isEmpty()) {
            return 0f
        }

        var totalDistance = 0f
        for (i in a.landmarks.indices) {
            val dx = a.landmarks[i].x - b.landmarks[i].x
            val dy = a.landmarks[i].y - b.landmarks[i].y
            totalDistance += sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        }
        val avgDistance = totalDistance / a.landmarks.size
        // Convert distance to score: closer = higher score
        val distanceScore = (1f - avgDistance.coerceAtMost(1f)) * 0.5f

        // ─── Hash Similarity Score (0.0 or 0.5) ───
        // Simple exact match. For production, you'd use perceptual hashing.
        val hashScore = if (a.ridgePatternHash == b.ridgePatternHash) 0.5f else 0f

        return distanceScore + hashScore
    }
}