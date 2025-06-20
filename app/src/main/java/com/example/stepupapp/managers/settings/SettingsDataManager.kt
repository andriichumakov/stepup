package com.example.stepupapp.managers.settings

import android.content.Context
import com.example.stepupapp.UserPreferences
import com.example.stepupapp.services.ProfileService
import com.example.stepupapp.InterestCodeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface SettingsDataProvider {
    suspend fun loadProfileData(): ProfileData
    suspend fun loadStepTarget(): Int
    suspend fun loadNotificationPreference(): Boolean
    suspend fun loadInterests(): Set<String>
    suspend fun saveStepTarget(target: Int): Boolean
    suspend fun saveNotificationPreference(enabled: Boolean)
    suspend fun saveInterests(interests: Set<String>): Boolean
    suspend fun saveNickname(nickname: String): Boolean
}

data class ProfileData(
    val nickname: String = "",
    val hasProfileImage: Boolean = false
)

interface SettingsDataCallback {
    fun onDataLoaded()
    fun onDataSaved(success: Boolean, message: String)
    fun onDataError(error: String)
}

class SettingsDataManager(
    private val context: Context,
    private val callback: SettingsDataCallback?
) : SettingsDataProvider {
    
    companion object {
        private const val TAG = "SettingsDataManager"
    }
    
    override suspend fun loadProfileData(): ProfileData = withContext(Dispatchers.IO) {
        var nickname = ""
        var hasProfileImage = false
        
        try {
            // Try database first
            val profile = ProfileService.getCurrentProfile()
            if (profile?.nickname != null && profile.nickname.isNotEmpty()) {
                nickname = profile.nickname
                // Update local storage for consistency
                UserPreferences.saveUserNickname(context, nickname)
                UserPreferences.markNicknameNeedingSync(context, false)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to load nickname from database: ${e.message}")
        }
        
        // Fallback to local storage if needed
        if (nickname.isEmpty()) {
            nickname = UserPreferences.getUserNickname(context)
        }
        
        // Check for profile image
        hasProfileImage = UserPreferences.hasProfileImage(context)
        
        ProfileData(nickname, hasProfileImage)
    }
    
    override suspend fun loadStepTarget(): Int = withContext(Dispatchers.IO) {
        UserPreferences.getStepTarget(context)
    }
    
    override suspend fun loadNotificationPreference(): Boolean = withContext(Dispatchers.IO) {
        UserPreferences.shouldShowStepCounterNotification(context)
    }
    
    override suspend fun loadInterests(): Set<String> = withContext(Dispatchers.IO) {
        try {
            // Try server first
            val serverInterestsCode = ProfileService.getUserInterestsCode()
            if (serverInterestsCode != null) {
                val interests = InterestCodeManager.codeToInterests(serverInterestsCode)
                
                // Update local cache
                UserPreferences.saveUserInterests(context, interests)
                UserPreferences.saveInterestsCodeLocally(context, serverInterestsCode)
                UserPreferences.markInterestsNeedingSync(context, false)
                
                return@withContext interests
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to load interests from server: ${e.message}")
        }
        
        // Fallback to local storage
        val localCode = UserPreferences.getInterestsCodeLocally(context)
        return@withContext if (localCode != null) {
            InterestCodeManager.codeToInterests(localCode)
        } else {
            UserPreferences.getUserInterests(context)
        }
    }
    
    override suspend fun saveStepTarget(target: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            UserPreferences.setStepTarget(context, target)
            val success = ProfileService.updateStepGoal(target)
            success
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save step target: ${e.message}", e)
            false
        }
    }
    
    override suspend fun saveNotificationPreference(enabled: Boolean) = withContext(Dispatchers.IO) {
        UserPreferences.setStepCounterNotificationVisibility(context, enabled)
    }
    
    override suspend fun saveInterests(interests: Set<String>): Boolean = withContext(Dispatchers.IO) {
        val interestsCode = InterestCodeManager.interestsToCode(interests)
        
        // Always save locally first
        UserPreferences.saveUserInterests(context, interests)
        UserPreferences.saveInterestsCodeLocally(context, interestsCode)
        
        try {
            val success = ProfileService.updateInterestsCode(interestsCode)
            if (success) {
                UserPreferences.markInterestsNeedingSync(context, false)
            } else {
                UserPreferences.markInterestsNeedingSync(context, true)
            }
            success
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to sync interests: ${e.message}", e)
            UserPreferences.markInterestsNeedingSync(context, true)
            false
        }
    }
    
    override suspend fun saveNickname(nickname: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Save locally first
            UserPreferences.saveUserNickname(context, nickname)
            
            // Try to sync to server
            val success = ProfileService.updateNickname(nickname)
            if (success) {
                UserPreferences.markNicknameNeedingSync(context, false)
            } else {
                UserPreferences.markNicknameNeedingSync(context, true)
            }
            true // Return true since local save succeeded
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save nickname: ${e.message}", e)
            false
        }
    }
} 