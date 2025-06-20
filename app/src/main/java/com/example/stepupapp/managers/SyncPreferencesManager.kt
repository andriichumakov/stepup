package com.example.stepupapp.managers

class SyncPreferencesManager(
    storage: PreferencesStorage,
    userIdProvider: UserIdProvider = AuthUserIdProvider()
) : BasePreferencesManager(storage, userIdProvider) {
    
    companion object {
        private const val KEY_PROFILE_IMAGE_NEEDS_SYNC = "profile_image_needs_sync"
        private const val KEY_INTERESTS_NEEDS_SYNC = "interests_needs_sync"
        private const val KEY_NAME_NEEDS_SYNC = "name_needs_sync"
        private const val KEY_NICKNAME_NEEDS_SYNC = "nickname_needs_sync"
        private const val TAG = "SyncPreferencesManager"
    }
    
    // Profile image sync
    fun markProfileImageNeedingSync(needsSync: Boolean) {
        val userId = userIdProvider.getCurrentUserId()
        if (userId != null) {
            val key = "${KEY_PROFILE_IMAGE_NEEDS_SYNC}_$userId"
            storage.putBoolean(key, needsSync)
            logDebug(TAG, "Profile image sync flag set to: $needsSync for user $userId")
        } else {
            logWarning(TAG, "Cannot set profile image sync flag - no user logged in")
        }
    }

    fun doesProfileImageNeedSync(): Boolean {
        val userId = userIdProvider.getCurrentUserId()
        return if (userId != null) {
            val key = "${KEY_PROFILE_IMAGE_NEEDS_SYNC}_$userId"
            storage.getBoolean(key, false)
        } else {
            false
        }
    }
    
    // Interests sync
    fun markInterestsNeedingSync(needsSync: Boolean) {
        storage.putBoolean(KEY_INTERESTS_NEEDS_SYNC, needsSync)
        logDebug(TAG, "Interests sync flag set to: $needsSync")
    }

    fun doInterestsNeedSync(): Boolean {
        return storage.getBoolean(KEY_INTERESTS_NEEDS_SYNC, false)
    }
    
    // Name sync (user-specific)
    fun markNameNeedingSync(needsSync: Boolean) {
        val userId = userIdProvider.getCurrentUserId()
        if (userId != null) {
            val key = "${KEY_NAME_NEEDS_SYNC}_$userId"
            storage.putBoolean(key, needsSync)
            logDebug(TAG, "Name sync flag set to: $needsSync for user $userId")
        } else {
            logWarning(TAG, "Cannot set name sync flag - no user logged in")
        }
    }

    fun doesNameNeedSync(): Boolean {
        val userId = userIdProvider.getCurrentUserId()
        return if (userId != null) {
            val key = "${KEY_NAME_NEEDS_SYNC}_$userId"
            storage.getBoolean(key, false)
        } else {
            false
        }
    }
    
    // Nickname sync (user-specific)
    fun markNicknameNeedingSync(needsSync: Boolean) {
        val userId = userIdProvider.getCurrentUserId()
        if (userId != null) {
            val key = "${KEY_NICKNAME_NEEDS_SYNC}_$userId"
            storage.putBoolean(key, needsSync)
            logDebug(TAG, "Nickname sync flag set to: $needsSync for user $userId")
        } else {
            logWarning(TAG, "Cannot set nickname sync flag - no user logged in")
        }
    }

    fun doesNicknameNeedSync(): Boolean {
        val userId = userIdProvider.getCurrentUserId()
        return if (userId != null) {
            val key = "${KEY_NICKNAME_NEEDS_SYNC}_$userId"
            storage.getBoolean(key, false)
        } else {
            false
        }
    }
} 