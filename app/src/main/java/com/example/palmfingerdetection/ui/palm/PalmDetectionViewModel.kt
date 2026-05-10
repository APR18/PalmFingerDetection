package com.example.palmfingerdetection.ui.palm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.palmfingerdetection.data.model.*

class PalmDetectionViewModel: ViewModel() {
    private val _handResult = MutableLiveData<HandResult>()
    val handResult: LiveData<HandResult> = _handResult

    private val _luminosity = MutableLiveData<LuminosityResult>()
    val luminosity: LiveData<LuminosityResult> = _luminosity

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _palmCaptured = MutableLiveData<Boolean>(false)
    val palmCaptured: LiveData<Boolean> = _palmCaptured



    private var minutiaeRecords: List<MinutiaeRecord> = emptyList()
    private var detectedHandedness: Handedness = Handedness.UNKNOWN
    private var lastCaptureMetadata: CaptureMetadata? = null
    /**
     * Called when the hand detector returns a result (on every frame).
     */
    fun onHandDetected(result: HandResult) {
        _handResult.value = result

        if (result.isDetected && !result.isPalmSide) {
            _errorMessage.value =
                "Palm dorsal side detected, minutiae points won't be extracted."
        }
    }

    /**
     * Called when the luminosity analyzer reports a new reading.
     */
    fun onLuminosityChanged(result: LuminosityResult) {
        _luminosity.value = result
    }

    /**
     * Called after palm image is captured and minutiae are extracted.
     */
    fun onPalmCaptured(
        records: List<MinutiaeRecord>,
        handedness: Handedness,
        metadata: CaptureMetadata
    ) {
        minutiaeRecords = records
        detectedHandedness = handedness
        lastCaptureMetadata = metadata
        _palmCaptured.value = true
    }

    fun setError(message: String) {
        _errorMessage.value = message
    }

    // ─── Getters for data that the FingerDetection screen needs ───

    fun getMinutiaeRecords(): List<MinutiaeRecord> = minutiaeRecords
    fun getDetectedHandedness(): Handedness = detectedHandedness
    fun getCaptureMetadata(): CaptureMetadata? = lastCaptureMetadata
}