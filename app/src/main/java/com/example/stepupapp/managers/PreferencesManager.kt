package com.example.stepupapp.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.stepupapp.UserPreferences
import java.util.*

interface PreferencesStorage {
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>?
    fun putStringSet(key: String, value: Set<String>)
    fun remove(key: String)
    fun clear()
}

class SharedPreferencesStorage(
    private val context: Context,
    private val prefsName: String = "StepUpPrefs"
) : PreferencesStorage {
    
    private fun getPrefs(): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
    
    override fun getInt(key: String, defaultValue: Int): Int {
        return getPrefs().getInt(key, defaultValue)
    }
    
    override fun putInt(key: String, value: Int) {
        getPrefs().edit().putInt(key, value).apply()
    }
    
    override fun getString(key: String, defaultValue: String?): String? {
        return getPrefs().getString(key, defaultValue)
    }
    
    override fun putString(key: String, value: String) {
        getPrefs().edit().putString(key, value).apply()
    }
    
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return getPrefs().getBoolean(key, defaultValue)
    }
    
    override fun putBoolean(key: String, value: Boolean) {
        getPrefs().edit().putBoolean(key, value).apply()
    }
    
    override fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? {
        return getPrefs().getStringSet(key, defaultValue)
    }
    
    override fun putStringSet(key: String, value: Set<String>) {
        getPrefs().edit().putStringSet(key, value).apply()
    }
    
    override fun remove(key: String) {
        getPrefs().edit().remove(key).apply()
    }
    
    override fun clear() {
        getPrefs().edit().clear().apply()
    }
}

interface DateProvider {
    fun getCurrentDate(): Date
    fun formatDate(date: Date, pattern: String): String
}

class SystemDateProvider : DateProvider {
    override fun getCurrentDate(): Date = Date()
    
    override fun formatDate(date: Date, pattern: String): String {
        return java.text.SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }
}

interface UserIdProvider {
    fun getCurrentUserId(): String?
}

class AuthUserIdProvider : UserIdProvider {
    override fun getCurrentUserId(): String? {
        return try {
            com.example.stepupapp.services.ProfileService.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            Log.e("AuthUserIdProvider", "Error getting current user ID", e)
            null
        }
    }
}

abstract class BasePreferencesManager(
    protected val storage: PreferencesStorage,
    protected val userIdProvider: UserIdProvider = AuthUserIdProvider()
) {
    
    protected fun getUserSpecificKey(baseKey: String): String {
        val userId = userIdProvider.getCurrentUserId()
        return if (userId != null) "${baseKey}_$userId" else baseKey
    }
    
    protected fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
    }
    
    protected fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    protected fun logError(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}

class StepPreferencesManager(
    storage: PreferencesStorage,
    private val dateProvider: DateProvider = SystemDateProvider(),
    userIdProvider: UserIdProvider = AuthUserIdProvider()
) : BasePreferencesManager(storage, userIdProvider) {
    
    companion object {
        private const val KEY_STEP_TARGET = "step_target"
        private const val KEY_DAILY_STEPS_PREFIX = "daily_steps_"
        private const val KEY_DAILY_CALORIES_PREFIX = "daily_calories_"
        private const val KEY_DAILY_DISTANCE_PREFIX = "daily_distance_"
        private const val DEFAULT_STEP_TARGET = 6000
        private const val DEFAULT_CALORIE_TARGET = 300
        private const val DEFAULT_DISTANCE_TARGET = 5000
        private const val TAG = "StepPreferencesManager"
    }
    
    fun getStepTarget(): Int {
        return storage.getInt(KEY_STEP_TARGET, DEFAULT_STEP_TARGET)
    }
    
    fun setStepTarget(target: Int) {
        storage.putInt(KEY_STEP_TARGET, target)
        logDebug(TAG, "Step target set to: $target")
    }
    
    fun saveDailySteps(steps: Int) {
        val today = dateProvider.formatDate(dateProvider.getCurrentDate(), "yyyy-MM-dd")
        storage.putInt("$KEY_DAILY_STEPS_PREFIX$today", steps)
        storage.putInt("$KEY_DAILY_CALORIES_PREFIX$today", calculateCaloriesFromSteps(steps))
        storage.putInt("$KEY_DAILY_DISTANCE_PREFIX$today", calculateDistanceFromSteps(steps))
        logDebug(TAG, "Saved daily steps: $steps for date: $today")
    }
    
    fun getDailySteps(date: Date): Int {
        val dateStr = dateProvider.formatDate(date, "yyyy-MM-dd")
        return storage.getInt("$KEY_DAILY_STEPS_PREFIX$dateStr", 0)
    }
    
    fun getWeeklySteps(): List<UserPreferences.DailyStepsData> {
        return getWeeklyData(KEY_DAILY_STEPS_PREFIX) { getStepTarget() }
    }
    
    fun getWeeklyCalories(): List<UserPreferences.DailyStepsData> {
        return getWeeklyData(KEY_DAILY_CALORIES_PREFIX) { DEFAULT_CALORIE_TARGET }
    }
    
    fun getWeeklyDistance(): List<UserPreferences.DailyStepsData> {
        return getWeeklyData(KEY_DAILY_DISTANCE_PREFIX) { DEFAULT_DISTANCE_TARGET }
    }
    
    private fun getWeeklyData(prefix: String, targetProvider: () -> Int): List<UserPreferences.DailyStepsData> {
        return try {
            val calendar = Calendar.getInstance()
            val result = mutableListOf<UserPreferences.DailyStepsData>()
            
            for (i in 6 downTo 0) {
                calendar.time = dateProvider.getCurrentDate()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = calendar.time
                val dateStr = dateProvider.formatDate(date, "yyyy-MM-dd")
                val dayName = dateProvider.formatDate(date, "EEEE")
                val value = storage.getInt("$prefix$dateStr", 0)
                result.add(UserPreferences.DailyStepsData(dayName, value, targetProvider(), date))
            }
            
            logDebug(TAG, "Generated weekly data with ${result.size} days for prefix: $prefix")
            result
        } catch (e: Exception) {
            logError(TAG, "Error getting weekly data for prefix: $prefix", e)
            emptyList()
        }
    }
    
    private fun calculateCaloriesFromSteps(steps: Int): Int {
        return (steps * 0.04).toInt()
    }
    
    private fun calculateDistanceFromSteps(steps: Int): Int {
        return (steps * 0.762).toInt()
    }
}

class MemoryPreferencesManager(
    private val context: Context,
    userIdProvider: UserIdProvider = AuthUserIdProvider()
) : BasePreferencesManager(SharedPreferencesStorage(context), userIdProvider) {
    
    companion object {
        private const val MEMORY_PREFS_NAME = "memory_prefs"
        private const val KEY_LAST_MEMORY_ID = "last_memory_id"
        private const val TAG = "MemoryPreferencesManager"
    }
    
    private val memoryStorage = SharedPreferencesStorage(context, MEMORY_PREFS_NAME)
    
    fun setLastMemoryId(id: Int) {
        memoryStorage.putInt(KEY_LAST_MEMORY_ID, id)
        logDebug(TAG, "Last memory ID set to: $id")
    }
    
    fun getLastMemoryId(): Int {
        return memoryStorage.getInt(KEY_LAST_MEMORY_ID, -1)
    }
}

object PreferencesManagerFactory {
    private val managerCache = mutableMapOf<String, Any>()
    
    fun createStepPreferencesManager(context: Context): StepPreferencesManager {
        val key = "step_${context.hashCode()}"
        return managerCache.getOrPut(key) {
            StepPreferencesManager(SharedPreferencesStorage(context))
        } as StepPreferencesManager
    }
    
    fun createUserProfilePreferencesManager(context: Context): UserProfilePreferencesManager {
        val key = "profile_${context.hashCode()}"
        return managerCache.getOrPut(key) {
            UserProfilePreferencesManager(SharedPreferencesStorage(context))
        } as UserProfilePreferencesManager
    }
    
    fun createInterestPreferencesManager(context: Context): InterestPreferencesManager {
        val key = "interest_${context.hashCode()}"
        return managerCache.getOrPut(key) {
            InterestPreferencesManager(SharedPreferencesStorage(context))
        } as InterestPreferencesManager
    }
    
    fun createSyncPreferencesManager(context: Context): SyncPreferencesManager {
        val key = "sync_${context.hashCode()}"
        return managerCache.getOrPut(key) {
            SyncPreferencesManager(SharedPreferencesStorage(context))
        } as SyncPreferencesManager
    }
    
    fun createStreakPreferencesManager(context: Context): StreakPreferencesManager {
        val key = "streak_${context.hashCode()}"
        return managerCache.getOrPut(key) {
            val stepManager = createStepPreferencesManager(context)
            StreakPreferencesManager(SharedPreferencesStorage(context), stepManager)
        } as StreakPreferencesManager
    }
    
    fun createAppConfigPreferencesManager(context: Context): AppConfigPreferencesManager {
        val key = "config_${context.hashCode()}"
        return managerCache.getOrPut(key) {
            AppConfigPreferencesManager(SharedPreferencesStorage(context))
        } as AppConfigPreferencesManager
    }
    
    fun createMemoryPreferencesManager(context: Context): MemoryPreferencesManager {
        val key = "memory_${context.hashCode()}"
        return managerCache.getOrPut(key) {
            MemoryPreferencesManager(context)
        } as MemoryPreferencesManager
    }
    
    fun clearCache() {
        managerCache.clear()
    }
} 