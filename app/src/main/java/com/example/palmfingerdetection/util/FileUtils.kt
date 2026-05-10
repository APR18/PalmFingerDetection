package com.example.palmfingerdetection.util
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.palmfingerdetection.data.model.Handedness
object FileUtils {

    private const val FOLDER_NAME = "Finger Data"

    /**
     * Get (or create) the "Finger Data" folder in the public Pictures directory.
     *
     * Path: /storage/emulated/0/Pictures/Finger Data/
     */
    fun getFingerDataFolder(): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val folder = File(picturesDir, FOLDER_NAME)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    /**
     * Create a file in the Finger Data folder with the given name.
     */
    fun createFileInFingerDataFolder(fileName: String): File {
        return File(getFingerDataFolder(), fileName)
    }

    /**
     * Generate the file name for a palm image.
     *
     * Format: "Left_Hand_20240115_143022.png" or "Right_Hand_20240115_143022.jpg"
     */
    fun palmFileName(handedness: Handedness): String {
        val hand = when (handedness) {
            Handedness.LEFT -> "Left"
            Handedness.RIGHT -> "Right"
            Handedness.UNKNOWN -> "Unknown"
        }
        val timestamp = currentTimestamp()
        return "${hand}_Hand_$timestamp.png"
    }

    /**
     * Generate the file name for a finger image.
     *
     * Format: "Left_Hand_Thumb_Finger_20240115_143022.jpg"
     *
     * @param fingerName One of: "Thumb", "Index", "Middle", "Ring", "Little"
     */
    fun fingerFileName(handedness: Handedness, fingerName: String): String {
        val hand = when (handedness) {
            Handedness.LEFT -> "Left"
            Handedness.RIGHT -> "Right"
            Handedness.UNKNOWN -> "Unknown"
        }
        val timestamp = currentTimestamp()
        return "${hand}_Hand_${fingerName}_Finger_$timestamp.jpg"
    }

    /**
     * Generate a timestamp string like "20240115_143022"
     */
    private fun currentTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
}