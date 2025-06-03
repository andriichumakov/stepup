package com.example.stepupapp.connection

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.stepupapp.BuildConfig

object ProfileService {

    private const val TAG = "ProfileService"

    // Create Supabase client with Postgrest installed
    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
    }

    fun checkLoginStatus() : Boolean {
        return false
    }

    fun login(email: String, password: String): Boolean {
        return false
    }

    fun register_profile(email: String, username: String): Boolean {

    }


    fun insertTestRow() { // insert a default row into "Test" table
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Insert empty map to trigger server-side defaults (id, timestamp, uuid)
                val result = supabase
                    .from("Test")
                    .insert(Unit) // no fields needed

                Log.d(TAG, "Insert successful: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Insert failed: ${e.localizedMessage}", e)
            }
        }
    }
}
