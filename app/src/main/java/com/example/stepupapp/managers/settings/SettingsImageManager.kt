package com.example.stepupapp.managers.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.ImageBase64Utils
import com.example.stepupapp.UserPreferences
import com.example.stepupapp.databinding.SettingsPageBinding
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.launch
import java.io.File

interface SettingsImageProvider {
    fun initializeImageLaunchers()
    fun showImageSelectionDialog()
    fun requestCameraPermission()
    fun openGallery()
    fun openCamera()
}

interface SettingsImageCallback {
    fun onImageSelected(success: Boolean, message: String)
    fun onImageProcessing(isProcessing: Boolean)
}

class SettingsImageManager(
    private val activity: ComponentActivity,
    private val binding: SettingsPageBinding,
    private val callback: SettingsImageCallback?
) : SettingsImageProvider {
    
    companion object {
        private const val TAG = "SettingsImageManager"
    }
    
    private var currentImageFile: File? = null
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    
    override fun initializeImageLaunchers() {
        // Gallery launcher
        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleImageSelected(uri)
                }
            }
        }

        // Camera launcher
        cameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                currentImageFile?.let { file ->
                    handleImageSelected(Uri.fromFile(file))
                }
            } else {
                // Clean up the file if camera operation failed
                currentImageFile?.delete()
                currentImageFile = null
                showMessage("Camera operation cancelled")
            }
        }

        // Camera permission launcher
        cameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                openCamera()
            } else {
                showMessage("Camera permission denied. You can still choose from gallery.")
                // Fallback to gallery if camera permission denied
                openGallery()
            }
        }
        
        // Setup click listener for profile picture change
        binding.changeProfilePictureButton.setOnClickListener {
            showImageSelectionDialog()
        }
    }
    
    override fun showImageSelectionDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        
        AlertDialog.Builder(activity)
            .setTitle("Select Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestCameraPermission()
                    1 -> openGallery()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun requestCameraPermission() {
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }
    
    override fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            galleryLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error opening gallery: ${e.message}", e)
            showMessage("Error opening gallery: ${e.message}")
        }
    }
    
    override fun openCamera() {
        try {
            currentImageFile = File(activity.filesDir, "profile_camera_${System.currentTimeMillis()}.jpg")
            val imageUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.provider",
                currentImageFile!!
            )
            cameraLauncher.launch(imageUri)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Camera error: ${e.message}", e)
            showMessage("Camera not available: ${e.message}")
            openGallery() // Fallback to gallery
        }
    }
    
    private fun handleImageSelected(uri: Uri) {
        callback?.onImageProcessing(true)
        
        activity.lifecycleScope.launch {
            try {
                // Convert to base64 using ImageBase64Utils
                val bitmap = ImageBase64Utils.uriToBitmap(activity, uri)
                if (bitmap != null) {
                    val base64String = ImageBase64Utils.bitmapToBase64(bitmap)
                    
                    if (base64String != null) {
                        // Save locally first (guaranteed to succeed)
                        UserPreferences.saveProfileImageBase64(activity, base64String)
                        
                        activity.runOnUiThread {
                            // Update the UI immediately
                            binding.currentProfileImageView.setImageBitmap(bitmap)
                            callback?.onImageProcessing(false)
                            callback?.onImageSelected(true, "Profile picture updated")
                        }
                        
                        // Try to sync to database in background
                        syncImageToServer(base64String)
                        
                    } else {
                        activity.runOnUiThread {
                            callback?.onImageProcessing(false)
                            callback?.onImageSelected(false, "Failed to convert image to base64")
                        }
                    }
                } else {
                    activity.runOnUiThread {
                        callback?.onImageProcessing(false)
                        callback?.onImageSelected(false, "Failed to process image")
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error handling image: ${e.message}", e)
                activity.runOnUiThread {
                    callback?.onImageProcessing(false)
                    callback?.onImageSelected(false, "Error processing image: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun syncImageToServer(base64String: String) {
        try {
            val success = ProfileService.updateProfilePictureBase64(base64String)
            
            activity.runOnUiThread {
                if (!success) {
                    // Mark for later sync but don't show error since local save succeeded
                    UserPreferences.markProfileImageNeedingSync(activity, true)
                    android.util.Log.w(TAG, "Profile picture saved locally, will sync when online")
                } else {
                    // Clear sync flag on successful upload
                    UserPreferences.markProfileImageNeedingSync(activity, false)
                    android.util.Log.d(TAG, "Profile picture successfully synced to server")
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to sync profile picture: ${e.message}")
            activity.runOnUiThread {
                UserPreferences.markProfileImageNeedingSync(activity, true)
            }
        }
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
} 