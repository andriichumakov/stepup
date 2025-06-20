package com.example.stepupapp.managers

import android.content.Context
import com.example.stepupapp.InterestCodeManager

class InterestPreferencesManager(
    storage: PreferencesStorage,
    userIdProvider: UserIdProvider = AuthUserIdProvider()
) : BasePreferencesManager(storage, userIdProvider) {
    
    companion object {
        private const val KEY_USER_INTERESTS = "user_interests"
        private const val KEY_LOCAL_INTERESTS_CODE = "local_interests_code"
        private const val TAG = "InterestPreferencesManager"
    }
    
    fun saveUserInterests(interests: Set<String>) {
        val interestsString = interests.joinToString(",")
        storage.putString(KEY_USER_INTERESTS, interestsString)
        logDebug(TAG, "Saved interests: $interestsString")
    }
    
    fun getUserInterests(): Set<String> {
        val interestsString = storage.getString(KEY_USER_INTERESTS, "")
        return if (interestsString.isNullOrEmpty()) {
            emptySet()
        } else {
            interestsString.split(",").toSet()
        }
    }
    
    fun getFirstUserInterest(): String {
        val interests = getUserInterests()
        return when {
            interests.isEmpty() -> "All"
            interests.contains("Amusements") -> "Amusements"
            interests.contains("Architecture") -> "Architecture"
            interests.contains("Cultural") -> "Cultural"
            interests.contains("Shops") -> "Shops"
            interests.contains("Foods") -> "Foods"
            interests.contains("Sport") -> "Sport"
            interests.contains("Historical") -> "Historical"
            interests.contains("Natural") -> "Natural"
            interests.contains("Other") -> "Other"
            else -> interests.first()
        }
    }
    
    fun getInterestsCodeFromLocal(): String {
        val interests = getUserInterests()
        return InterestCodeManager.interestsToCode(interests)
    }
    
    fun saveInterestsCodeLocally(interestsCode: String) {
        storage.putString(KEY_LOCAL_INTERESTS_CODE, interestsCode)
        logDebug(TAG, "Saved interests code locally: $interestsCode")
    }
    
    fun getInterestsCodeLocally(): String? {
        return storage.getString(KEY_LOCAL_INTERESTS_CODE, null)
    }
    
    fun getMostRecentInterests(): Set<String> {
        // Try local interests code first (fastest)
        val localCode = getInterestsCodeLocally()
        if (localCode != null) {
            return InterestCodeManager.codeToInterests(localCode)
        }
        
        // Fall back to string-based interests
        val stringInterests = getUserInterests()
        if (stringInterests.isNotEmpty()) {
            return stringInterests
        }
        
        // Ultimate fallback
        return setOf("All")
    }
}

 