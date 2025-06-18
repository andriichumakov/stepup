package com.example.stepupapp.services

import android.content.Context
import android.util.Log
import com.example.stepupapp.BuildConfig
import com.example.stepupapp.models.UserProfile
import com.example.stepupapp.storage.SecureSessionStorage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from

object ProfileService {
    private const val TAG = "ProfileService"
    private val sessionStorage = SecureSessionStorage

    private val client: SupabaseClient = createSupabaseClient(
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

    private val auth = client.auth

    // Check if there's currently a session in memory
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

    // Attempt to restore session from securely stored refresh token
    suspend fun restoreSessionFromToken(context: Context): Boolean {
        val refreshToken = sessionStorage.loadRefreshToken(context)
        if (refreshToken != null) {
            return try {
                auth.refreshSession(refreshToken)
                Log.d(TAG, "Session successfully restored from refresh token.")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore session: ${e.localizedMessage}", e)
                false
            }
        }
        return false
    }

    // User Login
    suspend fun login(context: Context, email: String, password: String): UserProfile? {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            storeRefreshToken(context)
            getCurrentProfile()
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.localizedMessage}", e)
            null
        }
    }

    // User Sign Out
    suspend fun signOut(context: Context) {
        try {
            auth.signOut()
            sessionStorage.clearRefreshToken(context)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed: ${e.localizedMessage}", e)
        }
    }

    // User Registration (auth + profile insert)
    suspend fun register(context: Context, username: String, email: String, password: String): UserProfile? {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = auth.currentSessionOrNull()?.user?.id ?: run {
                Log.e(TAG, "Sign-up failed: user ID not available.")
                return null
            }

            // Insert into Profiles table manually
            val profileInsert = mapOf(
                "id" to userId,
                "name" to username,
                "email" to email,
                "pfp_url" to null
            )

            client.from("Profiles").insert(profileInsert)
            storeRefreshToken(context)
            getCurrentProfile()

        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.localizedMessage}", e)
            null
        }
    }

    // Fetch currently logged-in user's profile from server
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

    // Update profile (server-side only)
    suspend fun updateProfile(userProfile: UserProfile): Boolean {
        val id = userProfile.id ?: return false
        return try {
            client.from("Profiles")
                .update(userProfile) {
                    filter { eq("id", id) }
                }
            Log.d(TAG, "Profile updated successfully: $userProfile")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile: ${e.localizedMessage}", e)
            false
        }
    }
}
