package com.example.stepupapp

import android.content.Context
import android.util.Log

/**
 * Test class to demonstrate and test the streak notification system
 * This can be used to manually test the streak functionality
 */
class StreakNotificationTest(private val context: Context) {
    
    companion object {
        private const val TAG = "StreakNotificationTest"
    }
    
    /**
     * Simulate achieving step goal and test streak notification
     * This method can be called to manually test the streak system
     */
    fun testStreakNotification() {
        try {
            Log.d(TAG, "Testing streak notification system")
            
            // Get current streak before testing
            val currentStreak = UserPreferences.getCurrentStreak(context)
            Log.d(TAG, "Current streak before test: $currentStreak")
            
            // Simulate achieving goal (this would normally be called by StepCounterService)
            UserPreferences.updateStreakOnGoalAchievement(context)
            
            // Get updated streak (now calculated from weekly data)
            val newStreak = UserPreferences.getCurrentStreak(context)
            Log.d(TAG, "Streak after goal achievement: $newStreak")
            
            // Check if notification should be sent
            if (UserPreferences.shouldSendStreakNotification(context, newStreak)) {
                Log.d(TAG, "Sending streak notification for $newStreak day streak")
                AddReminders.sendStreakNotification(context, newStreak)
                UserPreferences.setLastStreakNotification(context, newStreak)
            } else {
                Log.d(TAG, "No streak notification needed (already notified for this streak level)")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing streak notification", e)
        }
    }
    
    /**
     * Reset streak for testing purposes
     */
    fun resetStreakForTesting() {
        try {
            UserPreferences.resetStreakNotificationTracking(context)
            UserPreferences.setLastGoalAchievedDate(context, "")
            Log.d(TAG, "Streak notification tracking reset for testing")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting streak", e)
        }
    }
    
    /**
     * Get current streak information for debugging
     */
    fun getStreakInfo(): String {
        val currentStreak = UserPreferences.getCurrentStreak(context)
        val lastNotified = UserPreferences.getLastStreakNotification(context)
        val lastGoalDate = UserPreferences.getLastGoalAchievedDate(context)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val shouldNotify = UserPreferences.shouldSendStreakNotification(context, currentStreak)
        
        return "Current Streak: $currentStreak, Last Notified: $lastNotified, Last Goal Date: $lastGoalDate, Today: $today, Should Notify: $shouldNotify"
    }
} 