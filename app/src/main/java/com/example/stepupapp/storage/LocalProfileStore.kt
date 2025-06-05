package com.example.stepupapp.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences.Key
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.stepupapp.models.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DATASTORE_NAME = "user_profiles_prefs"

// MUST be outside object or class — this is correct placement
val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)
private val PROFILES_KEY = stringPreferencesKey("device_profiles_json")

object LocalProfileStore {
    private val PROFILES_KEY = stringPreferencesKey("device_profiles_json")

    /**
     * Read all stored UserProfiles from DataStore.
     * Returns an empty list if nothing is stored or on parse error.
     */
    suspend fun getAllProfiles(context: Context): List<UserProfile> {
        return try {
            val jsonString = context.dataStore.data
                .map { prefs -> prefs[PROFILES_KEY] ?: "" }
                .first()

            if (jsonString.isBlank()) {
                emptyList()
            } else {
                Json.decodeFromString(jsonString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Overwrite the list of profiles.
     */
    suspend fun saveAllProfiles(context: Context, profiles: List<UserProfile>) {
        try {
            val jsonString = Json.encodeToString(profiles)
            context.dataStore.edit { prefs ->
                prefs[PROFILES_KEY] = jsonString
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Add or update a single profile by ID.
     */
    suspend fun addOrUpdateProfile(context: Context, profile: UserProfile) {
        val current = getAllProfiles(context).toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        saveAllProfiles(context, current)
    }

    /**
     * Remove a profile by ID.
     */
    suspend fun removeProfile(context: Context, profileId: String) {
        val updated = getAllProfiles(context).filter { it.id != profileId }
        saveAllProfiles(context, updated)
    }
}
