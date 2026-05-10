package com.example.palmfingerdetection.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.palmfingerdetection.data.model.LightCondition
import com.example.palmfingerdetection.data.model.LuminosityResult


class LuminosityAnalyzer (
    private val onLuminosityChanged: (LuminosityResult) -> Unit
) : ImageAnalysis.Analyzer{
    override fun analyze(image: ImageProxy) {
        // The image is in YUV format. planes[0] is the Y (luminance) plane.
        // Each byte represents the brightness of one pixel (0 = black, 255 = white).
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        // Calculate average brightness across all pixels
        // (toInt() and 0xFF converts signed byte to unsigned 0-255)
        val averageLuminance = data.map { (it.toInt() and 0xFF).toDouble() }.average()

        // Classify the lighting condition
        val condition = when {
            averageLuminance < 50.0 -> LightCondition.LOW
            averageLuminance > 200.0 -> LightCondition.BRIGHT
            else -> LightCondition.NORMAL
        }

        // Notify the ViewModel/Fragment about the current lighting
        onLuminosityChanged(
            LuminosityResult(
                brightnessScore = averageLuminance,
                lightCondition = condition
            )
        )
        image.close()
    }
}

