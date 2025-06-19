package com.example.stepupapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Utility class for converting between Bitmap and Base64 string formats
 * Used for storing profile pictures in the database
 */
object ImageBase64Utils {
    private const val TAG = "ImageBase64Utils"
    private const val QUALITY_COMPRESSION = 85 // JPEG quality for compression
    private const val MAX_DIMENSION = 512 // Max width/height to prevent huge base64 strings
    
    /**
     * Convert a Bitmap to Base64 string
     * Automatically resizes large images to prevent database storage issues
     */
    fun bitmapToBase64(bitmap: Bitmap): String? {
        return try {
            // Resize bitmap if it's too large
            val resizedBitmap = resizeBitmapIfNeeded(bitmap)
            
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY_COMPRESSION, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            Log.d(TAG, "Bitmap converted to base64. Size: ${byteArray.size} bytes")
            
            base64String
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to base64", e)
            null
        }
    }
    
    /**
     * Convert a Base64 string to Bitmap
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            Log.d(TAG, "Base64 converted to bitmap. Dimensions: ${bitmap?.width}x${bitmap?.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error converting base64 to bitmap", e)
            null
        }
    }
    
    /**
     * Resize bitmap if it exceeds maximum dimensions to prevent huge base64 strings
     */
    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return bitmap // No resizing needed
        }
        
        // Calculate new dimensions maintaining aspect ratio
        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = MAX_DIMENSION
            newHeight = (MAX_DIMENSION / aspectRatio).toInt()
        } else {
            newHeight = MAX_DIMENSION
            newWidth = (MAX_DIMENSION * aspectRatio).toInt()
        }
        
        Log.d(TAG, "Resizing bitmap from ${width}x${height} to ${newWidth}x${newHeight}")
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Get the estimated size of a base64 string in KB
     */
    fun getBase64SizeKB(base64String: String): Double {
        // Base64 encoding increases size by ~33%, then we account for the actual bytes
        val bytes = base64String.length * 0.75 // Approximate bytes from base64
        return bytes / 1024.0
    }
    
    /**
     * Convert URI to Bitmap
     * Used for handling images selected from gallery or camera
     */
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            Log.d(TAG, "URI converted to bitmap. Dimensions: ${bitmap?.width}x${bitmap?.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to bitmap", e)
            null
        }
    }
    
    /**
     * Validate that a base64 string is a valid image
     */
    fun isValidImageBase64(base64String: String): Boolean {
        return try {
            val bitmap = base64ToBitmap(base64String)
            bitmap != null
        } catch (e: Exception) {
            false
        }
    }
} 