package com.example.palmfingerdetection.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.camera.core.*
import java.io.File
import androidx.lifecycle.LifecycleOwner
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.palmfingerdetection.data.model.LightCondition
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraManager(

    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    companion object{
        private const val TAG = "CameraManager"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    private val analysisExecutor = Executors.newSingleThreadExecutor()


    fun startCamera(lensFacing: Int = CameraSelector.LENS_FACING_BACK,
                    analyzer:ImageAnalysis.Analyzer? = null){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also{
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val imageAnalysis = analyzer?.let {a-> ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also{it.setAnalyzer(analysisExecutor,a)}
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            try{
                cameraProvider?.unbindAll()
                val useCases = mutableListOf<UseCase>(preview, imageCapture!!)
                imageAnalysis?.let{useCases.add(it)}

                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,*useCases.toTypedArray()
                )

                setupAutoFocus()
            }catch(e: Exception){
                Log.e(TAG,"Camera binding failed", e)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupAutoFocus( ) {
        val factory = previewView.meteringPointFactory
        val centerPoint = factory.createPoint(
            previewView.width / 2f,
            previewView.height / 2f)
        val action = FocusMeteringAction.Builder(centerPoint)
            .setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun focusOnPoint(x: Float, y: Float) {
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun captureImage(outputFile: File, onResult: (Boolean, String?) -> Unit) {
        val capture = imageCapture ?: run {
            onResult(false, "Camera not initialized")
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onResult(true, outputFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed", exception)
                    onResult(false, exception.message)
                }
            }
        )
    }
    fun adjustExposure(condition: LightCondition) {
        val exposureState = camera?.cameraInfo?.exposureState ?: return
        val range = exposureState.exposureCompensationRange

        val targetEV = when (condition) {
            LightCondition.LOW -> range.upper.coerceAtMost(3)    // Brighten
            LightCondition.BRIGHT -> range.lower.coerceAtLeast(-3) // Darken
            LightCondition.NORMAL -> 0                              // Auto
        }

        camera?.cameraControl?.setExposureCompensationIndex(targetEV)
    }

    fun getCameraMetadata(): Map<String, Any> {
        val cameraInfo = camera?.cameraInfo
        return mapOf(
            "cameraType" to "rear",  // Update based on lensFacing
            "hasFlashUnit" to (cameraInfo?.hasFlashUnit() ?: false),
            "exposureState" to (cameraInfo?.exposureState?.exposureCompensationIndex ?: 0)
        )
    }
    fun shutdown() {
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }

}