package com.yourname.palmfingerdetection.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.palmfingerdetection.data.model.LightCondition
import com.example.palmfingerdetection.data.model.LuminosityResult
import com.example.palmfingerdetection.data.model.HandResult
import com.example.palmfingerdetection.detection.HandDetector
import java.io.ByteArrayOutputStream

/**
 * Combines luminosity analysis AND hand detection in a single analyzer.
 * Runs on every camera frame.
 *
 * Why combine them?
 * CameraX only allows ONE ImageAnalysis use case per camera session.
 * So we do both jobs in the same analyze() call.
 *
 * Hand detection is heavier than luminosity, so we throttle it
 * to run every 5th frame (~6 times per second at 30fps).
 */
class CameraFrameAnalyzer(
    private val handDetector: HandDetector,
    private val onLuminosityChanged: (LuminosityResult) -> Unit,
    private val onHandDetected: (HandResult) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameCount = 0
    private val DETECT_EVERY_N_FRAMES = 5  // Run hand detection every 5th frame

    override fun analyze(image: ImageProxy) {
        try {
            // ... luminosity code stays the same ...

            frameCount++
            if (frameCount % DETECT_EVERY_N_FRAMES == 0) {
                Log.d("FrameAnalyzer", "📷 Frame #$frameCount — converting to bitmap...")

                val bitmap = imageToBitmap(image)
                if (bitmap != null) {
                    Log.d("FrameAnalyzer", "📷 Bitmap created: ${bitmap.width}x${bitmap.height}")
                    val result = handDetector.detect(bitmap)
                    Log.d("FrameAnalyzer", "📷 Detection result: detected=${result.isDetected}")
                    onHandDetected(result)
                    bitmap.recycle()
                } else {
                    Log.e("FrameAnalyzer", "❌ imageToBitmap returned null! Format=${image.format}")
                }
            }
        } catch (e: Exception) {
            Log.e("FrameAnalyzer", "❌ Analyzer crashed: ${e.message}", e)
        } finally {
            image.close()
        }
    }

    /**
     * Convert an ImageProxy (YUV format) to a Bitmap.
     *
     * CameraX gives us frames in YUV_420_888 format.
     * We need to convert to Bitmap for MediaPipe.
     */
    private fun imageToBitmap(image: ImageProxy): Bitmap? {
        if (image.format != ImageFormat.YUV_420_888) return null

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        // NV21 format: Y plane followed by interleaved VU
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, image.width, image.height),
            80,  // Quality (lower = faster, 80 is fine for detection)
            out
        )

        val bytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}