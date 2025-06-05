package com.example.stepupapp.services

import android.content.Context
import android.util.Log
import com.example.stepupapp.BuildConfig
import com.example.stepupapp.models.UserProfile
import com.example.stepupapp.storage.LocalProfileStore
import com.google.android.gms.tasks.Tasks.await
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object ProfileService {
    private const val TAG = "ProfileService"

    // 1) Initialize Supabase client (Auth + Postgrest)
    private val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth) {
            flowType = FlowType.IMPLICIT
            scheme = "app"
            host = "supabase.com"
        }
    }

    private val auth = client.auth

    // Check if session exists (so whether user is signed in or not)
    fun isSignedIn(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    suspend fun signOut() {
        auth.signOut()
    }

    // Register a new user (email + password + username), then insert into "Profiles" table
    //    On success, return UserProfile (with supabase-generated ID), or null on failure.
    suspend fun registerProfile(
        email: String,
        username: String,
        password: String
    ): UserProfile? {
        return try {
            // Sign up with Supabase Auth
            val user = auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = auth.currentSessionOrNull()?.user?.id ?: run {
                Log.e(TAG, "Sign-up failed: no user ID returned (confirmation required?)")
                return null
            }

            // Insert profile manually — include the required 'id'
            val profileInsert = mapOf(
                "id" to userId,
                "name" to username,
                "email" to email,
                "pfp_url" to null
            )

            val response = client.from("Profiles").insert(profileInsert)

            // Fetch the just-inserted profile with timestamps and any server-side changes
            val profile = client.from("Profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<UserProfile>()

            Log.d(TAG, "Profile registered successfully: $profile")
            profile

        } catch (e: Exception) {
            Log.e(TAG, "registerProfile failed: ${e.localizedMessage}", e)
            null
        }
    }


    suspend fun login(email: String, password: String): UserProfile? {
        return try {
            val signInResult = auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = auth.currentSessionOrNull()?.user?.id
            if (userId == null) {
                Log.e(TAG, "Login failed: userId is null (session missing?)")
                return null
            }

            val fetched = client.from("Profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<UserProfile>()

            Log.d(TAG, "Login success, fetched profile: $fetched")
            fetched

        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.localizedMessage}", e)
            null
        }
    }

    // Update the server copy of a profile (e.g. username or pfpUrl change)
    suspend fun updateServerProfile(profile: UserProfile): Boolean {
        val id = profile.id ?: return false  // 🔒 Fail early if ID is null

        return try {
            client.from("Profiles")
                .update(profile) {
                    filter { eq("id", id) }
                }
            Log.d(TAG, "Server profile updated: $profile")
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateServerProfile failed: ${e.localizedMessage}", e)
            false
        }
    }


    // Local (device) storage methods using DataStore wrapper

    /**
     * Load all profiles stored on this device.
     * Commonly used to populate an "Account Selection" screen.
     */
    suspend fun loadDeviceProfiles(context: Context): List<UserProfile> {
        return LocalProfileStore.getAllProfiles(context)
    }

    /**
     * Save or update a single UserProfile in local DataStore.
     */
    suspend fun saveDeviceProfile(context: Context, profile: UserProfile) {
        LocalProfileStore.addOrUpdateProfile(context, profile)
    }

    /**
     * Remove a profile from local DataStore by ID.
     */
    suspend fun removeDeviceProfile(context: Context, profileId: String) {
        LocalProfileStore.removeProfile(context, profileId)
    }

    /**
     * Fetch the most up-to-date profile (server → local) and store it locally.
     * If no network or it fails, just returns false.
     */
    suspend fun syncFromServer(context: Context): Boolean {
        return try {
            val userId = auth.currentSessionOrNull()?.user?.id ?: return false
            val fetched = client.from("Profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<UserProfile>()
            // Overwrite local copy
            saveDeviceProfile(context, fetched)
            true
        } catch (e: Exception) {
            Log.e(TAG, "syncFromServer failed: ${e.localizedMessage}", e)
            false
        }
    }

    /**
     * Push local profile changes to server (local → server).
     * Returns true on success, false on failure.
     */
    suspend fun syncToServer(context: Context): Boolean {
        return try {
            val userId = auth.currentSessionOrNull()?.user?.id ?: return false
            // Get local copy
            val localProfiles = LocalProfileStore.getAllProfiles(context)
            val localProfile = localProfiles.firstOrNull { it.id == userId }
                ?: return false

            // Update server
            return updateServerProfile(localProfile)
        } catch (e: Exception) {
            Log.e(TAG, "syncToServer failed: ${e.localizedMessage}", e)
            false
        }
    }
}
