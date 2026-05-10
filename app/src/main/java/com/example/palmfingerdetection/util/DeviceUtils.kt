package com.example.palmfingerdetection.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

/**
 * Utility for device-specific information.
 */
object DeviceUtils {

    /**
     * Get a unique device ID.
     *
     * ANDROID_ID is a 64-bit hex string, unique per device + user + app signing key.
     * It's reset on factory reset. It's NOT the IMEI (which requires phone permission).
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
}