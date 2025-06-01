package com.example.stepupapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object UserPreferences {
    private const val PREFS_NAME = "StepUpPrefs"
    private const val KEY_STEP_TARGET = "step_target"
    private const val DEFAULT_STEP_TARGET = 6000
    private const val KEY_DAILY_STEPS_PREFIX = "daily_steps_"
    private const val KEY_DAILY_CALORIES_PREFIX = "daily_calories_"
    private const val KEY_DAILY_DISTANCE_PREFIX = "daily_distance_"
    private const val DEFAULT_CALORIE_TARGET = 300 // calories
    private const val DEFAULT_DISTANCE_TARGET = 5000 // meters

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getStepTarget(context: Context): Int {
        return getPrefs(context).getInt(KEY_STEP_TARGET, DEFAULT_STEP_TARGET)
    }

    fun setStepTarget(context: Context, target: Int) {
        getPrefs(context).edit().putInt(KEY_STEP_TARGET, target).apply()
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
                result.add(DailyStepsData(dayName, steps, getStepTarget(context)))
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
                result.add(DailyStepsData(dayName, calories, DEFAULT_CALORIE_TARGET))
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
                result.add(DailyStepsData(dayName, distance, DEFAULT_DISTANCE_TARGET))
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

    data class DailyStepsData(
        val day: String,
        val steps: Int,
        val target: Int
    )
}