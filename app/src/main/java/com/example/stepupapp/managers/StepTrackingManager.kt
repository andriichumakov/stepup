package com.example.stepupapp.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.stepupapp.StepCounterService
import com.example.stepupapp.UserPreferences
import com.example.stepupapp.databinding.ActivityHomeBinding
import java.util.*

class StepTrackingManager(
    private val context: Context,
    private val binding: ActivityHomeBinding
) {
    
    interface StepUpdateCallback {
        fun onStepDataUpdated(steps: Int, distance: Double, calories: Int)
        fun onStepTrackingError(error: String)
    }
    
    private var target: Int = 6000
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var callback: StepUpdateCallback? = null
    
    private val stepUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("StepTrackingManager", "Local broadcast received with action: ${intent?.action}")
            if (intent?.action == "LOCAL_STEP_COUNT_UPDATE") {
                val steps = intent.getIntExtra("steps", 0)
                val distance = intent.getDoubleExtra("distance", 0.0)
                val calories = intent.getIntExtra("calories", 0)

                Log.d("StepTrackingManager", "Received update - Steps: $steps, Distance: $distance, Calories: $calories")
                updateUI(steps, distance, calories)
                callback?.onStepDataUpdated(steps, distance, calories)
                Log.d("StepTrackingManager", "UI updated with new values")
            }
        }
    }
    
    fun initialize(callback: StepUpdateCallback?) {
        this.callback = callback
        localBroadcastManager = LocalBroadcastManager.getInstance(context)
        
        // Get current target and set up progress bar
        target = UserPreferences.getStepTarget(context)
        binding.stepProgressBar.max = target
        updateTargetText()
        
        // Get current step count and update UI
        val currentSteps = UserPreferences.getDailySteps(context, Date())
        val currentDistance = calculateDistance(currentSteps)
        val currentCalories = calculateCalories(currentSteps)
        updateUI(currentSteps, currentDistance, currentCalories)
        
        // Register for local broadcasts
        val filter = IntentFilter("LOCAL_STEP_COUNT_UPDATE")
        localBroadcastManager.registerReceiver(stepUpdateReceiver, filter)
        Log.d("StepTrackingManager", "Registered for local broadcasts")
    }
    
    fun startStepCounterService() {
        try {
            Log.d("StepTrackingManager", "Starting step counter service")
            val serviceIntent = Intent(context, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d("StepTrackingManager", "Step counter service started successfully")
        } catch (e: Exception) {
            Log.e("StepTrackingManager", "Error starting step counter service", e)
            callback?.onStepTrackingError("Error starting step counter service")
            updateUI(0, 0.0, 0)
        }
    }
    
    fun refreshStepCount(): StepData {
        return try {
            Log.d("StepTrackingManager", "Refreshing step count")
            val currentSteps = UserPreferences.getDailySteps(context, Date())
            val currentDistance = calculateDistance(currentSteps)
            val currentCalories = calculateCalories(currentSteps)
            updateUI(currentSteps, currentDistance, currentCalories)
            
            Log.d("StepTrackingManager", "Step count refreshed successfully")
            StepData(currentSteps, currentDistance, currentCalories)
        } catch (e: Exception) {
            Log.e("StepTrackingManager", "Error refreshing step count", e)
            callback?.onStepTrackingError("Error refreshing step count")
            StepData(0, 0.0, 0)
        }
    }
    
    fun updateStepTarget() {
        target = UserPreferences.getStepTarget(context)
        binding.stepProgressBar.max = target
        updateTargetText()
    }
    
    fun getCurrentSteps(): Int {
        return binding.stepCountText.text.toString().split(" ")[0].toIntOrNull() ?: 0
    }
    
    fun cleanup() {
        try {
            localBroadcastManager.unregisterReceiver(stepUpdateReceiver)
        } catch (e: Exception) {
            Log.e("StepTrackingManager", "Error unregistering receiver", e)
        }
    }
    
    private fun updateUI(steps: Int, distance: Double, calories: Int) {
        try {
            Log.d("StepTrackingManager", "Updating UI - Steps: $steps, Distance: $distance, Calories: $calories")
            binding.stepCountText.text = "$steps steps"
            binding.stepProgressBar.progress = steps
            binding.distanceText.text = String.format("%.2f km", distance)
            binding.caloriesText.text = "$calories Cal"
            Log.d("StepTrackingManager", "UI update complete")
        } catch (e: Exception) {
            Log.e("StepTrackingManager", "Error updating UI", e)
        }
    }
    
    private fun updateTargetText() {
        binding.targetText.text = "Target: $target"
    }
    
    private fun calculateDistance(steps: Int): Double {
        return steps / 1312.33595801 // Convert steps to kilometers
    }
    
    private fun calculateCalories(steps: Int): Int {
        return (steps * 0.04).toInt() // Convert steps to calories
    }
    
    data class StepData(
        val steps: Int,
        val distance: Double,
        val calories: Int
    )
} 