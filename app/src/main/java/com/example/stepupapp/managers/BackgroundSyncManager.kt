package com.example.stepupapp.managers

import android.content.Context
import android.util.Log
import com.example.stepupapp.UserPreferences
import com.example.stepupapp.services.ProfileService

class BackgroundSyncManager(private val context: Context) {
    
    interface SyncCallback {
        fun onSyncCompleted(syncedItems: List<String>)
        fun onSyncError(error: String)
    }
    
    suspend fun performBackgroundSync(): SyncResult {
        val syncedItems = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        try {
            // Sync pending interests
            if (UserPreferences.doInterestsNeedSync(context)) {
                Log.d("BackgroundSyncManager", "Attempting to sync pending interests...")
                val interestsSuccess = ProfileService.syncPendingInterests(context)
                
                if (interestsSuccess) {
                    Log.d("BackgroundSyncManager", "Successfully synced pending interests")
                    syncedItems.add("Interests")
                } else {
                    Log.w("BackgroundSyncManager", "Failed to sync pending interests")
                    errors.add("Interests sync failed")
                }
            }
            
            // Sync pending profile picture
            if (UserPreferences.doesProfileImageNeedSync(context)) {
                Log.d("BackgroundSyncManager", "Attempting to sync pending profile picture...")
                val pictureSuccess = ProfileService.syncPendingProfilePicture(context)
                
                if (pictureSuccess) {
                    Log.d("BackgroundSyncManager", "Successfully synced pending profile picture")
                    syncedItems.add("Profile Picture")
                } else {
                    Log.w("BackgroundSyncManager", "Failed to sync pending profile picture")
                    errors.add("Profile picture sync failed")
                }
            }

            // Sync pending nickname
            if (UserPreferences.doesNicknameNeedSync(context)) {
                Log.d("BackgroundSyncManager", "Attempting to sync pending nickname...")
                val nicknameSuccess = ProfileService.syncPendingNickname(context)
                
                if (nicknameSuccess) {
                    Log.d("BackgroundSyncManager", "Successfully synced pending nickname")
                    syncedItems.add("Nickname")
                } else {
                    Log.w("BackgroundSyncManager", "Failed to sync pending nickname")
                    errors.add("Nickname sync failed")
                }
            }

            // Sync pending name
            if (UserPreferences.doesNameNeedSync(context)) {
                Log.d("BackgroundSyncManager", "Attempting to sync pending name...")
                val nameSuccess = ProfileService.syncPendingName(context)
                
                if (nameSuccess) {
                    Log.d("BackgroundSyncManager", "Successfully synced pending name")
                    syncedItems.add("Name")
                } else {
                    Log.w("BackgroundSyncManager", "Failed to sync pending name")
                    errors.add("Name sync failed")
                }
            }
            
            if (syncedItems.isEmpty() && errors.isEmpty()) {
                Log.d("BackgroundSyncManager", "No data needs syncing")
            }
            
            return SyncResult(syncedItems, errors)
            
        } catch (e: Exception) {
            Log.e("BackgroundSyncManager", "Error during background sync: ${e.message}", e)
            return SyncResult(syncedItems, listOf("Sync error: ${e.message}"))
        }
    }
    
    data class SyncResult(
        val syncedItems: List<String>,
        val errors: List<String>
    ) {
        val isSuccessful: Boolean get() = errors.isEmpty()
        val hasSyncedItems: Boolean get() = syncedItems.isNotEmpty()
    }
} 