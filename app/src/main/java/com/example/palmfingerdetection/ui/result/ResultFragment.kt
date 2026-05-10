package com.example.palmfingerdetection.ui.result

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.example.palmfingerdetection.data.model.CaptureMetadata
import com.example.palmfingerdetection.data.model.Handedness
import com.example.palmfingerdetection.databinding.FragmentResultBinding
import java.io.File
import java.text.SimpleDateFormat
import com.example.palmfingerdetection.R
import com.example.palmfingerdetection.ui.base.BaseActivity
import java.util.*
import com.example.palmfingerdetection.ui.base.BaseFragment
import com.example.palmfingerdetection.ui.palm.PalmDetectionFragment


class ResultFragment : BaseFragment() {

    // ─── ViewBinding ───
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ResultViewModel by viewModels()

    companion object {
        private const val ARG_BLUR_SCORE = "blur_score"
        private const val ARG_BRIGHTNESS_SCORE = "brightness_score"
        private const val ARG_FOCUS_DISTANCE = "focus_distance"
        private const val ARG_DEVICE_ID = "device_id"
        private const val ARG_CAMERA_TYPE = "camera_type"
        private const val ARG_HANDEDNESS = "handedness"
        private const val ARG_FOCAL_LENGTH = "focal_length"
        private const val ARG_APERTURE = "aperture"
        /**
         * Factory method. Pass all the metadata values from the capture session.
         */
        fun newInstance(
            metadata: CaptureMetadata?,
            handedness: Handedness
        ): ResultFragment {
            return ResultFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_BLUR_SCORE, metadata?.blurScore ?: 0.0)
                    putDouble(ARG_BRIGHTNESS_SCORE, metadata?.brightnessScore ?: 0.0)
                    putFloat(ARG_FOCUS_DISTANCE, metadata?.focusDistance ?: 0f)
                    putString(ARG_DEVICE_ID, metadata?.deviceId ?: "unknown")
                    putString(ARG_CAMERA_TYPE, metadata?.cameraType ?: "rear")
                    putString(ARG_HANDEDNESS, handedness.name)
                    putFloat(ARG_FOCAL_LENGTH, metadata?.focalLength ?: 0f)
                    putFloat(ARG_APERTURE, metadata?.apertureScore ?: 0f)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Extract metadata from arguments and display
        displayMetadata()

        // Observe saved images
        observeViewModel()

        // Load images from the Finger Data folder
        viewModel.loadSavedImages()

        // Set up the "Start New Scan" button
        binding.doneButton.setOnClickListener {
            startNewScan()
        }
    }


    private fun displayMetadata() {
        val args = arguments ?: return

        // ─── Blur Score ───
        val blurScore = args.getDouble(ARG_BLUR_SCORE, 0.0)
        binding.blurScoreValue.text = String.format("%.2f", blurScore)

        // Classify blur quality
        val blurQuality = when {
            blurScore > 200.0 -> Pair("Very Sharp", "#4CAF50")      // Green
            blurScore > 100.0 -> Pair("Sharp", "#8BC34A")           // Light green
            blurScore > 50.0 -> Pair("Acceptable", "#FFC107")       // Yellow
            else -> Pair("Blurry", "#F44336")                        // Red
        }
        binding.blurQualityText.text = "Image Quality: ${blurQuality.first}"
        binding.blurQualityText.setTextColor(android.graphics.Color.parseColor(blurQuality.second))

        // ─── Brightness Score ───
        val brightnessScore = args.getDouble(ARG_BRIGHTNESS_SCORE, 0.0)
        binding.brightnessScoreValue.text = String.format("%.1f", brightnessScore)

        // Classify lighting condition
        val lightCondition = when {
            brightnessScore < 50.0 -> Pair("Low Light", "#F44336")
            brightnessScore > 200.0 -> Pair("Bright Light", "#FFC107")
            else -> Pair("Normal", "#4CAF50")
        }
        binding.brightnessConditionText.text = "Lighting: ${lightCondition.first}"
        binding.brightnessConditionText.setTextColor(
            android.graphics.Color.parseColor(lightCondition.second)
        )

        // ─── Focus Distance ───
        val focusDistance = args.getFloat(ARG_FOCUS_DISTANCE, 0f)
        binding.focusDistanceValue.text = if (focusDistance > 0f) {
            String.format("%.2f cm", focusDistance * 100)  // Convert meters to cm
        } else {
            "Auto"
        }

        // ─── Device Info ───
        binding.deviceIdValue.text = args.getString(ARG_DEVICE_ID, "unknown")
        binding.cameraTypeValue.text = args.getString(ARG_CAMERA_TYPE, "rear")
            .replaceFirstChar { it.uppercase() }

        val handedness = args.getString(ARG_HANDEDNESS, "UNKNOWN")
        binding.handDetectedValue.text = when (handedness) {
            "LEFT" -> "Left Hand"
            "RIGHT" -> "Right Hand"
            else -> "Unknown"
        }
    }
    private fun observeViewModel() {
        viewModel.savedImages.observe(viewLifecycleOwner) { images ->
            if (images.isNotEmpty()) {
                binding.noImagesText.visibility = View.GONE
                displayThumbnails(images)
            } else {
                binding.noImagesText.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Dynamically creates a row for each saved image showing
     * a thumbnail and the file name.
     */
    private fun displayThumbnails(images: List<File>) {
        val container = binding.thumbnailContainer
        container.removeAllViews()  // Clear any existing thumbnails

        for (imageFile in images) {
            // Create a horizontal layout for each image row
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
                setPadding(12, 12, 12, 12)
                setBackgroundColor(android.graphics.Color.parseColor("#16213E"))
            }

            // ─── Thumbnail ImageView ───
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                    marginEnd = 16
                }
                scaleType = ImageView.ScaleType.CENTER_CROP

                // Load bitmap efficiently (downscaled to save memory)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4  // Load at 1/4 resolution
                }
                try {
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)
                    setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // If image can't be loaded, show a placeholder color
                    setBackgroundColor(android.graphics.Color.GRAY)
                }
            }

            // ─── File Name & Details ───
            val infoLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f  // Take remaining space
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            // File name (make it readable)
            // "Left_Hand_Thumb_Finger_20240115_143022.jpg" → "Left Hand Thumb Finger"
            val displayName = imageFile.nameWithoutExtension
                .replace("_", " ")
                .replace(Regex("\\d{8} \\d{6}"), "")  // Remove timestamp
                .trim()

            val nameText = TextView(requireContext()).apply {
                text = displayName
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
            }

            // File size
            val sizeText = TextView(requireContext()).apply {
                val sizeKB = imageFile.length() / 1024
                text = "${sizeKB}KB • ${imageFile.extension.uppercase()}"
                setTextColor(android.graphics.Color.parseColor("#B0B0B0"))
                textSize = 12f
            }

            infoLayout.addView(nameText)
            infoLayout.addView(sizeText)

            row.addView(imageView)
            row.addView(infoLayout)

            container.addView(row)
        }
    }

    private fun startNewScan() {
        // Clear the entire back stack
        val fm = requireActivity().supportFragmentManager
        for (i in 0 until fm.backStackEntryCount) {
            fm.popBackStack()
        }

        // Load a fresh PalmDetectionFragment
        (requireActivity() as BaseActivity).loadFragment(
            containerId = R.id.fragmentContainer,
            fragment = PalmDetectionFragment(),
            addToBackStack = false
        )
    }
}