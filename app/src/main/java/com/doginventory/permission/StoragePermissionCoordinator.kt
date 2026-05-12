package com.doginventory.permission

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class StoragePermissionCoordinator(
    private val activity: ComponentActivity,
    caller: ActivityResultCaller
) {
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val launcher: ActivityResultLauncher<String> = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult?.invoke(granted)
        onPermissionResult = null
    }

    fun isRequired(): Boolean = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    fun isGranted(): Boolean {
        if (!isRequired()) return true
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun request(onResult: (Boolean) -> Unit) {
        if (!isRequired()) {
            onResult(true)
            return
        }
        if (isGranted()) {
            onResult(true)
            return
        }
        onPermissionResult = onResult
        launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
