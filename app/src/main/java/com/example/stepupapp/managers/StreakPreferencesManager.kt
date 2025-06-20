package com.example.stepupapp.managers

import com.example.stepupapp.UserPreferences
import java.util.*
import android.content.Context

class StreakPreferencesManager(
    storage: PreferencesStorage,
    private val stepPreferencesManager: StepPreferencesManager,
    private val dateProvider: DateProvider = SystemDateProvider(),
    userIdProvider: UserIdProvider = AuthUserIdProvider()
) : BasePreferencesManager(storage, userIdProvider) {
    
    companion object {
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LAST_STREAK_NOTIFICATION = "last_streak_notification"
        private const val KEY_LAST_GOAL_ACHIEVED_DATE = "last_goal_achieved_date"
        private const val TAG = "StreakPreferencesManager"
    }
    
    fun getCurrentStreak(): Int {
        return calculateCurrentStreakFromWeeklyData()
    }
    
    fun setCurrentStreak(streak: Int) {
        storage.putInt(KEY_CURRENT_STREAK, streak)
        logDebug(TAG, "Current streak set to: $streak")
    }
    
    fun getLastStreakNotification(): Int {
        return storage.getInt(KEY_LAST_STREAK_NOTIFICATION, 0)
    }
    
    fun setLastStreakNotification(streak: Int) {
        storage.putInt(KEY_LAST_STREAK_NOTIFICATION, streak)
        logDebug(TAG, "Last streak notification set to: $streak")
    }
    
    fun getLastGoalAchievedDate(): String {
        return storage.getString(KEY_LAST_GOAL_ACHIEVED_DATE, "") ?: ""
    }
    
    fun setLastGoalAchievedDate(date: String) {
        storage.putString(KEY_LAST_GOAL_ACHIEVED_DATE, date)
        logDebug(TAG, "Last goal achieved date set to: $date")
    }
    
    fun shouldSendStreakNotification(currentStreak: Int): Boolean {
        val lastNotifiedStreak = getLastStreakNotification()
        val lastGoalDate = getLastGoalAchievedDate()
        val today = dateProvider.formatDate(dateProvider.getCurrentDate(), "yyyy-MM-dd")
        
        // Always send notification if:
        // 1. Current streak is greater than last notified streak, OR
        // 2. We achieved goal today and have a streak (even if same as last notified)
        val shouldNotify = currentStreak > lastNotifiedStreak || 
                          (lastGoalDate == today && currentStreak > 0)
        
        logDebug(TAG, "Streak notification check - Current: $currentStreak, Last notified: $lastNotifiedStreak, Goal today: ${lastGoalDate == today}, Should notify: $shouldNotify")
        
        return shouldNotify
    }
    
    fun updateStreakOnGoalAchievement() {
        val today = dateProvider.formatDate(dateProvider.getCurrentDate(), "yyyy-MM-dd")
        val lastGoalDate = getLastGoalAchievedDate()
        
        if (lastGoalDate == today) {
            // Already achieved goal today, don't update streak
            logDebug(TAG, "Goal already achieved today, streak calculation will be based on weekly data")
            return
        }
        
        // Mark that we achieved the goal today
        setLastGoalAchievedDate(today)
        logDebug(TAG, "Goal achieved today, streak will be calculated from weekly data")
    }
    
    fun checkAndResetStreakIfNeeded() {
        // This method is no longer needed since we calculate streak from actual data
        // But keeping it for backward compatibility
        logDebug(TAG, "checkAndResetStreakIfNeeded called - streak now calculated from weekly data")
    }
    
    fun resetStreakNotificationTracking() {
        setLastStreakNotification(0)
        logDebug(TAG, "Streak notification tracking reset - will notify for next streak achievement")
    }
    
    private fun calculateCurrentStreakFromWeeklyData(): Int {
        return try {
            val weeklyData = stepPreferencesManager.getWeeklySteps()
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
            
            logDebug(TAG, "Calculated streak from weekly data: $streak")
            streak
        } catch (e: Exception) {
            logError(TAG, "Error calculating streak from weekly data", e)
            0
        }
    }
}

 