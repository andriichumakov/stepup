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
        getPrefs(context).edit().putInt("$KEY_DAILY_STEPS_PREFIX$today", steps).apply()
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

    data class DailyStepsData(
        val day: String,
        val steps: Int,
        val target: Int
    )
}