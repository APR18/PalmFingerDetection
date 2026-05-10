package com.example.palmfingerdetection.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.palmfingerdetection.data.model.CaptureMetadata
import com.example.palmfingerdetection.data.model.Handedness
import com.example.palmfingerdetection.util.FileUtils
import java.io.File
class ResultViewModel: ViewModel() {

    private val _metadata = MutableLiveData<CaptureMetadata>()
    val metadata: LiveData<CaptureMetadata> = _metadata

    private val _savedImages = MutableLiveData<List<File>>()
    val savedImages: LiveData<List<File>> = _savedImages

    private var handedness: Handedness = Handedness.UNKNOWN

    /**
     * Set the capture metadata received from the previous screen.
     */
    fun setMetadata(meta: CaptureMetadata, hand: Handedness) {
        _metadata.value = meta
        handedness = hand
    }

    /**
     * Scan the "Finger Data" folder and load all saved images.
     */
    fun loadSavedImages() {
        val folder = FileUtils.getFingerDataFolder()
        if (folder.exists()) {
            val images = folder.listFiles { file ->
                file.isFile && (file.extension == "png" || file.extension == "jpg")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            _savedImages.value = images
        } else {
            _savedImages.value = emptyList()
        }
    }

    fun getHandedness(): Handedness = handedness
}