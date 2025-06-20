package com.example.stepupapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.stepupapp.managers.*
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
        return PreferencesManagerFactory.createStepPreferencesManager(context).getStepTarget()
    }

    fun setStepTarget(context: Context, target: Int) {
        PreferencesManagerFactory.createStepPreferencesManager(context).setStepTarget(target)
    }

    fun isSetupCompleted(context: Context): Boolean {
        return PreferencesManagerFactory.createAppConfigPreferencesManager(context).isSetupCompleted()
    }

    fun setSetupCompleted(context: Context, completed: Boolean = true) {
        PreferencesManagerFactory.createAppConfigPreferencesManager(context).setSetupCompleted(completed)
    }

    // User name management functions (user-specific)
    fun saveUserName(context: Context, name: String) {
        PreferencesManagerFactory.createUserProfilePreferencesManager(context).saveUserName(name)
    }

    fun getUserName(context: Context): String {
        return PreferencesManagerFactory.createUserProfilePreferencesManager(context).getUserName()
    }

    // User nickname management functions (user-specific)
    fun saveUserNickname(context: Context, nickname: String) {
        PreferencesManagerFactory.createUserProfilePreferencesManager(context).saveUserNickname(nickname)
    }

    fun getUserNickname(context: Context): String {
        return PreferencesManagerFactory.createUserProfilePreferencesManager(context).getUserNickname()
    }

    // Interest management functions
    fun saveUserInterests(context: Context, interests: Set<String>) {
        PreferencesManagerFactory.createInterestPreferencesManager(context).saveUserInterests(interests)
    }

    fun getUserInterests(context: Context): Set<String> {
        return PreferencesManagerFactory.createInterestPreferencesManager(context).getUserInterests()
    }

    fun getFirstUserInterest(context: Context): String {
        return PreferencesManagerFactory.createInterestPreferencesManager(context).getFirstUserInterest()
    }

    fun saveDailySteps(context: Context, steps: Int) {
        PreferencesManagerFactory.createStepPreferencesManager(context).saveDailySteps(steps)
    }

    fun getDailySteps(context: Context, date: Date): Int {
        return PreferencesManagerFactory.createStepPreferencesManager(context).getDailySteps(date)
    }

    fun getWeeklySteps(context: Context): List<DailyStepsData> {
        return PreferencesManagerFactory.createStepPreferencesManager(context).getWeeklySteps()
    }

    fun getWeeklyCalories(context: Context): List<DailyStepsData> {
        return PreferencesManagerFactory.createStepPreferencesManager(context).getWeeklyCalories()
    }

    fun getWeeklyDistance(context: Context): List<DailyStepsData> {
        return PreferencesManagerFactory.createStepPreferencesManager(context).getWeeklyDistance()
    }

    fun saveDailyCalories(context: Context, calories: Int) {
        // This is handled automatically by saveDailySteps in the new implementation
        Log.d("UserPreferences", "saveDailyCalories called - calories are auto-calculated from steps")
    }

    fun saveDailyDistance(context: Context, distance: Int) {
        // This is handled automatically by saveDailySteps in the new implementation
        Log.d("UserPreferences", "saveDailyDistance called - distance is auto-calculated from steps")
    }

    fun getDailyCalories(context: Context, date: Date): Int {
        val steps = getDailySteps(context, date)
        return calculateCaloriesFromSteps(steps)
    }

    fun getDailyDistance(context: Context, date: Date): Int {
        val steps = getDailySteps(context, date)
        return calculateDistanceFromSteps(steps)
    }

    // Helper function to calculate calories from steps (approximately 0.04 calories per step)
    fun calculateCaloriesFromSteps(steps: Int): Int {
        return (steps * 0.04).toInt()
    }

    // Helper function to calculate distance from steps (approximately 0.762 meters per step)
    fun calculateDistanceFromSteps(steps: Int): Int {
        return (steps * 0.762).toInt()
    }
    fun setLastMemoryId(context: Context, id: Int) {
        PreferencesManagerFactory.createMemoryPreferencesManager(context).setLastMemoryId(id)
    }

    fun getLastMemoryId(context: Context): Int {
        return PreferencesManagerFactory.createMemoryPreferencesManager(context).getLastMemoryId()
    }

    // Step counter notification visibility functions
    fun shouldShowStepCounterNotification(context: Context): Boolean {
        return PreferencesManagerFactory.createAppConfigPreferencesManager(context).shouldShowStepCounterNotification()
    }

    fun setStepCounterNotificationVisibility(context: Context, show: Boolean) {
        PreferencesManagerFactory.createAppConfigPreferencesManager(context).setStepCounterNotificationVisibility(show)
    }

    // Streak tracking functions
    fun getCurrentStreak(context: Context): Int {
        return PreferencesManagerFactory.createStreakPreferencesManager(context).getCurrentStreak()
    }

    fun setCurrentStreak(context: Context, streak: Int) {
        PreferencesManagerFactory.createStreakPreferencesManager(context).setCurrentStreak(streak)
    }

    fun getLastStreakNotification(context: Context): Int {
        return PreferencesManagerFactory.createStreakPreferencesManager(context).getLastStreakNotification()
    }

    fun setLastStreakNotification(context: Context, streak: Int) {
        PreferencesManagerFactory.createStreakPreferencesManager(context).setLastStreakNotification(streak)
    }

    fun getLastGoalAchievedDate(context: Context): String {
        return PreferencesManagerFactory.createStreakPreferencesManager(context).getLastGoalAchievedDate()
    }

    fun setLastGoalAchievedDate(context: Context, date: String) {
        PreferencesManagerFactory.createStreakPreferencesManager(context).setLastGoalAchievedDate(date)
    }

    fun shouldSendStreakNotification(context: Context, currentStreak: Int): Boolean {
        return PreferencesManagerFactory.createStreakPreferencesManager(context).shouldSendStreakNotification(currentStreak)
    }

    fun updateStreakOnGoalAchievement(context: Context) {
        PreferencesManagerFactory.createStreakPreferencesManager(context).updateStreakOnGoalAchievement()
    }

    fun checkAndResetStreakIfNeeded(context: Context) {
        PreferencesManagerFactory.createStreakPreferencesManager(context).checkAndResetStreakIfNeeded()
    }

    fun resetStreakNotificationTracking(context: Context) {
        PreferencesManagerFactory.createStreakPreferencesManager(context).resetStreakNotificationTracking()
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
        // Also clear memory preferences
        val memoryPrefs = context.getSharedPreferences("memory_prefs", Context.MODE_PRIVATE)
        memoryPrefs.edit().clear().apply()
        // Clear manager cache
        PreferencesManagerFactory.clearCache()
    }

    // Helper function to get current user ID - kept for backward compatibility
    private fun getCurrentUserId(): String? {
        return try {
            com.example.stepupapp.services.ProfileService.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            Log.e("UserPreferences", "Error getting current user ID", e)
            null
        }
    }

    // Profile image management functions (user-specific)
    fun saveProfileImagePath(context: Context, imagePath: String) {
        PreferencesManagerFactory.createUserProfilePreferencesManager(context).saveProfileImagePath(imagePath)
    }

    fun getProfileImagePath(context: Context): String? {
        return PreferencesManagerFactory.createUserProfilePreferencesManager(context).getProfileImagePath()
    }

    fun hasProfileImage(context: Context): Boolean {
        return PreferencesManagerFactory.createUserProfilePreferencesManager(context).hasProfileImage()
    }

    fun clearProfileImage(context: Context) {
        PreferencesManagerFactory.createUserProfilePreferencesManager(context).clearProfileImage()
    }

    fun clearAllUserSpecificData(context: Context, userId: String) {
        PreferencesManagerFactory.createUserProfilePreferencesManager(context).clearAllUserSpecificData(userId)
    }

    // Keep the old method name for backward compatibility
    fun clearAllProfileImagesForUser(context: Context, userId: String) {
        clearAllUserSpecificData(context, userId)
    }

    // Base64 profile image management functions (user-specific)
    fun saveProfileImageBase64(context: Context, base64Image: String) {
        PreferencesManagerFactory.createUserProfilePreferencesManager(context).saveProfileImageBase64(base64Image)
    }

    fun getProfileImageBase64(context: Context): String? {
        return PreferencesManagerFactory.createUserProfilePreferencesManager(context).getProfileImageBase64()
    }

    fun markProfileImageNeedingSync(context: Context, needsSync: Boolean) {
        PreferencesManagerFactory.createSyncPreferencesManager(context).markProfileImageNeedingSync(needsSync)
    }

    fun doesProfileImageNeedSync(context: Context): Boolean {
        return PreferencesManagerFactory.createSyncPreferencesManager(context).doesProfileImageNeedSync()
    }
    
    fun getInterestsCodeFromLocal(context: Context): String {
        return PreferencesManagerFactory.createInterestPreferencesManager(context).getInterestsCodeFromLocal()
    }
    
    fun saveInterestsCodeLocally(context: Context, interestsCode: String) {
        PreferencesManagerFactory.createInterestPreferencesManager(context).saveInterestsCodeLocally(interestsCode)
    }

    fun getInterestsCodeLocally(context: Context): String? {
        return PreferencesManagerFactory.createInterestPreferencesManager(context).getInterestsCodeLocally()
    }

    fun markInterestsNeedingSync(context: Context, needsSync: Boolean) {
        PreferencesManagerFactory.createSyncPreferencesManager(context).markInterestsNeedingSync(needsSync)
    }

    fun doInterestsNeedSync(context: Context): Boolean {
        return PreferencesManagerFactory.createSyncPreferencesManager(context).doInterestsNeedSync()
    }
    
    fun getMostRecentInterests(context: Context): Set<String> {
        return PreferencesManagerFactory.createInterestPreferencesManager(context).getMostRecentInterests()
    }

    // Name sync management functions (user-specific)
    fun markNameNeedingSync(context: Context, needsSync: Boolean) {
        PreferencesManagerFactory.createSyncPreferencesManager(context).markNameNeedingSync(needsSync)
    }

    fun doesNameNeedSync(context: Context): Boolean {
        return PreferencesManagerFactory.createSyncPreferencesManager(context).doesNameNeedSync()
    }

    // Nickname sync management functions (user-specific)
    fun markNicknameNeedingSync(context: Context, needsSync: Boolean) {
        PreferencesManagerFactory.createSyncPreferencesManager(context).markNicknameNeedingSync(needsSync)
    }

    fun doesNicknameNeedSync(context: Context): Boolean {
        return PreferencesManagerFactory.createSyncPreferencesManager(context).doesNicknameNeedSync()
    }

    data class DailyStepsData(
        val day: String,
        val steps: Int,
        val target: Int,
        val date: Date
    )
}