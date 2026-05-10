package com.example.palmfingerdetection.data.model

import android.graphics.PointF
data class HandResult(
    val isDetected:Boolean,
    val landmarks: List<PointF> = emptyList(),
    val handedness: Handedness = Handedness.UNKNOWN,
    val fingerCount: Int = 0,
    val isPalmSide:Boolean = true
)


enum class  Handedness{
    LEFT, RIGHT, UNKNOWN
}

data class MinutiaeRecord(
    val fingerId: String,           // e.g., "Left_Hand_Index_Finger"
    val landmarks: List<PointF>,    // Key reference points from MediaPipe
    val ridgePatternHash: String    // A hash of the fingertip image for comparison
)

data class MatchResult(
    val isMatch: Boolean,
    val matchedFingerId:String? = null,
    val confidence: Float = 0f
)


// Camera meta data
data class CaptureMetadata(
    val deviceId:String,
     val brightnessScore:Double,
    val cameraType:String,
    val focalLength: Float = 0f,
    val apertureScore:Float = 0f,
    val focusDistance: Float = 0f,
    val blurScore:Double = 0.0
)

data class BlurResult(val isBlurry: Boolean,
    val blurScore:Double)


data  class LuminosityResult(
    val brightnessScore:Double,
    val lightCondition:LightCondition,
    val timestamp:Long = System.currentTimeMillis()
)


enum class LightCondition{
    LOW, NORMAL, BRIGHT
}





