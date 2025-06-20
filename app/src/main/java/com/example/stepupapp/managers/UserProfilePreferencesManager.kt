package com.example.stepupapp.managers

import android.content.Context
import java.io.File

class UserProfilePreferencesManager(
    storage: PreferencesStorage,
    userIdProvider: UserIdProvider = AuthUserIdProvider()
) : BasePreferencesManager(storage, userIdProvider) {
    
    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_NICKNAME = "user_nickname"
        private const val KEY_PROFILE_IMAGE_PATH = "profile_image_path"
        private const val KEY_PROFILE_IMAGE_BASE64 = "profile_image_base64"
        private const val TAG = "UserProfilePreferencesManager"
    }
    
    fun saveUserName(name: String) {
        val trimmedName = name.trim()
        val key = getUserSpecificKey(KEY_USER_NAME)
        storage.putString(key, trimmedName)
        
        val userId = userIdProvider.getCurrentUserId()
        if (userId != null) {
            logDebug(TAG, "Saved user name for user $userId: $trimmedName")
        } else {
            storage.putString(KEY_USER_NAME, trimmedName)
            logDebug(TAG, "Saved user name globally (no user logged in): $trimmedName")
        }
    }
    
    fun getUserName(): String {
        val userId = userIdProvider.getCurrentUserId()
        return if (userId != null) {
            val key = getUserSpecificKey(KEY_USER_NAME)
            var userName = storage.getString(key, "") ?: ""
            
            // Migration: If no user-specific name but global name exists, migrate it
            if (userName.isEmpty()) {
                val globalName = storage.getString(KEY_USER_NAME, "") ?: ""
                if (globalName.isNotEmpty()) {
                    logDebug(TAG, "Migrating global name to user-specific: '$globalName' for user $userId")
                    storage.putString(key, globalName)
                    userName = globalName
                }
            }
            userName
        } else {
            storage.getString(KEY_USER_NAME, "") ?: ""
        }
    }
    
    fun saveUserNickname(nickname: String) {
        val trimmedNickname = nickname.trim()
        val userId = userIdProvider.getCurrentUserId()
        logDebug(TAG, "Saving nickname '$trimmedNickname' for user ID: $userId")
        
        if (userId != null) {
            val key = getUserSpecificKey(KEY_USER_NICKNAME)
            storage.putString(key, trimmedNickname)
            logDebug(TAG, "Saved user nickname for user $userId: $trimmedNickname")
        } else {
            storage.putString(KEY_USER_NICKNAME, trimmedNickname)
            logDebug(TAG, "Saved user nickname globally (no user logged in): $trimmedNickname")
        }
    }
    
    fun getUserNickname(): String {
        val userId = userIdProvider.getCurrentUserId()
        logDebug(TAG, "Getting nickname for user ID: $userId")
        
        return if (userId != null) {
            val key = getUserSpecificKey(KEY_USER_NICKNAME)
            var userNickname = storage.getString(key, "") ?: ""
            logDebug(TAG, "Found user-specific nickname: '$userNickname'")
            
            // Migration: If no user-specific nickname but global nickname exists, migrate it
            if (userNickname.isEmpty()) {
                val globalNickname = storage.getString(KEY_USER_NICKNAME, "") ?: ""
                if (globalNickname.isNotEmpty()) {
                    logDebug(TAG, "Migrating global nickname to user-specific: '$globalNickname' for user $userId")
                    storage.putString(key, globalNickname)
                    userNickname = globalNickname
                } else {
                    logDebug(TAG, "No global nickname found for migration")
                }
            }
            
            logDebug(TAG, "Returning nickname: '$userNickname'")
            userNickname
        } else {
            val globalNickname = storage.getString(KEY_USER_NICKNAME, "") ?: ""
            logDebug(TAG, "No user ID, returning global nickname: '$globalNickname'")
            globalNickname
        }
    }
    
    fun saveProfileImagePath(imagePath: String) {
        val userId = userIdProvider.getCurrentUserId()
        if (userId != null) {
            val key = getUserSpecificKey(KEY_PROFILE_IMAGE_PATH)
            storage.putString(key, imagePath)
            logDebug(TAG, "Saved profile image path for user $userId: $imagePath")
        } else {
            logWarning(TAG, "Cannot save profile image path - no user logged in")
        }
    }
    
    fun getProfileImagePath(): String? {
        val userId = userIdProvider.getCurrentUserId()
        return if (userId != null) {
            val key = getUserSpecificKey(KEY_PROFILE_IMAGE_PATH)
            storage.getString(key, null)
        } else {
            null
        }
    }
    
    fun hasProfileImage(): Boolean {
        val imagePath = getProfileImagePath()
        return !imagePath.isNullOrEmpty() && File(imagePath).exists()
    }
    
    fun saveProfileImageBase64(base64Image: String) {
        val userId = userIdProvider.getCurrentUserId()
        if (userId != null) {
            val key = getUserSpecificKey(KEY_PROFILE_IMAGE_BASE64)
            storage.putString(key, base64Image)
            logDebug(TAG, "Saved profile image base64 locally for user $userId")
        } else {
            logWarning(TAG, "Cannot save profile image base64 - no user logged in")
        }
    }
    
    fun getProfileImageBase64(): String? {
        val userId = userIdProvider.getCurrentUserId()
        return if (userId != null) {
            val key = getUserSpecificKey(KEY_PROFILE_IMAGE_BASE64)
            storage.getString(key, null)
        } else {
            null
        }
    }
    
    fun clearProfileImage() {
        val userId = userIdProvider.getCurrentUserId()
        if (userId != null) {
            // Clear file if it exists
            val imagePath = getProfileImagePath()
            if (!imagePath.isNullOrEmpty()) {
                try {
                    File(imagePath).delete()
                } catch (e: Exception) {
                    logError(TAG, "Error deleting profile image file", e)
                }
            }
            
            // Clear all user-specific profile image preferences
            val pathKey = getUserSpecificKey(KEY_PROFILE_IMAGE_PATH)
            val base64Key = getUserSpecificKey(KEY_PROFILE_IMAGE_BASE64)
            
            storage.remove(pathKey)
            storage.remove(base64Key)
            
            logDebug(TAG, "Profile image cleared for user $userId")
        }
    }
    
    fun clearAllUserSpecificData(userId: String) {
        // Clear profile image file if it exists
        val pathKey = "${KEY_PROFILE_IMAGE_PATH}_$userId"
        val imagePath = storage.getString(pathKey, null)
        if (!imagePath.isNullOrEmpty()) {
            try {
                File(imagePath).delete()
            } catch (e: Exception) {
                logError(TAG, "Error deleting profile image file", e)
            }
        }
        
        // Clear all user-specific preferences
        val base64Key = "${KEY_PROFILE_IMAGE_BASE64}_$userId"
        val nameKey = "${KEY_USER_NAME}_$userId"
        val nicknameKey = "${KEY_USER_NICKNAME}_$userId"
        
        storage.remove(pathKey)
        storage.remove(base64Key)
        storage.remove(nameKey)
        storage.remove(nicknameKey)
        
        logDebug(TAG, "All user-specific profile data cleared for user $userId")
    }
} 