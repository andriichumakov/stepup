package com.example.stepupapp

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utility class for loading profile pictures consistently across the app
 * Handles loading from database (base64), local cache, or file fallback
 */
object ProfilePictureLoader {
    private const val TAG = "ProfilePictureLoader"
    
    /**
     * Migrate legacy global profile pictures to user-specific storage
     */
    private fun migrateLegacyProfilePictures(context: Context) {
        try {
            val sharedPrefs = context.getSharedPreferences("StepUpPrefs", Context.MODE_PRIVATE)
            
            // Check if there are legacy profile pictures stored globally
            val legacyBase64 = sharedPrefs.getString("profile_image_base64", null)
            val legacyPath = sharedPrefs.getString("profile_image_path", null)
            
            if (!legacyBase64.isNullOrEmpty() || !legacyPath.isNullOrEmpty()) {
                // Only migrate if there's no user-specific data yet
                val currentBase64 = UserPreferences.getProfileImageBase64(context)
                val currentPath = UserPreferences.getProfileImagePath(context)
                
                if (currentBase64.isNullOrEmpty() && currentPath.isNullOrEmpty()) {
                    // Migrate the data
                    if (!legacyBase64.isNullOrEmpty()) {
                        UserPreferences.saveProfileImageBase64(context, legacyBase64)
                        Log.d(TAG, "Migrated legacy base64 profile picture to user-specific storage")
                    }
                    
                    if (!legacyPath.isNullOrEmpty()) {
                        UserPreferences.saveProfileImagePath(context, legacyPath)
                        Log.d(TAG, "Migrated legacy file path profile picture to user-specific storage")
                    }
                }
                
                // Clear legacy data
                sharedPrefs.edit().apply {
                    remove("profile_image_base64")
                    remove("profile_image_path")
                    remove("profile_image_needs_sync")
                }.apply()
                Log.d(TAG, "Cleared legacy profile picture data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during profile picture migration: ${e.message}")
        }
    }

    /**
     * Load profile picture into an ImageView
     * Priority: Database -> Local Base64 -> Local File -> Default
     */
    fun loadProfilePicture(context: Context, imageView: ImageView, showDefault: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Migrate legacy profile pictures if needed
                migrateLegacyProfilePictures(context)
                
                var bitmap: Bitmap? = null
                
                // 1. Try to load from database first
                try {
                    val serverBase64 = ProfileService.getUserProfilePictureBase64()
                    if (serverBase64 != null) {
                        bitmap = ImageBase64Utils.base64ToBitmap(serverBase64)
                        if (bitmap != null) {
                            // Update local cache with server data
                            UserPreferences.saveProfileImageBase64(context, serverBase64)
                            UserPreferences.markProfileImageNeedingSync(context, false)
                            Log.d(TAG, "Loaded profile picture from database")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load from database: ${e.message}")
                }
                
                // 2. Fallback to local base64 cache
                if (bitmap == null) {
                    val localBase64 = UserPreferences.getProfileImageBase64(context)
                    if (localBase64 != null) {
                        bitmap = ImageBase64Utils.base64ToBitmap(localBase64)
                        if (bitmap != null) {
                            Log.d(TAG, "Loaded profile picture from local base64 cache")
                        }
                    }
                }
                
                // 3. Final fallback to file path
                if (bitmap == null) {
                    val imagePath = UserPreferences.getProfileImagePath(context)
                    if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
                        bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.fromFile(File(imagePath)))
                        if (bitmap != null) {
                            Log.d(TAG, "Loaded profile picture from file path")
                        }
                    }
                }
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else if (showDefault) {
                        // Set default image if no profile picture found
                        imageView.setImageResource(R.drawable.outline_image_24)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile picture", e)
                withContext(Dispatchers.Main) {
                    if (showDefault) {
                        imageView.setImageResource(R.drawable.outline_image_24)
                    }
                }
            }
        }
    }
    
    /**
     * Get profile picture as bitmap (async)
     * Returns null if no profile picture is available
     */
    suspend fun getProfilePictureBitmap(context: Context): Bitmap? {
        return try {
            // Try database first
            val serverBase64 = ProfileService.getUserProfilePictureBase64()
            if (serverBase64 != null) {
                val bitmap = ImageBase64Utils.base64ToBitmap(serverBase64)
                if (bitmap != null) {
                    // Update local cache
                    UserPreferences.saveProfileImageBase64(context, serverBase64)
                    UserPreferences.markProfileImageNeedingSync(context, false)
                    return bitmap
                }
            }
            
            // Fallback to local base64
            val localBase64 = UserPreferences.getProfileImageBase64(context)
            if (localBase64 != null) {
                val bitmap = ImageBase64Utils.base64ToBitmap(localBase64)
                if (bitmap != null) return bitmap
            }
            
            // Final fallback to file
            val imagePath = UserPreferences.getProfileImagePath(context)
            if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
                return MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.fromFile(File(imagePath)))
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile picture bitmap", e)
            null
        }
    }
    
    /**
     * Check if user has a profile picture
     */
    suspend fun hasProfilePicture(context: Context): Boolean {
        return try {
            // Check database
            val serverBase64 = ProfileService.getUserProfilePictureBase64()
            if (!serverBase64.isNullOrEmpty()) return true
            
            // Check local base64
            val localBase64 = UserPreferences.getProfileImageBase64(context)
            if (!localBase64.isNullOrEmpty()) return true
            
            // Check file
            val imagePath = UserPreferences.getProfileImagePath(context)
            if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) return true
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if profile picture exists", e)
            false
        }
    }
    
    /**
     * Clear all profile picture data
     */
    fun clearProfilePicture(context: Context) {
        UserPreferences.clearProfileImage(context)
        
        // Also try to clear from database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ProfileService.updateProfilePictureBase64("")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear profile picture from database: ${e.message}")
            }
        }
    }
} 