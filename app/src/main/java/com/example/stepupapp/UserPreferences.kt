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
    private const val DEFAULT_STEP_TARGET = 6000
    private const val KEY_DAILY_STEPS_PREFIX = "daily_steps_"
    private const val KEY_DAILY_CALORIES_PREFIX = "daily_calories_"
    private const val KEY_DAILY_DISTANCE_PREFIX = "daily_distance_"
    private const val DEFAULT_CALORIE_TARGET = 300 // calories
    private const val DEFAULT_DISTANCE_TARGET = 5000 // meters
    private const val KEY_SHOW_STEP_COUNTER_NOTIFICATION = "show_step_counter_notification"
    
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

    // User name management functions
    fun saveUserName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name.trim()).apply()
        Log.d("UserPreferences", "Saved user name: $name")
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "") ?: ""
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


    data class DailyStepsData(
        val day: String,
        val steps: Int,
        val target: Int,
        val date: Date
    )
}