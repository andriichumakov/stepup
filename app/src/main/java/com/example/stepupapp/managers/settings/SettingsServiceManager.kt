package com.example.stepupapp.managers.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.stepupapp.StepCounterService
import com.example.stepupapp.UserPreferences

interface SettingsServiceProvider {
    fun restartStepCounterService()
    fun resetNotificationStates()
    fun resetStreakNotificationTracking()
    fun getServiceStepData(): ServiceStepData?
}

data class ServiceStepData(
    val totalSteps: Int,
    val initialSteps: Int
)

class SettingsServiceManager(
    private val context: Context
) : SettingsServiceProvider {
    
    companion object {
        private const val TAG = "SettingsServiceManager"
    }
    
    override fun restartStepCounterService() {
        try {
            // Get current step data before restarting service
            val stepData = getServiceStepData() ?: ServiceStepData(0, 0)
            
            // Stop the current service
            val stopIntent = Intent(context, StepCounterService::class.java)
            context.stopService(stopIntent)
            
            // Create new service intent with preserved step data
            val startIntent = StepCounterService.createStartIntent(
                context,
                stepData.totalSteps,
                stepData.initialSteps
            )
            
            // Start the service based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
            
            Log.d(TAG, "Step counter service restarted with preserved data: $stepData")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting step counter service: ${e.message}", e)
        }
    }
    
    override fun resetNotificationStates() {
        try {
            // Reset notification states in the service
            StepCounterService.getInstance()?.resetNotificationStates()
            Log.d(TAG, "Notification states reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting notification states: ${e.message}", e)
        }
    }
    
    override fun resetStreakNotificationTracking() {
        try {
            // Reset streak notification tracking so new streaks with new target are notified
            UserPreferences.resetStreakNotificationTracking(context)
            Log.d(TAG, "Streak notification tracking reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting streak notification tracking: ${e.message}", e)
        }
    }
    
    override fun getServiceStepData(): ServiceStepData? {
        return try {
            val currentService = StepCounterService.getInstance()
            val stepData = currentService?.getCurrentStepCountData()
            
            if (stepData != null) {
                ServiceStepData(stepData.totalSteps, stepData.initialSteps)
            } else {
                Log.w(TAG, "No step data available from service")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting service step data: ${e.message}", e)
            null
        }
    }
    
    fun isServiceRunning(): Boolean {
        return StepCounterService.getInstance() != null
    }
    
    fun startStepCounterService() {
        try {
            val intent = Intent(context, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Step counter service started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting step counter service: ${e.message}", e)
        }
    }
    
    fun stopStepCounterService() {
        try {
            val intent = Intent(context, StepCounterService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Step counter service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping step counter service: ${e.message}", e)
        }
    }
} 