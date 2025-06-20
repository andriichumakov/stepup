package com.example.stepupapp.managers

import android.content.Context
import android.util.Log
import com.example.stepupapp.services.ProfileService

class SessionManager(private val context: Context) {
    
    interface SessionCallback {
        fun onSessionRestored()
        fun onSessionFailed()
        fun onStorageCorrupted()
        fun onNoSession()
    }
    
    companion object {
        private const val TAG = "SessionManager"
    }
    
    private lateinit var secureStorageManager: SecureStorageManager
    private var callback: SessionCallback? = null
    
    fun initialize(callback: SessionCallback? = null) {
        this.callback = callback
        secureStorageManager = SecureStorageManager(context)
        secureStorageManager.initialize(createStorageCallback())
    }
    
    suspend fun restoreSession(): SessionRestoreResult {
        return try {
            Log.d(TAG, "Starting session restoration...")
            
            // First, check if storage is healthy
            val healthCheck = secureStorageManager.performHealthCheck()
            if (healthCheck == SecureStorageManager.HealthCheckResult.CORRUPTED) {
                Log.w(TAG, "Storage health check failed, clearing corrupted data")
                callback?.onStorageCorrupted()
                return SessionRestoreResult.STORAGE_CORRUPTED
            }
            
            // Try to load refresh token
            val refreshToken = secureStorageManager.loadRefreshToken()
            if (refreshToken == null) {
                Log.d(TAG, "No refresh token found")
                callback?.onNoSession()
                return SessionRestoreResult.NO_SESSION
            }
            
            // Try to restore session with the token
            val restored = ProfileService.restoreSessionFromToken(context, refreshToken)
            if (restored) {
                Log.d(TAG, "Session successfully restored")
                callback?.onSessionRestored()
                
                // Update token in storage (in case it was refreshed)
                saveCurrentSession()
                
                SessionRestoreResult.SUCCESS
            } else {
                Log.w(TAG, "Failed to restore session with existing token")
                
                // Clear invalid token
                secureStorageManager.clearRefreshToken()
                callback?.onSessionFailed()
                
                SessionRestoreResult.INVALID_TOKEN
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during session restoration: ${e.message}", e)
            callback?.onSessionFailed()
            SessionRestoreResult.ERROR
        }
    }
    
    fun saveCurrentSession(): Boolean {
        return try {
            val refreshToken = ProfileService.getCurrentRefreshToken()
            if (!refreshToken.isNullOrEmpty()) {
                secureStorageManager.saveRefreshToken(refreshToken)
            } else {
                Log.w(TAG, "No current refresh token to save")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save current session: ${e.message}", e)
            false
        }
    }
    
    fun clearSession(): Boolean {
        return try {
            secureStorageManager.clearRefreshToken()
            Log.d(TAG, "Session cleared successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear session: ${e.message}", e)
            false
        }
    }
    
    fun hasStoredSession(): Boolean {
        return secureStorageManager.isTokenStored()
    }
    
    private fun createStorageCallback() = object : SecureStorageManager.SecureStorageCallback {
        override fun onStorageCorrupted() {
            Log.w(TAG, "Storage corruption detected, session will need to be re-established")
            callback?.onStorageCorrupted()
        }
        
        override fun onStorageRestored() {
            Log.i(TAG, "Storage restored successfully")
        }
        
        override fun onStorageError(error: String) {
            Log.e(TAG, "Storage error: $error")
        }
    }
    
    enum class SessionRestoreResult {
        SUCCESS,
        NO_SESSION,
        INVALID_TOKEN,
        STORAGE_CORRUPTED,
        ERROR
    }
} 