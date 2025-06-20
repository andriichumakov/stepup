package com.example.stepupapp.managers

import android.content.Context
import android.widget.Toast
import com.example.stepupapp.InterestCodeManager
import com.example.stepupapp.UserPreferences
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileSetupManager(private val context: Context) {
    
    interface SetupCallback {
        suspend fun onSetupComplete()
        suspend fun onSetupError(message: String)
    }
    
    suspend fun saveProfileSetup(
        nickname: String,
        stepGoal: Int,
        selectedInterests: Set<String>,
        callback: SetupCallback
    ) {
        // Save locally first (always succeeds)
        saveDataLocally(nickname, stepGoal, selectedInterests)
        
        // Sync to server in background
        val syncResults = syncToServer(stepGoal, selectedInterests, nickname)
        
        withContext(Dispatchers.Main) {
            if (syncResults.any { !it }) {
                callback.onSetupError("Setup saved locally. Will sync when online.")
            } else {
                callback.onSetupComplete()
            }
        }
    }
    
    private fun saveDataLocally(nickname: String, stepGoal: Int, selectedInterests: Set<String>) {
        UserPreferences.saveUserNickname(context, nickname)
        UserPreferences.setStepTarget(context, stepGoal)
        saveUserInterestsLocally(selectedInterests)
    }
    
    private suspend fun syncToServer(
        stepGoal: Int,
        selectedInterests: Set<String>,
        nickname: String
    ): List<Boolean> {
        val stepGoalSuccess = ProfileService.updateStepGoal(stepGoal)
        
        val interestsSuccess = try {
            val interestsCode = InterestCodeManager.interestsToCode(selectedInterests)
            ProfileService.updateInterestsCode(interestsCode)
        } catch (e: Exception) {
            android.util.Log.w("ProfileSetupManager", "Failed to save interests: ${e.message}")
            UserPreferences.markInterestsNeedingSync(context, true)
            false
        }
        
        val nicknameSuccess = try {
            if (nickname.isNotEmpty()) {
                ProfileService.updateNickname(nickname)
            } else true
        } catch (e: Exception) {
            android.util.Log.w("ProfileSetupManager", "Failed to save nickname: ${e.message}")
            UserPreferences.markNicknameNeedingSync(context, true)
            false
        }
        
        return listOf(stepGoalSuccess, interestsSuccess, nicknameSuccess)
    }
    
    private fun saveUserInterestsLocally(selectedInterests: Set<String>) {
        UserPreferences.saveUserInterests(context, selectedInterests)
        val interestsCode = InterestCodeManager.interestsToCode(selectedInterests)
        UserPreferences.saveInterestsCodeLocally(context, interestsCode)
    }
} 