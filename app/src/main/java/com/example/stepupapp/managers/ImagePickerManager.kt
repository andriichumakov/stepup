package com.example.stepupapp.managers

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.stepupapp.ImageBase64Utils
import com.example.stepupapp.UserPreferences
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImagePickerManager(
    private val activity: AppCompatActivity
) {
    
    interface ImagePickerCallback {
        fun onImageSelected(bitmap: Bitmap)
        fun onImageError(message: String)
    }
    
    private var currentPhotoPath: String? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var imageView: ImageView? = null
    
    // This must be called during onCreate() before activity starts
    fun registerActivityLaunchers() {
        initializeActivityLaunchers()
    }
    
    // This can be called later to set up UI interactions
    fun setupImageView(imageView: ImageView) {
        this.imageView = imageView
    }
    
    fun showImagePickerDialog(callback: ImagePickerCallback? = null) {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        
        AlertDialog.Builder(activity)
            .setTitle("Select Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkPermissionsAndTakePhoto(callback)
                    1 -> checkPermissionsAndChooseFromGallery(callback)
                }
            }
            .show()
    }
    
    private fun initializeActivityLaunchers() {
        cameraLauncher = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            handleCameraResult(success)
        }
        
        galleryLauncher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            handleGalleryResult(uri)
        }
        
        permissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionResult(permissions)
        }
    }
    
    private fun handleCameraResult(success: Boolean) {
        if (success) {
            currentPhotoPath?.let { path ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, Uri.fromFile(File(path)))
                    imageView?.setImageBitmap(bitmap)
                    saveProfileImageToPreferences(bitmap)
                } catch (e: Exception) {
                    showError("Error processing camera image")
                }
            }
        } else {
            cleanupCameraFile()
        }
    }
    
    private fun handleGalleryResult(uri: Uri?) {
        uri?.let {
            try {
                val bitmap = ImageBase64Utils.uriToBitmap(activity, it)
                if (bitmap != null) {
                    imageView?.setImageBitmap(bitmap)
                    saveProfileImageToPreferences(bitmap)
                } else {
                    showError("Error loading image")
                }
            } catch (e: Exception) {
                showError("Error loading image: ${e.message}")
            }
        }
    }
    
    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val cameraPermissionGranted = permissions[Manifest.permission.CAMERA] ?: false
        
        if (cameraPermissionGranted) {
            takePhoto()
        } else {
            showError("Camera permission denied. You can still choose from gallery.")
            openGallery()
        }
    }
    
    private fun checkPermissionsAndTakePhoto(callback: ImagePickerCallback? = null) {
        if (hasCameraPermission()) {
            takePhoto()
        } else {
            requestCameraPermission()
        }
    }
    
    private fun checkPermissionsAndChooseFromGallery(callback: ImagePickerCallback? = null) {
        openGallery()
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }
    
    private fun takePhoto() {
        val photoFile = createImageFile()
        photoFile?.let { file ->
            currentPhotoPath = file.absolutePath
            val photoURI = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.provider",
                file
            )
            cameraLauncher.launch(photoURI)
        }
    }
    
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
    
    private fun createImageFile(): File? {
        return try {
            File(activity.filesDir, "profile_camera_${System.currentTimeMillis()}.jpg")
        } catch (e: Exception) {
            showError("Error creating image file")
            null
        }
    }
    
    private fun saveProfileImageToPreferences(bitmap: Bitmap) {
        try {
            val base64Image = ImageBase64Utils.bitmapToBase64(bitmap)
            if (base64Image != null) {
                saveImageLocally(bitmap, base64Image)
                syncImageToServer(base64Image)
            } else {
                showError("Error converting image")
            }
        } catch (e: Exception) {
            showError("Error saving profile picture")
        }
    }
    
    private fun saveImageLocally(bitmap: Bitmap, base64Image: String) {
        UserPreferences.saveProfileImageBase64(activity, base64Image)
        
        val imageFile = File(activity.filesDir, "profile_image.jpg")
        val outputStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.close()
        UserPreferences.saveProfileImagePath(activity, imageFile.absolutePath)
    }
    
    private fun syncImageToServer(base64Image: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = ProfileService.updateProfilePictureBase64(base64Image)
                withContext(Dispatchers.Main) {
                    UserPreferences.markProfileImageNeedingSync(activity, !success)
                    val message = if (success) {
                        "Profile picture saved"
                    } else {
                        "Profile picture saved locally. Will sync when online."
                    }
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.w("ImagePickerManager", "Failed to sync profile picture: ${e.message}")
                UserPreferences.markProfileImageNeedingSync(activity, true)
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Profile picture saved locally. Will sync when online.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun cleanupCameraFile() {
        currentPhotoPath?.let { path ->
            File(path).delete()
        }
        currentPhotoPath = null
    }
    
    private fun showError(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
} 