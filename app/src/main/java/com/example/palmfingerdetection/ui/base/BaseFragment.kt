package com.example.palmfingerdetection.ui.base

import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment(){

    protected fun requestPermissions(
        permissions:Array<String>,
        onResult: (Map<String, Boolean>)-> Unit
    ){
        (requireActivity() as? BaseActivity)?.requestPermissions(permissions,onResult)
    }

    protected fun hasPermission(permission: String): Boolean {
        return (requireActivity() as? BaseActivity)?.hasPermission(permission) == true
    }

    protected fun hasAllPermissions(permissions: Array<String>): Boolean {
        return (requireActivity() as? BaseActivity)?.hasAllPermissions(permissions) == true
    }

}