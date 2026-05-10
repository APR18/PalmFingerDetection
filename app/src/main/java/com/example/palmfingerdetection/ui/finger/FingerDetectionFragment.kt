package com.example.palmfingerdetection.ui.finger

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.palmfingerdetection.camera.BlurDetector

import com.example.palmfingerdetection.camera.CameraManager
import com.example.palmfingerdetection.data.model.*
import com.example.palmfingerdetection.databinding.FragmentFingerDetectionBinding
import com.example.palmfingerdetection.detection.HandDetector
import com.example.palmfingerdetection.detection.MinutiaeExtractor
import com.example.palmfingerdetection.ui.base.BaseActivity
import com.example.palmfingerdetection.ui.base.BaseFragment
import com.example.palmfingerdetection.ui.result.ResultFragment
import com.example.palmfingerdetection.util.FileUtils
import com.yourname.palmfingerdetection.camera.CameraFrameAnalyzer

class FingerDetectionFragment : BaseFragment() {

    private var _binding: FragmentFingerDetectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FingerDetectionViewModel by viewModels()

    private lateinit var cameraManager: CameraManager
    private lateinit var handDetector: HandDetector
    private val minutiaeExtractor = MinutiaeExtractor()

    private val handler = Handler(Looper.getMainLooper())

    private var palmRecords: List<MinutiaeRecord> = emptyList()
    private var expectedHandedness: Handedness = Handedness.UNKNOWN
    private var captureMetadata: CaptureMetadata? = null

    // Track which fingers are done
    private val fingersDone = BooleanArray(5) { false }

    // Finger tracker TextViews (set in onViewCreated)
    private lateinit var fingerTrackers: List<TextView>

    companion object {
        private const val ARG_HANDEDNESS = "handedness"
        private const val ARG_BRIGHTNESS = "brightness"
        private const val ARG_DEVICE_ID = "device_id"
        private const val ARG_BLUR_SCORE = "blur_score"

        fun newInstance(
            palmRecords: List<MinutiaeRecord>,
            handedness: Handedness,
            metadata: CaptureMetadata?
        ): FingerDetectionFragment {
            return FingerDetectionFragment().apply {
                this.palmRecords = palmRecords
                this.expectedHandedness = handedness
                this.captureMetadata = metadata
                arguments = Bundle().apply {
                    putString(ARG_HANDEDNESS, handedness.name)
                    putDouble(ARG_BRIGHTNESS, metadata?.brightnessScore ?: 0.0)
                    putString(ARG_DEVICE_ID, metadata?.deviceId ?: "unknown")
                    putDouble(ARG_BLUR_SCORE, metadata?.blurScore ?: 0.0)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFingerDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { args ->
            val handName = args.getString(ARG_HANDEDNESS, "UNKNOWN")
            expectedHandedness = Handedness.valueOf(handName)
        }

        viewModel.setPalmData(palmRecords, expectedHandedness)

        // Set up finger tracker references
        fingerTrackers = listOf(
            binding.thumbStatus,
            binding.indexStatus,
            binding.middleStatus,
            binding.ringStatus,
            binding.littleStatus
        )

        handDetector = HandDetector(requireContext())
        handDetector.initialize()

        setupCamera()
        observeViewModel()
        setupCaptureButton()
        updateUIForCurrentFinger()
    }

    // ─── Camera Setup ───

    private fun setupCamera() {
        cameraManager = CameraManager(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            previewView = binding.previewView
        )

        val frameAnalyzer = CameraFrameAnalyzer(
            handDetector = handDetector,
            onLuminosityChanged = { result ->
                handler.post { cameraManager.adjustExposure(result.lightCondition) }
            },
            onHandDetected = { result ->
                handler.post { updateLiveDetectionUI(result) }
            }
        )

        cameraManager.startCamera(analyzer = frameAnalyzer)
    }

    // ─── Live Detection UI (updates every frame) ───

    private fun updateLiveDetectionUI(result: HandResult) {
        if (result.isDetected) {
            binding.detectionStatusText.text = "✅ Finger Detected"
            binding.detectionStatusText.setTextColor(Color.parseColor("#4CAF50"))

            if (result.isPalmSide) {
                binding.sideCheckText.text = "Side: Palm ✅ (correct)"
                binding.sideCheckText.setTextColor(Color.parseColor("#4CAF50"))
                binding.errorText.visibility = View.GONE
            } else {
                binding.sideCheckText.text = "Side: Dorsal ❌ (flip your hand)"
                binding.sideCheckText.setTextColor(Color.parseColor("#FF0000"))
                binding.errorText.text = "Finger dorsal side detected, please show palm side finger which contains finger record or minutiae points"
                binding.errorText.visibility = View.VISIBLE
            }
        } else {
            binding.detectionStatusText.text = "👆 Waiting for finger..."
            binding.detectionStatusText.setTextColor(Color.WHITE)
            binding.sideCheckText.text = "Side: --"
            binding.sideCheckText.setTextColor(Color.WHITE)
        }
    }

    // ─── ViewModel Observers ───

    private fun observeViewModel() {
        viewModel.currentFingerIndex.observe(viewLifecycleOwner) { index ->
            updateUIForCurrentFinger()
        }

        viewModel.matchResult.observe(viewLifecycleOwner) { result ->
            if (result.isMatch && result.matchedFingerId != null) {
                val fingerName = result.matchedFingerId
                    .replace("Left_Hand_", "")
                    .replace("Right_Hand_", "")
                    .replace("_", " ")

                // Show match result
                binding.matchResultText.text = "✅ Matched: $fingerName (${(result.confidence * 100).toInt()}%)"
                binding.matchResultText.visibility = View.VISIBLE
                handler.postDelayed({
                    binding.matchResultText.visibility = View.GONE
                }, 2000)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            binding.errorText.text = "❌ $message"
            binding.errorText.visibility = View.VISIBLE
            handler.postDelayed({
                binding.errorText.visibility = View.GONE
            }, 3000)
        }

        viewModel.allFingersCaptured.observe(viewLifecycleOwner) { done ->
            if (done) {
                binding.instructionText.text = "🎉 All fingers captured!"
                binding.instructionText.setTextColor(Color.parseColor("#4CAF50"))
                handler.postDelayed({ navigateToResult() }, 1500)
            }
        }
    }

    // ─── UI Updates ───

    private fun updateUIForCurrentFinger() {
        val index = viewModel.currentFingerIndex.value ?: 0
        val fingerName = viewModel.getCurrentFingerName()
        val hand = if (expectedHandedness == Handedness.LEFT) "Left" else "Right"

        // Update progress
        binding.scanProgressText.text = "Scanning: $index/5"

        // Update current finger name
        val fingerEmojis = listOf("👍", "☝️", "🖕", "💍", "🤙")
        val emoji = fingerEmojis.getOrElse(index) { "👆" }
        binding.currentFingerText.text = "$emoji Next: $hand $fingerName Finger"

        // Update instruction
        binding.instructionText.text = "Place your $fingerName finger in the oval and press capture"
        binding.instructionText.setTextColor(Color.WHITE)

        // Update finger overlay
        binding.fingerOverlay.apply {
            scannedCount = index
            currentFingerName = "$hand $fingerName Finger"
        }

        // Update tracker icons
        updateFingerTrackers(index)
    }

    private fun updateFingerTrackers(currentIndex: Int) {
        val fingerNames = listOf("Thumb", "Index", "Middle", "Ring", "Little")
        val fingerEmojis = listOf("👍", "☝️", "🖕", "💍", "🤙")

        for (i in fingerTrackers.indices) {
            val status = when {
                fingersDone[i] -> "✅"          // Done
                i == currentIndex -> "🔵"       // Current
                else -> "⬜"                     // Pending
            }
            fingerTrackers[i].text = "${fingerEmojis[i]}\n${fingerNames[i]}\n$status"

            // Highlight current finger
            if (i == currentIndex && !fingersDone[i]) {
                fingerTrackers[i].setTextColor(Color.parseColor("#FFC107"))
            } else if (fingersDone[i]) {
                fingerTrackers[i].setTextColor(Color.parseColor("#4CAF50"))
            } else {
                fingerTrackers[i].setTextColor(Color.WHITE)
            }
        }
    }

    // ─── Capture ───

    private fun setupCaptureButton() {
        binding.captureButton.setOnClickListener {
            captureAndProcessFinger()
        }
    }

    private fun captureAndProcessFinger() {
        val bitmap = binding.previewView.bitmap
        if (bitmap == null) {
            showError("Camera not ready. Try again.")
            return
        }

        val handResult = handDetector.detect(bitmap)

        if (!handResult.isDetected) {
            showError("No finger detected. Position your finger in the oval.")
            return
        }

        if (!handResult.isPalmSide) {
            viewModel.onDorsalDetected()
            return
        }

        val blurResult = BlurDetector.detect(bitmap)
        if (blurResult.isBlurry) {
            showError("Image is blurry. Hold steady and recapture.")
            return
        }

        val currentIndex = viewModel.currentFingerIndex.value ?: 0
        val rawLandmarks = handDetector.getLastRawLandmarks()

        if (rawLandmarks.isEmpty()) {
            showError("Could not extract finger data. Try again.")
            return
        }

        val capturedRecord = minutiaeExtractor.extractFromFinger(
            bitmap = bitmap,
            landmarks = rawLandmarks,
            handedness = handResult.handedness,
            fingerIndex = currentIndex
        )

        viewModel.validateFinger(capturedRecord, handResult.handedness)

        val newIndex = viewModel.currentFingerIndex.value ?: 0
        val allDone = viewModel.allFingersCaptured.value ?: false

        if (newIndex > currentIndex || allDone) {
            fingersDone[currentIndex] = true
            updateFingerTrackers(newIndex)
            saveFingerImage(bitmap, currentIndex)

            // Show success feedback
            binding.matchResultText.text = "✅ ${getFingerName(currentIndex)} captured!"
            binding.matchResultText.visibility = View.VISIBLE
            handler.postDelayed({
                binding.matchResultText.visibility = View.GONE
            }, 2000)
        }
    }

    private fun getFingerName(index: Int): String {
        return listOf("Thumb", "Index", "Middle", "Ring", "Little").getOrElse(index) { "Unknown" }
    }

    private fun showError(message: String) {
        binding.errorText.text = "❌ $message"
        binding.errorText.visibility = View.VISIBLE
        handler.postDelayed({
            binding.errorText.visibility = View.GONE
        }, 3000)
    }

    // ─── File Saving ───

    private fun saveFingerImage(bitmap: Bitmap, fingerIndex: Int) {
        val fingerName = getFingerName(fingerIndex)
        val fileName = FileUtils.fingerFileName(expectedHandedness, fingerName)
        val outputFile = FileUtils.createFileInFingerDataFolder(fileName)

        try {
            outputFile.outputStream().use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
            }
        } catch (e: Exception) {
            showError("Failed to save: ${e.message}")
        }
    }

    // ─── Navigation ───

    private fun navigateToResult() {
        val resultFragment = ResultFragment.newInstance(
            metadata = captureMetadata,
            handedness = expectedHandedness
        )

        (requireActivity() as BaseActivity).loadFragment(
            containerId = com.example.palmfingerdetection.R.id.fragmentContainer,
            fragment = resultFragment,
            addToBackStack = true
        )
    }

    // ─── Cleanup ───

    override fun onDestroyView() {
        super.onDestroyView()
        handDetector.release()
        cameraManager.shutdown()
        _binding = null
    }
}