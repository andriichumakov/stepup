package com.example.stepupapp.services

import android.content.Context
import android.util.Log
import com.example.stepupapp.BuildConfig
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

    private fun getCurrentRefreshToken(): String? {
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
            auth.signOut()
            sessionStorage.clearRefreshToken(context)
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
                "email" to email,
                "pfp_url" to null
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
