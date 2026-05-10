package com.example.palmfingerdetection.util


import android.Manifest
import android.os.Build

object PermissionHelper {

    fun cameraPermissions(): Array<String> = arrayOf(Manifest.permission.CAMERA)

    fun storagePermissions(): Array<String>{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else{
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    fun allRequiredPermissions():Array<String> {
        return cameraPermissions() + storagePermissions()
    }
}


