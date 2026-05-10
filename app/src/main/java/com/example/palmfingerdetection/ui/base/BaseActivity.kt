package com.example.palmfingerdetection.ui.base

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


abstract class BaseActivity : AppCompatActivity() {

    private var permissionCallback: ((Map<String, Boolean>) -> Unit)? = null
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants :Map<String,Boolean> ->
            permissionCallback?.invoke(grants)
        }

    fun requestPermissions(
        permissions: Array<String>,
        onResult: (Map<String, Boolean>) -> Unit
    ) {
        permissionCallback = onResult
        permissionLauncher.launch(permissions)
    }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED


    fun hasAllPermissions(permissions:Array<String>):Boolean{
        return permissions.all{hasPermission(it)}
    }
    // Fragment transaction helpers
    fun loadFragment(
        containerId: Int,
        fragment: Fragment,
        addToBackStack: Boolean = true
    ) {
        supportFragmentManager.beginTransaction().apply {
            replace(containerId, fragment)
            if (addToBackStack) addToBackStack(fragment::class.simpleName)
            commit()
        }
    }
}