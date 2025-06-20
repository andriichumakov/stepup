package com.example.stepupapp.managers

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStorageManager(private val context: Context) {
    
    interface SecureStorageCallback {
        fun onStorageCorrupted()
        fun onStorageRestored()
        fun onStorageError(error: String)
    }
    
    companion object {
        private const val TAG = "SecureStorageManager"
        private const val ALIAS = "SessionAESKey"
        private const val PREFS_NAME = "secure_session"
        private const val PREFS_KEY = "encrypted_refresh_token"
    }
    
    private var callback: SecureStorageCallback? = null
    
    fun initialize(callback: SecureStorageCallback? = null) {
        this.callback = callback
    }
    
    fun saveRefreshToken(refreshToken: String): Boolean {
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(refreshToken.toByteArray(Charsets.UTF_8))
            val combined = iv + ciphertext
            val encoded = Base64.encodeToString(combined, Base64.DEFAULT)

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREFS_KEY, encoded)
                .apply()
            
            Log.d(TAG, "Refresh token saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save refresh token: ${e.message}", e)
            callback?.onStorageError("Failed to save refresh token")
            false
        }
    }
    
    fun loadRefreshToken(): String? {
        return try {
            val key = getOrCreateKey()
            val encoded = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREFS_KEY, null) ?: return null
            
            val combined = Base64.decode(encoded, Base64.DEFAULT)
            val iv = combined.sliceArray(0 until 12)
            val ciphertext = combined.sliceArray(12 until combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(ciphertext)
            
            val token = decrypted.toString(Charsets.UTF_8)
            Log.d(TAG, "Refresh token loaded successfully")
            return token
            
        } catch (e: AEADBadTagException) {
            Log.w(TAG, "Stored refresh token is corrupted or tampered with. Clearing storage.", e)
            handleCorruptedStorage()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load refresh token: ${e.message}", e)
            callback?.onStorageError("Failed to load refresh token")
            null
        }
    }
    
    fun clearRefreshToken(): Boolean {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(PREFS_KEY)
                .apply()
            
            Log.d(TAG, "Refresh token cleared successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear refresh token: ${e.message}", e)
            callback?.onStorageError("Failed to clear refresh token")
            false
        }
    }
    
    fun isTokenStored(): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.contains(PREFS_KEY)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if token is stored: ${e.message}", e)
            false
        }
    }
    
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        
        // Try to get existing key
        try {
            keyStore.getKey(ALIAS, null)?.let { return it as SecretKey }
        } catch (e: Exception) {
            Log.w(TAG, "Existing key is corrupted, generating new key", e)
            // Key exists but is corrupted, delete it
            try {
                keyStore.deleteEntry(ALIAS)
            } catch (deleteException: Exception) {
                Log.w(TAG, "Failed to delete corrupted key", deleteException)
            }
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(spec)
        val newKey = keyGenerator.generateKey()
        Log.d(TAG, "Generated new encryption key")
        return newKey
    }
    
    private fun handleCorruptedStorage() {
        try {
            // Clear corrupted data
            clearRefreshToken()
            
            // Delete and recreate the encryption key
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(ALIAS)) {
                keyStore.deleteEntry(ALIAS)
                Log.d(TAG, "Deleted corrupted encryption key")
            }
            
            callback?.onStorageCorrupted()
            Log.i(TAG, "Corrupted storage cleaned up successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle corrupted storage: ${e.message}", e)
            callback?.onStorageError("Failed to recover from corrupted storage")
        }
    }
    
    fun performHealthCheck(): HealthCheckResult {
        return try {
            // Test key access
            val key = getOrCreateKey()
            
            // Test encryption/decryption with dummy data
            val testData = "health_check_test"
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(testData.toByteArray())
            
            // Test decryption
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(encrypted)
            val decryptedString = decrypted.toString(Charsets.UTF_8)
            
            if (testData == decryptedString) {
                HealthCheckResult.HEALTHY
            } else {
                HealthCheckResult.CORRUPTED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Health check failed: ${e.message}", e)
            HealthCheckResult.CORRUPTED
        }
    }
    
    enum class HealthCheckResult {
        HEALTHY,
        CORRUPTED,
        ERROR
    }
} 