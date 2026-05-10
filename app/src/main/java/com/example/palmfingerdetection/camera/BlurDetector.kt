package com.example.palmfingerdetection.camera

import android.graphics.Bitmap
import com.example.palmfingerdetection.data.model.BlurResult

object BlurDetector {

    /**
     * @param bitmap    The image to check.
     * @param threshold Below this variance value, the image is considered blurry.
     * @return BlurResult with isBlurry flag and the numeric blurScore.
     */
    fun detect(bitmap: Bitmap, threshold: Double = 100.0): BlurResult {
        val width = bitmap.width
        val height = bitmap.height

        // Step 1: Get all pixel values
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Step 2: Convert to grayscale
        // Each pixel is an ARGB int. Extract R, G, B and compute luminance.
        val gray = DoubleArray(width * height) { i ->
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF  // Red channel
            val g = (pixel shr 8) and 0xFF   // Green channel
            val b = pixel and 0xFF           // Blue channel
            // Standard luminance formula (matches human perception)
            0.299 * r + 0.587 * g + 0.114 * b
        }

        // Step 3: Apply Laplacian kernel and collect values
        var sum = 0.0
        var sumOfSquares = 0.0
        var count = 0

        // Skip the border pixels (Laplacian needs 1-pixel neighbours)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x

                // Laplacian = -4 * center + top + bottom + left + right
                val laplacian = -4.0 * gray[idx] +
                        gray[idx - 1] +            // left
                        gray[idx + 1] +            // right
                        gray[idx - width] +        // top
                        gray[idx + width]          // bottom

                sum += laplacian
                sumOfSquares += laplacian * laplacian
                count++
            }
        }

        // Step 4: Calculate variance
        // Variance = E[X²] - (E[X])²
        val mean = sum / count
        val variance = (sumOfSquares / count) - (mean * mean)

        return BlurResult(
            isBlurry = variance < threshold,
            blurScore = variance
        )
    }
}