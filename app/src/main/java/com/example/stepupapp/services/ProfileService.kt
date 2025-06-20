package com.example.stepupapp.services

import android.content.Context
import android.util.Log
import com.example.stepupapp.BuildConfig
import com.example.stepupapp.UserPreferences
import com.example.stepupapp.models.AuthResult
import com.example.stepupapp.models.UserProfile
import com.example.stepupapp.storage.SecureSessionStorage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from

object ProfileService {
    private const val TAG = "ProfileService"
    private val sessionStorage = SecureSessionStorage

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY,
    ) {
        install(Postgrest)
        install(Auth) {
            flowType = FlowType.IMPLICIT
            scheme = "app"
            host = "supabase.com"
        }
    }

    val auth = client.auth

    fun isSignedIn(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    fun getCurrentRefreshToken(): String? {
        return auth.currentSessionOrNull()?.refreshToken
    }

    private fun storeRefreshToken(context: Context) {
        val refreshToken = getCurrentRefreshToken()
        if (!refreshToken.isNullOrEmpty()) {
            sessionStorage.saveRefreshToken(context, refreshToken)
        }
    }

    suspend fun restoreSessionFromToken(context: Context): Boolean {
        val refreshToken = sessionStorage.loadRefreshToken(context)
        return if (refreshToken != null) {
            try {
                auth.refreshSession(refreshToken)
                Log.d(TAG, "Session successfully restored from refresh token.")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore session: ${e.localizedMessage}", e)
                false
            }
        } else {
            false
        }
    }

    suspend fun restoreSessionFromToken(context: Context, refreshToken: String): Boolean {
        return try {
            auth.refreshSession(refreshToken)
            Log.d(TAG, "Session successfully restored from provided refresh token.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore session: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun login(context: Context, email: String, password: String): AuthResult<UserProfile> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            storeRefreshToken(context)
            val profile = getCurrentProfile()
            if (profile != null) {
                AuthResult.Success(profile)
            } else {
                AuthResult.Error("Failed to load user profile.")
            }
        } catch (e: Exception) {
            val errorMessage = extractSupabaseError(e)
            Log.e(TAG, "Login failed: $errorMessage", e)
            AuthResult.Error(errorMessage)
        }
    }

    suspend fun hasSetStepGoal(): Boolean {
        return try {
            val profile = getCurrentProfile()
            profile?.step_goal != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check step goal: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun signOut(context: Context) {
        try {
            // Get user ID before signing out to clear their data
            val userId = auth.currentSessionOrNull()?.user?.id
            
            auth.signOut()
            sessionStorage.clearRefreshToken(context)
            
            // Clear all user-specific data if we have the user ID
            if (userId != null) {
                UserPreferences.clearAllUserSpecificData(context, userId)
                Log.d(TAG, "Cleared user-specific data for user $userId on logout")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed: ${e.localizedMessage}", e)
        }
    }

    suspend fun register(context: Context, username: String, email: String, password: String): AuthResult<UserProfile> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = auth.currentSessionOrNull()?.user?.id ?: return AuthResult.Error("User ID not found after signup.")

            val profileInsert = mapOf(
                "id" to userId,
                "name" to username,
                "nickname" to username, // Initially, nickname same as account name
                "email" to email,
                "pfp_64base" to null
            )
            client.from("Profiles").insert(profileInsert)
            storeRefreshToken(context)

            val profile = getCurrentProfile()
            if (profile != null) {
                AuthResult.Success(profile)
            } else {
                AuthResult.Error("Failed to load user profile.")
            }
        } catch (e: Exception) {
            val errorMessage = extractSupabaseError(e)
            Log.e(TAG, "Registration failed: $errorMessage", e)
            AuthResult.Error(errorMessage)
        }
    }

    suspend fun getCurrentProfile(): UserProfile? {
        return try {
            val userId = auth.currentSessionOrNull()?.user?.id ?: return null

            client.from("Profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<UserProfile>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch profile: ${e.localizedMessage}", e)
            null
        }
    }

    suspend fun updateProfile(userProfile: UserProfile): Boolean {
        val id = userProfile.id ?: return false
        return try {
            client.from("Profiles")
                .update(userProfile) { filter { eq("id", id) } }
            Log.d(TAG, "Profile updated successfully: $userProfile")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun updateStepGoal(stepGoal: Int): Boolean {
        val id = auth.currentSessionOrNull()?.user?.id ?: return false
        return try {
            client.from("Profiles")
                .update(mapOf("step_goal" to stepGoal)) {
                    filter { eq("id", id) }
                }
            Log.d(TAG, "Step goal updated successfully: $stepGoal")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update step goal: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun updateInterestsCode(interestsCode: String): Boolean {
        val id = auth.currentSessionOrNull()?.user?.id ?: return false
        return try {
            client.from("Profiles")
                .update(mapOf("interests_code" to interestsCode)) {
                    filter { eq("id", id) }
                }
            Log.d(TAG, "Interests code updated successfully: $interestsCode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update interests code: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun getUserInterestsCode(): String? {
        return try {
            getCurrentProfile()?.interestsCode
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user interests code: ${e.localizedMessage}", e)
            null
        }
    }

    suspend fun updateNickname(nickname: String): Boolean {
        val id = auth.currentSessionOrNull()?.user?.id ?: return false
        return try {
            client.from("Profiles")
                .update(mapOf("nickname" to nickname)) {
                    filter { eq("id", id) }
                }
            Log.d(TAG, "Nickname updated successfully: $nickname")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update nickname: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun getUserNickname(): String? {
        return try {
            getCurrentProfile()?.nickname
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user nickname: ${e.localizedMessage}", e)
            null
        }
    }

    /**
     * Sync local interests to server if they need syncing
     * Call this when the app starts or when network connectivity is restored
     */
    suspend fun syncPendingInterests(context: Context): Boolean {
        return try {
            if (!UserPreferences.doInterestsNeedSync(context)) {
                Log.d(TAG, "No interests need syncing")
                return true // Nothing to sync
            }

            val localCode = UserPreferences.getInterestsCodeLocally(context)
            if (localCode == null) {
                Log.w(TAG, "Interests marked for sync but no local code found")
                UserPreferences.markInterestsNeedingSync(context, false) // Clear the flag
                return true
            }

            val success = updateInterestsCode(localCode)
            if (success) {
                UserPreferences.markInterestsNeedingSync(context, false)
                Log.d(TAG, "Successfully synced pending interests: $localCode")
            } else {
                Log.w(TAG, "Failed to sync pending interests")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending interests: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun updateProfilePictureBase64(base64Image: String): Boolean {
        val id = auth.currentSessionOrNull()?.user?.id ?: return false
        return try {
            client.from("Profiles")
                .update(mapOf("pfp_64base" to base64Image)) {
                    filter { eq("id", id) }
                }
            Log.d(TAG, "Profile picture updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile picture: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun getUserProfilePictureBase64(): String? {
        return try {
            getCurrentProfile()?.pfp64Base
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user profile picture: ${e.localizedMessage}", e)
            null
        }
    }

    /**
     * Sync local profile picture to server if it needs syncing
     */
    suspend fun syncPendingProfilePicture(context: Context): Boolean {
        return try {
            if (!UserPreferences.doesProfileImageNeedSync(context)) {
                Log.d(TAG, "No profile picture needs syncing")
                return true // Nothing to sync
            }

            val localBase64 = UserPreferences.getProfileImageBase64(context)
            if (localBase64 == null) {
                Log.w(TAG, "Profile picture marked for sync but no local base64 found")
                UserPreferences.markProfileImageNeedingSync(context, false) // Clear the flag
                return true
            }

            val success = updateProfilePictureBase64(localBase64)
            if (success) {
                UserPreferences.markProfileImageNeedingSync(context, false)
                Log.d(TAG, "Successfully synced pending profile picture")
            } else {
                Log.w(TAG, "Failed to sync pending profile picture")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending profile picture: ${e.localizedMessage}", e)
            false
        }
    }

    /**
     * Sync local nickname to server if it needs syncing
     */
    suspend fun syncPendingNickname(context: Context): Boolean {
        return try {
            if (!UserPreferences.doesNicknameNeedSync(context)) {
                Log.d(TAG, "No nickname needs syncing")
                return true // Nothing to sync
            }

            val localNickname = UserPreferences.getUserNickname(context)
            if (localNickname.isEmpty()) {
                Log.w(TAG, "Nickname marked for sync but no local nickname found")
                UserPreferences.markNicknameNeedingSync(context, false) // Clear the flag
                return true
            }

            val success = updateNickname(localNickname)
            if (success) {
                UserPreferences.markNicknameNeedingSync(context, false)
                Log.d(TAG, "Successfully synced pending nickname: $localNickname")
            } else {
                Log.w(TAG, "Failed to sync pending nickname")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending nickname: ${e.localizedMessage}", e)
            false
        }
    }

    /**
     * Sync local name to server if it needs syncing
     */
    suspend fun syncPendingName(context: Context): Boolean {
        return try {
            if (!UserPreferences.doesNameNeedSync(context)) {
                Log.d(TAG, "No name needs syncing")
                return true // Nothing to sync
            }

            val localName = UserPreferences.getUserName(context)
            if (localName.isEmpty()) {
                Log.w(TAG, "Name marked for sync but no local name found")
                UserPreferences.markNameNeedingSync(context, false) // Clear the flag
                return true
            }

            val currentProfile = getCurrentProfile()
            if (currentProfile != null) {
                val updatedProfile = currentProfile.copy(name = localName)
                val success = updateProfile(updatedProfile)
                if (success) {
                    UserPreferences.markNameNeedingSync(context, false)
                    Log.d(TAG, "Successfully synced pending name: $localName")
                } else {
                    Log.w(TAG, "Failed to sync pending name")
                }
                success
            } else {
                Log.w(TAG, "No current profile available for name sync")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending name: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun updatePassword(newPassword: String): Boolean {
        return try {
            auth.updateUser {
                password = newPassword
            }
            Log.d(TAG, "Password updated successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update password: ${e.localizedMessage}", e)
            false
        }
    }

    private fun extractSupabaseError(e: Exception): String {
        return when (e) {
            is RestException -> e.error ?: e.message ?: "Unknown error"
            else -> e.message ?: "Unknown error"
        }
    }
}
