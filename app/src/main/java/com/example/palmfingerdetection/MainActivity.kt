package com.example.palmfingerdetection

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.palmfingerdetection.databinding.ActivityMainBinding
import com.example.palmfingerdetection.ui.base.BaseActivity
import com.example.palmfingerdetection.ui.palm.PalmDetectionFragment
import com.example.palmfingerdetection.util.PermissionHelper

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request all permissions on app launch
        checkAndRequestPermissions()
    }
    private fun checkAndRequestPermissions() {
        val permissions = PermissionHelper.allRequiredPermissions()

        if (hasAllPermissions(permissions)) {
            // All permissions already granted — go to palm detection
            startPalmDetection()
        } else {
            // Request permissions
            requestPermissions(permissions) { results ->
                val allGranted = results.values.all { it }
                if (allGranted) {
                    startPalmDetection()
                } else {
                    Toast.makeText(
                        this,
                        "Camera and storage permissions are required for this app.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Optionally: show a button to open app settings
                }
            }
        }
    }

    private fun startPalmDetection() {
        loadFragment(
            containerId = R.id.fragmentContainer,
            fragment = PalmDetectionFragment(),
            addToBackStack = false   // Don't add the first screen to back stack
        )
    }
}
