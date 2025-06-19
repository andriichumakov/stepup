package com.example.stepupapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object UserPreferences {
    private const val PREFS_NAME = "StepUpPrefs"
    private const val KEY_STEP_TARGET = "step_target"
    private const val KEY_SETUP_COMPLETED = "setup_completed"
    private const val KEY_USER_INTERESTS = "user_interests"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_NICKNAME = "user_nickname"
    private const val DEFAULT_STEP_TARGET = 6000
    private const val KEY_DAILY_STEPS_PREFIX = "daily_steps_"
    private const val KEY_DAILY_CALORIES_PREFIX = "daily_calories_"
    private const val KEY_DAILY_DISTANCE_PREFIX = "daily_distance_"
    private const val DEFAULT_CALORIE_TARGET = 300 // calories
    private const val DEFAULT_DISTANCE_TARGET = 5000 // meters
    private const val KEY_SHOW_STEP_COUNTER_NOTIFICATION = "show_step_counter_notification"
    
    // Profile image constants
    private const val KEY_PROFILE_IMAGE_PATH = "profile_image_path"
    private const val KEY_PROFILE_IMAGE_BASE64 = "profile_image_base64"
    private const val KEY_PROFILE_IMAGE_NEEDS_SYNC = "profile_image_needs_sync"
    
    // Local interests code storage
    private const val KEY_LOCAL_INTERESTS_CODE = "local_interests_code"
    private const val KEY_INTERESTS_NEEDS_SYNC = "interests_needs_sync"
    
    // Streak tracking constants
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_LAST_STREAK_NOTIFICATION = "last_streak_notification"
    private const val KEY_LAST_GOAL_ACHIEVED_DATE = "last_goal_achieved_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getStepTarget(context: Context): Int {
        return getPrefs(context).getInt(KEY_STEP_TARGET, DEFAULT_STEP_TARGET)
    }

    fun setStepTarget(context: Context, target: Int) {
        getPrefs(context).edit().putInt(KEY_STEP_TARGET, target).apply()
    }

    fun isSetupCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun setSetupCompleted(context: Context, completed: Boolean = true) {
        getPrefs(context).edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    // User name management functions (user-specific)
    fun saveUserName(context: Context, name: String) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val key = "${KEY_USER_NAME}_$userId"
            getPrefs(context).edit().putString(key, name.trim()).apply()
            Log.d("UserPreferences", "Saved user name for user $userId: $name")
        } else {
            // Fallback to global key if no user is logged in (for backward compatibility)
            getPrefs(context).edit().putString(KEY_USER_NAME, name.trim()).apply()
            Log.d("UserPreferences", "Saved user name globally (no user logged in): $name")
        }
    }

    fun getUserName(context: Context): String {
        val userId = getCurrentUserId()
        return if (userId != null) {
            val key = "${KEY_USER_NAME}_$userId"
            var userName = getPrefs(context).getString(key, "") ?: ""
            
            // Migration: If no user-specific name but global name exists, migrate it
            if (userName.isEmpty()) {
                val globalName = getPrefs(context).getString(KEY_USER_NAME, "") ?: ""
                if (globalName.isNotEmpty()) {
                    Log.d("UserPreferences", "Migrating global name to user-specific: '$globalName' for user $userId")
                    getPrefs(context).edit().putString(key, globalName).apply()
                    userName = globalName
                }
            }
            
            userName
        } else {
            // Fallback to global key if no user is logged in
            getPrefs(context).getString(KEY_USER_NAME, "") ?: ""
        }
    }

    // User nickname management functions (user-specific)
    fun saveUserNickname(context: Context, nickname: String) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val key = "${KEY_USER_NICKNAME}_$userId"
            getPrefs(context).edit().putString(key, nickname.trim()).apply()
            Log.d("UserPreferences", "Saved user nickname for user $userId: $nickname")
        } else {
            // Fallback to global key if no user is logged in (for backward compatibility)
            getPrefs(context).edit().putString(KEY_USER_NICKNAME, nickname.trim()).apply()
            Log.d("UserPreferences", "Saved user nickname globally (no user logged in): $nickname")
        }
    }

    fun getUserNickname(context: Context): String {
        val userId = getCurrentUserId()
        return if (userId != null) {
            val key = "${KEY_USER_NICKNAME}_$userId"
            var userNickname = getPrefs(context).getString(key, "") ?: ""
            
            // Migration: If no user-specific nickname but global nickname exists, migrate it
            if (userNickname.isEmpty()) {
                val globalNickname = getPrefs(context).getString(KEY_USER_NICKNAME, "") ?: ""
                if (globalNickname.isNotEmpty()) {
                    Log.d("UserPreferences", "Migrating global nickname to user-specific: '$globalNickname' for user $userId")
                    getPrefs(context).edit().putString(key, globalNickname).apply()
                    userNickname = globalNickname
                }
            }
            
            userNickname
        } else {
            // Fallback to global key if no user is logged in
            getPrefs(context).getString(KEY_USER_NICKNAME, "") ?: ""
        }
    }

    // Interest management functions
    fun saveUserInterests(context: Context, interests: Set<String>) {
        val interestsString = interests.joinToString(",")
        getPrefs(context).edit().putString(KEY_USER_INTERESTS, interestsString).apply()
        Log.d("UserPreferences", "Saved interests: $interestsString")
    }

    fun getUserInterests(context: Context): Set<String> {
        val interestsString = getPrefs(context).getString(KEY_USER_INTERESTS, "")
        return if (interestsString.isNullOrEmpty()) {
            emptySet()
        } else {
            interestsString.split(",").toSet()
        }
    }

    fun getFirstUserInterest(context: Context): String {
        val interests = getUserInterests(context)
        return when {
            interests.isEmpty() -> "All"
            interests.contains("Amusements") -> "Amusements"
            interests.contains("Architecture") -> "Architecture"
            interests.contains("Cultural") -> "Cultural"
            interests.contains("Shops") -> "Shops"
            interests.contains("Foods") -> "Foods"
            interests.contains("Sport") -> "Sport"
            interests.contains("Historical") -> "Historical"
            interests.contains("Natural") -> "Natural"
            interests.contains("Other") -> "Other"
            else -> interests.first()
        }
    }

    fun saveDailySteps(context: Context, steps: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        getPrefs(context).edit().apply {
            putInt("$KEY_DAILY_STEPS_PREFIX$today", steps)
            putInt("$KEY_DAILY_CALORIES_PREFIX$today", calculateCaloriesFromSteps(steps))
            putInt("$KEY_DAILY_DISTANCE_PREFIX$today", calculateDistanceFromSteps(steps))
        }.apply()
    }

    fun getDailySteps(context: Context, date: Date): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(date)
        return getPrefs(context).getInt("$KEY_DAILY_STEPS_PREFIX$dateStr", 0)
    }

    fun getWeeklySteps(context: Context): List<DailyStepsData> {
        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val result = mutableListOf<DailyStepsData>()

            // Get the last 7 days
            for (i in 6 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = calendar.time
                val dateStr = dateFormat.format(date)
                val dayName = dayFormat.format(date)
                val steps = getPrefs(context).getInt("$KEY_DAILY_STEPS_PREFIX$dateStr", 0)
                Log.d("UserPreferences", "Getting steps for $dateStr ($dayName): $steps")
                result.add(DailyStepsData(dayName, steps, getStepTarget(context), date))
            }

            Log.d("UserPreferences", "Generated weekly data with ${result.size} days")
            return result
        } catch (e: Exception) {
            Log.e("UserPreferences", "Error getting weekly steps", e)
            return emptyList()
        }
    }

    fun saveDailyCalories(context: Context, calories: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        getPrefs(context).edit().putInt("$KEY_DAILY_CALORIES_PREFIX$today", calories).apply()
    }

    fun saveDailyDistance(context: Context, distance: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        getPrefs(context).edit().putInt("$KEY_DAILY_DISTANCE_PREFIX$today", distance).apply()
    }

    fun getDailyCalories(context: Context, date: Date): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(date)
        return getPrefs(context).getInt("$KEY_DAILY_CALORIES_PREFIX$dateStr", 0)
    }

    fun getDailyDistance(context: Context, date: Date): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(date)
        return getPrefs(context).getInt("$KEY_DAILY_DISTANCE_PREFIX$dateStr", 0)
    }

    fun getWeeklyCalories(context: Context): List<DailyStepsData> {
        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val result = mutableListOf<DailyStepsData>()

            // Get the last 7 days
            for (i in 6 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = calendar.time
                val dateStr = dateFormat.format(date)
                val dayName = dayFormat.format(date)
                // Get the stored calories value
                val calories = getPrefs(context).getInt("$KEY_DAILY_CALORIES_PREFIX$dateStr", 0)
                Log.d("UserPreferences", "Getting stored calories for $dateStr ($dayName): $calories")
                result.add(DailyStepsData(dayName, calories, DEFAULT_CALORIE_TARGET, date))
            }

            Log.d("UserPreferences", "Generated weekly calories data with ${result.size} days")
            return result
        } catch (e: Exception) {
            Log.e("UserPreferences", "Error getting weekly calories", e)
            return emptyList()
        }
    }

    fun getWeeklyDistance(context: Context): List<DailyStepsData> {
        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val result = mutableListOf<DailyStepsData>()

            // Get the last 7 days
            for (i in 6 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = calendar.time
                val dateStr = dateFormat.format(date)
                val dayName = dayFormat.format(date)
                // Get the stored distance value
                val distance = getPrefs(context).getInt("$KEY_DAILY_DISTANCE_PREFIX$dateStr", 0)
                Log.d("UserPreferences", "Getting stored distance for $dateStr ($dayName): $distance")
                result.add(DailyStepsData(dayName, distance, DEFAULT_DISTANCE_TARGET, date))
            }

            Log.d("UserPreferences", "Generated weekly distance data with ${result.size} days")
            return result
        } catch (e: Exception) {
            Log.e("UserPreferences", "Error getting weekly distance", e)
            return emptyList()
        }
    }

    // Helper function to calculate calories from steps (approximately 0.04 calories per step)
    fun calculateCaloriesFromSteps(steps: Int): Int {
        return (steps * 0.04).toInt()
    }

    // Helper function to calculate distance from steps (approximately 0.762 meters per step)
    fun calculateDistanceFromSteps(steps: Int): Int {
        return (steps * 0.762).toInt()
    }

    // Step counter notification visibility functions
    fun shouldShowStepCounterNotification(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_STEP_COUNTER_NOTIFICATION, true) // Default to true
    }

    fun setStepCounterNotificationVisibility(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_STEP_COUNTER_NOTIFICATION, show).apply()
        Log.d("UserPreferences", "Step counter notification visibility set to: $show")
    }

    // Streak tracking functions
    fun getCurrentStreak(context: Context): Int {
        return calculateCurrentStreakFromWeeklyData(context)
    }

    fun setCurrentStreak(context: Context, streak: Int) {
        getPrefs(context).edit().putInt(KEY_CURRENT_STREAK, streak).apply()
        Log.d("UserPreferences", "Current streak set to: $streak")
    }

    fun getLastStreakNotification(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_STREAK_NOTIFICATION, 0)
    }

    fun setLastStreakNotification(context: Context, streak: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_STREAK_NOTIFICATION, streak).apply()
        Log.d("UserPreferences", "Last streak notification set to: $streak")
    }

    fun getLastGoalAchievedDate(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_GOAL_ACHIEVED_DATE, "") ?: ""
    }

    fun setLastGoalAchievedDate(context: Context, date: String) {
        getPrefs(context).edit().putString(KEY_LAST_GOAL_ACHIEVED_DATE, date).apply()
        Log.d("UserPreferences", "Last goal achieved date set to: $date")
    }

    fun shouldSendStreakNotification(context: Context, currentStreak: Int): Boolean {
        val lastNotifiedStreak = getLastStreakNotification(context)
        val lastGoalDate = getLastGoalAchievedDate(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Always send notification if:
        // 1. Current streak is greater than last notified streak, OR
        // 2. We achieved goal today and have a streak (even if same as last notified)
        val shouldNotify = currentStreak > lastNotifiedStreak || 
                          (lastGoalDate == today && currentStreak > 0)
        
        Log.d("UserPreferences", "Streak notification check - Current: $currentStreak, Last notified: $lastNotifiedStreak, Goal today: ${lastGoalDate == today}, Should notify: $shouldNotify")
        
        return shouldNotify
    }

    // Calculate streak based on actual weekly data (same logic as StepsOverviewActivity)
    private fun calculateCurrentStreakFromWeeklyData(context: Context): Int {
        try {
            val weeklyData = getWeeklySteps(context)
            var streak = 0
            
            // Sort data by date in descending order (most recent first)
            val sortedData = weeklyData.sortedByDescending { it.date }
            
            // Check consecutive days from most recent
            for (data in sortedData) {
                if (data.steps >= data.target) {
                    streak++
                } else {
                    break
                }
            }
            
            Log.d("UserPreferences", "Calculated streak from weekly data: $streak")
            return streak
        } catch (e: Exception) {
            Log.e("UserPreferences", "Error calculating streak from weekly data", e)
            return 0
        }
    }

    fun updateStreakOnGoalAchievement(context: Context) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastGoalDate = getLastGoalAchievedDate(context)
        
        if (lastGoalDate == today) {
            // Already achieved goal today, don't update streak
            Log.d("UserPreferences", "Goal already achieved today, streak calculation will be based on weekly data")
            return
        }
        
        // Mark that we achieved the goal today
        setLastGoalAchievedDate(context, today)
        Log.d("UserPreferences", "Goal achieved today, streak will be calculated from weekly data")
    }

    fun checkAndResetStreakIfNeeded(context: Context) {
        // This method is no longer needed since we calculate streak from actual data
        // But keeping it for backward compatibility
        Log.d("UserPreferences", "checkAndResetStreakIfNeeded called - streak now calculated from weekly data")
    }

    fun resetStreakNotificationTracking(context: Context) {
        setLastStreakNotification(context, 0)
        Log.d("UserPreferences", "Streak notification tracking reset - will notify for next streak achievement")
    }

    fun clear(context: Context) {
        val sharedPrefs = context.getSharedPreferences("step_preferences", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
    }

    // Helper function to get current user ID
    private fun getCurrentUserId(): String? {
        return try {
            // Import ProfileService to get the current user
            com.example.stepupapp.services.ProfileService.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            Log.e("UserPreferences", "Error getting current user ID", e)
            null
        }
    }

    // Profile image management functions (user-specific)
    fun saveProfileImagePath(context: Context, imagePath: String) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val key = "${KEY_PROFILE_IMAGE_PATH}_$userId"
            getPrefs(context).edit().putString(key, imagePath).apply()
            Log.d("UserPreferences", "Saved profile image path for user $userId: $imagePath")
        } else {
            Log.w("UserPreferences", "Cannot save profile image path - no user logged in")
        }
    }

    fun getProfileImagePath(context: Context): String? {
        val userId = getCurrentUserId()
        return if (userId != null) {
            val key = "${KEY_PROFILE_IMAGE_PATH}_$userId"
            getPrefs(context).getString(key, null)
        } else {
            null
        }
    }

    fun hasProfileImage(context: Context): Boolean {
        val imagePath = getProfileImagePath(context)
        return !imagePath.isNullOrEmpty() && java.io.File(imagePath).exists()
    }

    fun clearProfileImage(context: Context) {
        val userId = getCurrentUserId()
        if (userId != null) {
            // Clear file if it exists
            val imagePath = getProfileImagePath(context)
            if (!imagePath.isNullOrEmpty()) {
                try {
                    java.io.File(imagePath).delete()
                } catch (e: Exception) {
                    Log.e("UserPreferences", "Error deleting profile image file", e)
                }
            }
            
            // Clear all user-specific profile image preferences
            val pathKey = "${KEY_PROFILE_IMAGE_PATH}_$userId"
            val base64Key = "${KEY_PROFILE_IMAGE_BASE64}_$userId"
            val syncKey = "${KEY_PROFILE_IMAGE_NEEDS_SYNC}_$userId"
            
            getPrefs(context).edit().apply {
                remove(pathKey)
                remove(base64Key)
                remove(syncKey)
            }.apply()
            
            Log.d("UserPreferences", "Profile image cleared for user $userId")
        }
    }

    fun clearAllUserSpecificData(context: Context, userId: String) {
        // Clear profile image file if it exists
        val pathKey = "${KEY_PROFILE_IMAGE_PATH}_$userId"
        val imagePath = getPrefs(context).getString(pathKey, null)
        if (!imagePath.isNullOrEmpty()) {
            try {
                java.io.File(imagePath).delete()
            } catch (e: Exception) {
                Log.e("UserPreferences", "Error deleting profile image file", e)
            }
        }
        
        // Clear all user-specific preferences
        val base64Key = "${KEY_PROFILE_IMAGE_BASE64}_$userId"
        val syncKey = "${KEY_PROFILE_IMAGE_NEEDS_SYNC}_$userId"
        val nameKey = "${KEY_USER_NAME}_$userId"
        val nameSyncKey = "name_needs_sync_$userId"
        val nicknameKey = "${KEY_USER_NICKNAME}_$userId"
        val nicknameSyncKey = "nickname_needs_sync_$userId"
        
        getPrefs(context).edit().apply {
            // Profile image data
            remove(pathKey)
            remove(base64Key)
            remove(syncKey)
            // Name data
            remove(nameKey)
            remove(nameSyncKey)
            // Nickname data
            remove(nicknameKey)
            remove(nicknameSyncKey)
        }.apply()
        
        Log.d("UserPreferences", "All user-specific data cleared for user $userId")
    }

    // Keep the old method name for backward compatibility
    fun clearAllProfileImagesForUser(context: Context, userId: String) {
        clearAllUserSpecificData(context, userId)
    }

    // Base64 profile image management functions (user-specific)
    fun saveProfileImageBase64(context: Context, base64Image: String) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val key = "${KEY_PROFILE_IMAGE_BASE64}_$userId"
            getPrefs(context).edit().putString(key, base64Image).apply()
            Log.d("UserPreferences", "Saved profile image base64 locally for user $userId")
        } else {
            Log.w("UserPreferences", "Cannot save profile image base64 - no user logged in")
        }
    }

    fun getProfileImageBase64(context: Context): String? {
        val userId = getCurrentUserId()
        return if (userId != null) {
            val key = "${KEY_PROFILE_IMAGE_BASE64}_$userId"
            getPrefs(context).getString(key, null)
        } else {
            null
        }
    }

    fun markProfileImageNeedingSync(context: Context, needsSync: Boolean) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val key = "${KEY_PROFILE_IMAGE_NEEDS_SYNC}_$userId"
            getPrefs(context).edit().putBoolean(key, needsSync).apply()
            Log.d("UserPreferences", "Profile image sync flag set to: $needsSync for user $userId")
        } else {
            Log.w("UserPreferences", "Cannot set profile image sync flag - no user logged in")
        }
    }

    fun doesProfileImageNeedSync(context: Context): Boolean {
        val userId = getCurrentUserId()
        return if (userId != null) {
            val key = "${KEY_PROFILE_IMAGE_NEEDS_SYNC}_$userId"
            getPrefs(context).getBoolean(key, false)
        } else {
            false
        }
    }
    
    /**
     * Migration helper: Get interests code from current local preferences
     * This helps transition existing users to the new database system
     */
    fun getInterestsCodeFromLocal(context: Context): String {
        val interests = getUserInterests(context)
        return InterestCodeManager.interestsToCode(interests)
    }
    
    // Local interests code management functions
    fun saveInterestsCodeLocally(context: Context, interestsCode: String) {
        getPrefs(context).edit().putString(KEY_LOCAL_INTERESTS_CODE, interestsCode).apply()
        Log.d("UserPreferences", "Saved interests code locally: $interestsCode")
    }

    fun getInterestsCodeLocally(context: Context): String? {
        return getPrefs(context).getString(KEY_LOCAL_INTERESTS_CODE, null)
    }

    fun markInterestsNeedingSync(context: Context, needsSync: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INTERESTS_NEEDS_SYNC, needsSync).apply()
        Log.d("UserPreferences", "Interests sync flag set to: $needsSync")
    }

    fun doInterestsNeedSync(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INTERESTS_NEEDS_SYNC, false)
    }
    
    /**
     * Get the most recent interests, prioritizing local cache for speed
     * Falls back gracefully if data is missing
     */
    fun getMostRecentInterests(context: Context): Set<String> {
        // Try local interests code first (fastest)
        val localCode = getInterestsCodeLocally(context)
        if (localCode != null) {
            return InterestCodeManager.codeToInterests(localCode)
        }
        
        // Fall back to string-based interests
        val stringInterests = getUserInterests(context)
        if (stringInterests.isNotEmpty()) {
            return stringInterests
        }
        
        // Ultimate fallback
        return setOf("All")
    }

    // Name sync management functions (user-specific)
    fun markNameNeedingSync(context: Context, needsSync: Boolean) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val key = "name_needs_sync_$userId"
            getPrefs(context).edit().putBoolean(key, needsSync).apply()
            Log.d("UserPreferences", "Name sync flag set to: $needsSync for user $userId")
        } else {
            Log.w("UserPreferences", "Cannot set name sync flag - no user logged in")
        }
    }

    fun doesNameNeedSync(context: Context): Boolean {
        val userId = getCurrentUserId()
        return if (userId != null) {
            val key = "name_needs_sync_$userId"
            getPrefs(context).getBoolean(key, false)
        } else {
            false
        }
    }

    // Nickname sync management functions (user-specific)
    fun markNicknameNeedingSync(context: Context, needsSync: Boolean) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val key = "nickname_needs_sync_$userId"
            getPrefs(context).edit().putBoolean(key, needsSync).apply()
            Log.d("UserPreferences", "Nickname sync flag set to: $needsSync for user $userId")
        } else {
            Log.w("UserPreferences", "Cannot set nickname sync flag - no user logged in")
        }
    }

    fun doesNicknameNeedSync(context: Context): Boolean {
        val userId = getCurrentUserId()
        return if (userId != null) {
            val key = "nickname_needs_sync_$userId"
            getPrefs(context).getBoolean(key, false)
        } else {
            false
        }
    }


    data class DailyStepsData(
        val day: String,
        val steps: Int,
        val target: Int,
        val date: Date
    )
}