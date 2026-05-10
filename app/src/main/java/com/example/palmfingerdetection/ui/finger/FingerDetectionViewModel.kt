package com.example.palmfingerdetection.ui.finger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.palmfingerdetection.data.model.*
import com.example.palmfingerdetection.detection.FingerMatcher
class FingerDetectionViewModel: ViewModel (){
    private val fingerMatcher = FingerMatcher()

    // Which finger we're currently scanning (0–4)
    private val _currentFingerIndex = MutableLiveData(0)
    val currentFingerIndex: LiveData<Int> = _currentFingerIndex

    // Result of the latest finger match attempt
    private val _matchResult = MutableLiveData<MatchResult>()
    val matchResult: LiveData<MatchResult> = _matchResult

    // Error messages
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // All 5 fingers captured successfully
    private val _allFingersCaptured = MutableLiveData(false)
    val allFingersCaptured: LiveData<Boolean> = _allFingersCaptured

    // Stored palm records (set when fragment starts)
    private var palmRecords: List<MinutiaeRecord> = emptyList()
    private var expectedHandedness: Handedness = Handedness.UNKNOWN

    private val fingerNames = listOf("Thumb", "Index", "Middle", "Ring", "Little")

    /**
     * Set the palm records for validation. Called once when this screen opens.
     */
    fun setPalmData(records: List<MinutiaeRecord>, handedness: Handedness) {
        palmRecords = records
        expectedHandedness = handedness
    }

    fun getCurrentFingerName(): String {
        val index = _currentFingerIndex.value ?: 0
        return fingerNames.getOrElse(index) { "Unknown" }
    }

    /**
     * Validate a captured finger against the palm records.
     *
     * This implements the assignment's requirements:
     * - If finger doesn't match ANY palm record → "Finger does not match"
     * - If finger matches but from wrong hand → "Incorrect Finger"
     * - If matches correctly → show toast with finger name, advance counter
     */
    fun validateFinger(
        capturedRecord: MinutiaeRecord,
        detectedHandedness: Handedness
    ) {
        // Step 1: Check if the finger matches any palm record
        val result = fingerMatcher.matchFinger(capturedRecord, palmRecords)

        if (!result.isMatch) {
            _errorMessage.value = "Finger does not match"
            _matchResult.value = result
            return
        }

        // Step 2: Check if it's from the correct hand
        val matchedHand = when {
            result.matchedFingerId?.startsWith("Left") == true -> Handedness.LEFT
            result.matchedFingerId?.startsWith("Right") == true -> Handedness.RIGHT
            else -> Handedness.UNKNOWN
        }

        if (matchedHand != expectedHandedness) {
            _errorMessage.value = "Incorrect Finger"
            _matchResult.value = MatchResult(isMatch = false, confidence = result.confidence)
            return
        }

        // Step 3: Match is valid — advance to next finger
        _matchResult.value = result

        val nextIndex = (_currentFingerIndex.value ?: 0) + 1
        if (nextIndex >= 5) {
            _allFingersCaptured.value = true
        } else {
            _currentFingerIndex.value = nextIndex
        }
    }

    fun onDorsalDetected() {
        _errorMessage.value =
            "Finger dorsal side detected, please show palm side finger which contains finger record or minutiae points"
    }
}