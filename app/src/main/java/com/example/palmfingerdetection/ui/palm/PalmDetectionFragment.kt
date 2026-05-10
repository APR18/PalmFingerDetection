package com.example.palmfingerdetection.ui.palm

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.palmfingerdetection.R
import com.example.palmfingerdetection.databinding.FragmentPalmDetectionBinding
import com.example.palmfingerdetection.ui.base.BaseFragment
import com.example.palmfingerdetection.camera.CameraManager
import com.example.palmfingerdetection.camera.LuminosityAnalyzer
import com.example.palmfingerdetection.data.model.*
import com.example.palmfingerdetection.detection.HandDetector
import com.example.palmfingerdetection.detection.MinutiaeExtractor
import com.example.palmfingerdetection.util.DeviceUtils
import com.example.palmfingerdetection.util.FileUtils
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.viewModels
import com.example.palmfingerdetection.ui.base.BaseActivity
import com.example.palmfingerdetection.ui.finger.FingerDetectionFragment
import com.yourname.palmfingerdetection.camera.CameraFrameAnalyzer

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PalmDetectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PalmDetectionFragment : BaseFragment() {
    private var _binding: FragmentPalmDetectionBinding? = null
    private val binding get() = _binding!!

    // ViewModel — survives rotation
    private val viewModel: PalmDetectionViewModel by viewModels()

    // Camera and detection components
    private lateinit var cameraManager: CameraManager
    private lateinit var handDetector: HandDetector
    private val minutiaeExtractor = MinutiaeExtractor()

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPalmDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("PALM_DEBUG", "🚀 PalmDetectionFragment onViewCreated called!")
        // Initialize the hand detection ML model
        handDetector = HandDetector(requireContext())
        handDetector.initialize()
        Log.d("PALM_DEBUG", "🚀 HandDetector initialized, setting up camera...")
        // Set up the camera
        setupCamera()

        // Set up UI observers (connect ViewModel → UI)
        observeViewModel()

        // Set up button click
        binding.captureButton.setOnClickListener {
            captureAndProcessPalm()
        }
    }

    private fun setupCamera() {
        cameraManager = CameraManager(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            previewView = binding.previewView
        )

        // Combined analyzer: luminosity + hand detection on every frame
        val frameAnalyzer = CameraFrameAnalyzer(
            handDetector = handDetector,
            onLuminosityChanged = { result ->
                handler.post { viewModel.onLuminosityChanged(result) }
            },
            onHandDetected = { result ->
                handler.post { viewModel.onHandDetected(result) }
            }
        )

        cameraManager.startCamera(analyzer = frameAnalyzer)
    }

    private fun observeViewModel() {
        // Update light indicator text
        viewModel.luminosity.observe(viewLifecycleOwner) { result ->
            binding.lightIndicator.text = "Light: ${result.lightCondition.name}"

            // Auto-adjust camera exposure
            cameraManager.adjustExposure(result.lightCondition)
        }

        // Update hand info
        viewModel.handResult.observe(viewLifecycleOwner) { result ->
            if (result.isDetected) {
                val hand = when (result.handedness) {
                    Handedness.LEFT -> "Left"
                    Handedness.RIGHT -> "Right"
                    Handedness.UNKNOWN -> "Unknown"
                }
                binding.handInfoText.text = "$hand hand | Fingers: ${result.fingerCount}"
                binding.palmOverlay.statusText = "Palm detected — tap to capture"
            } else {
                binding.handInfoText.text = "No hand detected"
                binding.palmOverlay.statusText = "Place your palm in the oval"
            }
        }

        // Show error messages on the overlay
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            binding.palmOverlay.errorText = message
            // Auto-clear error after 3 seconds
            handler.postDelayed({ binding.palmOverlay.errorText = "" }, 3000)
        }

        // Navigate to finger detection when palm is captured
        viewModel.palmCaptured.observe(viewLifecycleOwner) { captured ->
            if (captured) {
                navigateToFingerDetection()
            }
        }
    }
    private fun captureAndProcessPalm() {
        val currentHand = viewModel.handResult.value

        // Validate: is a hand actually detected?
        if (currentHand == null || !currentHand.isDetected) {
            Toast.makeText(context, "No hand detected. Please show your palm.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate: is it the palm side?
        if (!currentHand.isPalmSide) {
            viewModel.setError("Palm dorsal side detected, minutiae points won't be extracted.")
            return
        }

        // Determine file name and path
        val fileName = FileUtils.palmFileName(currentHand.handedness)
        val outputFile = FileUtils.createFileInFingerDataFolder(fileName)

        // Capture the image
        cameraManager.captureImage(outputFile) { success, pathOrError ->
            if (success) {
                // The image was saved. Now extract minutiae from it.
                // In production, you'd load the saved image. For simplicity,
                // we'll use the bitmap from the preview.
                val bitmap = binding.previewView.bitmap
                if (bitmap != null) {
                    processCapturedPalm(bitmap, currentHand)
                }
            } else {
                Toast.makeText(context, "Capture failed: $pathOrError", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processCapturedPalm(bitmap: Bitmap, handResult: HandResult) {
        // Detect hand in the captured image to get landmarks
        val detection = handDetector.detect(bitmap)

        if (!detection.isDetected) {
            Toast.makeText(context, "Hand not clear in capture. Try again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Extract minutiae records for all 5 fingers
        // (We need MediaPipe NormalizedLandmarks, so re-detect from bitmap)
        // For the simplified version, we pass the detection landmarks
        val records = minutiaeExtractor.extractFromPalm(
            bitmap = bitmap,
            landmarks = handDetector.getLastRawLandmarks(),
            handedness = detection.handedness
        )

        // Gather camera metadata
        val luminosity = viewModel.luminosity.value
        val metadata = CaptureMetadata(
            deviceId = DeviceUtils.getDeviceId(requireContext()),
            brightnessScore = luminosity?.brightnessScore ?: 0.0,
            cameraType = "rear",
            blurScore = com.example.palmfingerdetection.camera.BlurDetector.detect(bitmap).blurScore
        )

        // Tell the ViewModel the palm is captured
        viewModel.onPalmCaptured(records, detection.handedness, metadata)
    }
    private fun navigateToFingerDetection() {
        val fingerFragment = FingerDetectionFragment.newInstance(
            palmRecords = viewModel.getMinutiaeRecords(),
            handedness = viewModel.getDetectedHandedness(),
            metadata = viewModel.getCaptureMetadata()
        )

        (requireActivity() as BaseActivity).loadFragment(
            containerId = R.id.fragmentContainer,
            fragment = fingerFragment,
            addToBackStack = true
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handDetector.release()
        cameraManager.shutdown()
        _binding = null   // Prevent memory leaks
    }
}