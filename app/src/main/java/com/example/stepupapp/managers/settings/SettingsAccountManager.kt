package com.example.stepupapp.managers.settings

import android.content.Context
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface SettingsAccountProvider {
    suspend fun updateUsername(username: String): AccountUpdateResult
    suspend fun updatePassword(password: String): AccountUpdateResult
    suspend fun updateUserAccount(username: String, password: String): AccountUpdateResult
    suspend fun signOut(): Boolean
}

data class AccountUpdateResult(
    val success: Boolean,
    val message: String,
    val errors: List<String> = emptyList()
)

interface SettingsAccountCallback {
    fun onAccountUpdated(result: AccountUpdateResult)
    fun onSignOutCompleted(success: Boolean)
}

class SettingsAccountManager(
    private val context: Context,
    private val callback: SettingsAccountCallback?
) : SettingsAccountProvider {
    
    companion object {
        private const val TAG = "SettingsAccountManager"
        private const val MIN_USERNAME_LENGTH = 3
        private const val MIN_PASSWORD_LENGTH = 6
    }
    
    override suspend fun updateUsername(username: String): AccountUpdateResult = withContext(Dispatchers.IO) {
        try {
            val validationResult = validateUsername(username)
            if (!validationResult.success) {
                return@withContext validationResult
            }
            
            val profile = ProfileService.getCurrentProfile()
            if (profile != null) {
                val updatedProfile = profile.copy(name = username)
                val success = ProfileService.updateProfile(updatedProfile)
                
                if (success) {
                    AccountUpdateResult(true, "Username updated successfully")
                } else {
                    AccountUpdateResult(false, "Failed to update username")
                }
            } else {
                AccountUpdateResult(false, "No user profile found")
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating username: ${e.message}", e)
            AccountUpdateResult(false, "Error updating username: ${e.message}")
        }
    }
    
    override suspend fun updatePassword(password: String): AccountUpdateResult = withContext(Dispatchers.IO) {
        try {
            val validationResult = validatePassword(password)
            if (!validationResult.success) {
                return@withContext validationResult
            }
            
            ProfileService.auth.updateUser {
                this.password = password
            }
            
            AccountUpdateResult(true, "Password updated successfully")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating password: ${e.message}", e)
            
            // Check if the error indicates the password is the same as current
            val errorMessage = e.message?.lowercase() ?: ""
            if (errorMessage.contains("same") || 
                errorMessage.contains("current") || 
                errorMessage.contains("identical") ||
                errorMessage.contains("unchanged")) {
                AccountUpdateResult(false, "Try unique password")
            } else {
                AccountUpdateResult(false, "Password update failed: ${e.message}")
            }
        }
    }
    
    override suspend fun updateUserAccount(username: String, password: String): AccountUpdateResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        
        // Validate both inputs
        if (username.isNotEmpty()) {
            val usernameValidation = validateUsername(username)
            if (!usernameValidation.success) {
                errors.addAll(usernameValidation.errors)
            }
        }
        
        if (password.isNotEmpty()) {
            val passwordValidation = validatePassword(password)
            if (!passwordValidation.success) {
                errors.addAll(passwordValidation.errors)
            }
        }
        
        if (errors.isNotEmpty()) {
            return@withContext AccountUpdateResult(false, "Validation failed", errors)
        }
        
        // Check if user is authenticated
        val session = ProfileService.auth.currentSessionOrNull()
        val userId = session?.user?.id
        
        if (userId == null) {
            return@withContext AccountUpdateResult(false, "No user session found")
        }
        
        var updateSuccess = true
        val messages = mutableListOf<String>()
        
        // Update username if provided
        if (username.isNotEmpty()) {
            val usernameResult = updateUsername(username)
            if (usernameResult.success) {
                messages.add("Username updated")
            } else {
                updateSuccess = false
                messages.add("Username update failed: ${usernameResult.message}")
            }
        }
        
        // Update password if provided
        if (password.isNotEmpty()) {
            val passwordResult = updatePassword(password)
            if (passwordResult.success) {
                messages.add("Password updated")
            } else {
                updateSuccess = false
                messages.add("Password update failed: ${passwordResult.message}")
            }
        }
        
        val finalMessage = if (updateSuccess) {
            "Account updated successfully"
        } else {
            messages.joinToString("; ")
        }
        
        AccountUpdateResult(updateSuccess, finalMessage)
    }
    
    override suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        try {
            ProfileService.signOut(context)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error signing out: ${e.message}", e)
            false
        }
    }
    
    private fun validateUsername(username: String): AccountUpdateResult {
        val trimmed = username.trim()
        val errors = mutableListOf<String>()
        
        if (trimmed.length < MIN_USERNAME_LENGTH) {
            errors.add("Username must be at least $MIN_USERNAME_LENGTH characters")
        }
        
        if (trimmed.contains(" ")) {
            errors.add("Username cannot contain spaces")
        }
        
        // Add more validation rules as needed
        if (!trimmed.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            errors.add("Username can only contain letters, numbers, and underscores")
        }
        
        return if (errors.isEmpty()) {
            AccountUpdateResult(true, "Username is valid")
        } else {
            AccountUpdateResult(false, "Invalid username", errors)
        }
    }
    
    private fun validatePassword(password: String): AccountUpdateResult {
        val errors = mutableListOf<String>()
        
        if (password.length < MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least $MIN_PASSWORD_LENGTH characters")
        }
        
        return if (errors.isEmpty()) {
            AccountUpdateResult(true, "Password is valid")
        } else {
            AccountUpdateResult(false, "Invalid password", errors)
        }
    }
    
    fun isUserAuthenticated(): Boolean {
        return ProfileService.auth.currentSessionOrNull() != null
    }
} 