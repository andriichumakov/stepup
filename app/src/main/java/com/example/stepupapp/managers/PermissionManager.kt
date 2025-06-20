package com.example.stepupapp.managers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: AppCompatActivity) {
    
    interface PermissionCallback {
        fun onPermissionsGranted()
        fun onPermissionsDenied()
        fun onPermissionsError(error: String)
    }
    
    private var callback: PermissionCallback? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    
    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    fun initialize() {
        requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(permissions)
        }
    }
    
    fun checkAndRequestPermissions(callback: PermissionCallback) {
        this.callback = callback
        
        val permissionsToRequest = getPermissionsToRequest()
        
        if (permissionsToRequest.isEmpty()) {
            callback.onPermissionsGranted()
            return
        }
        
        val shouldShowRationale = permissionsToRequest.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        
        if (shouldShowRationale) {
            showPermissionRationaleDialog(permissionsToRequest)
        } else {
            requestPermissions(permissionsToRequest)
        }
    }
    
    private fun getPermissionsToRequest(): Array<String> {
        return REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
    
    private fun showPermissionRationaleDialog(permissionsToRequest: Array<String>) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("This app needs permission to count your steps, show notifications, and access location for weather information. Without these permissions, some features won't work.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissions(permissionsToRequest)
            }
            .setNegativeButton("Cancel") { _, _ ->
                callback?.onPermissionsDenied()
            }
            .create()
            .show()
    }
    
    private fun requestPermissions(permissions: Array<String>) {
        requestPermissionLauncher.launch(permissions)
    }
    
    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        if (permissions.all { it.value }) {
            callback?.onPermissionsGranted()
        } else {
            showSettingsDialog()
        }
    }
    
    private fun showSettingsDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("This app requires permissions to track your activity and access location for weather information. Please grant the permissions in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { _, _ ->
                callback?.onPermissionsDenied()
            }
            .create()
            .show()
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            callback?.onPermissionsError("Unable to open settings")
        }
    }
    
    fun areAllPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }
} 