package com.example.stepupapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Context

class StepCounterService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var totalSteps = 0
    private var initialSteps = 0
    private var isFirstStep = true
    private var isEmulatorMode = false
    private var serviceScope: CoroutineScope? = null
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var lastResetDate: String = ""
    private var lastSensorValue: Int = 0  // Track the last sensor value

    // Track notification states
    private var hasNotified75 = false
    private var hasNotified90 = false
    private var hasNotified95 = false
    private var hasNotified100 = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "StepCounterChannel"
        private const val CHANNEL_NAME = "Step Counter Service"
        private const val STEPS_PER_KM = 1312.33595801 // Average steps per kilometer
        private const val CALORIES_PER_STEP = 0.04 // Average calories burned per step
        private const val EMULATOR_STEP_INTERVAL = 5000L // 5 seconds between steps in emulator mode
        private const val MIDNIGHT_CHECK_INTERVAL = 60000L // Check for midnight every minute
        private const val EXTRA_PRESERVED_STEPS = "preserved_steps"
        private const val EXTRA_PRESERVED_INITIAL_STEPS = "preserved_initial_steps"

        // Add a static instance to access the service
        private var instance: StepCounterService? = null

        fun getInstance(): StepCounterService? {
            return instance
        }

        fun createStartIntent(context: Context, preservedSteps: Int = 0, preservedInitialSteps: Int = 0): Intent {
            return Intent(context, StepCounterService::class.java).apply {
                putExtra(EXTRA_PRESERVED_STEPS, preservedSteps)
                putExtra(EXTRA_PRESERVED_INITIAL_STEPS, preservedInitialSteps)
            }
        }
    }

    // Add a data class to hold step count data
    data class StepCountData(
        val totalSteps: Int,
        val initialSteps: Int
    )

    // Add a public method to get current step count data
    fun getCurrentStepCountData(): StepCountData {
        return StepCountData(totalSteps, initialSteps)
    }

    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    private fun checkAndResetAtMidnight() {
        val currentDate = getCurrentDate()
        if (currentDate != lastResetDate) {
            android.util.Log.d("StepCounterService", "Midnight detected, resetting step count")
            // Reset step count
            totalSteps = 0
            initialSteps = 0
            isFirstStep = true
            lastResetDate = currentDate
            // Reset notification states
            resetNotificationStates()
            // Send update with reset steps
            sendStepUpdate(0)
        }
    }

    private fun startMidnightCheck() {
        serviceScope?.launch(Dispatchers.Default) {
            while (true) {
                try {
                    checkAndResetAtMidnight()
                    kotlinx.coroutines.delay(MIDNIGHT_CHECK_INTERVAL)
                } catch (e: Exception) {
                    android.util.Log.e("StepCounterService", "Error in midnight check", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            android.util.Log.d("StepCounterService", "Service onCreate called")
            serviceScope = CoroutineScope(Dispatchers.Default)
            localBroadcastManager = LocalBroadcastManager.getInstance(this)

            // Initialize last reset date and check if we need to reset
            val currentDate = getCurrentDate()
            if (currentDate != lastResetDate) {
                android.util.Log.d("StepCounterService", "New day detected, resetting step count")
                totalSteps = 0
                initialSteps = 0
                isFirstStep = true
                lastSensorValue = 0
                lastResetDate = currentDate
                resetNotificationStates()
            }

            // Send initial update with current steps
            sendStepUpdate(totalSteps)

            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

            // Check if we're running on an emulator
            isEmulatorMode = isEmulator()
            android.util.Log.d("StepCounterService", "Running in ${if (isEmulatorMode) "emulator" else "device"} mode")

            createNotificationChannel()
            
            // Start as foreground service with health type
            if (UserPreferences.shouldShowStepCounterNotification(this)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            } else {
                // If notifications are disabled, start as a background service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForeground(
                        NOTIFICATION_ID,
                        createMinimalNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createMinimalNotification())
                }
            }

            // Start midnight check
            startMidnightCheck()

            // Only start emulator mode if we're actually in an emulator
            if (isEmulatorMode) {
                android.util.Log.d("StepCounterService", "Starting emulator mode")
                startEmulatorMode()
            } else {
                // Register the step sensor listener
                stepSensor?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                    android.util.Log.d("StepCounterService", "Step sensor registered")
                } ?: run {
                    android.util.Log.e("StepCounterService", "No step sensor found on device")
                    Toast.makeText(this, "No step sensor found on your device", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error in onCreate: ${e.message}", e)
            stopSelf()
        }
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)
    }

    private fun startEmulatorMode() {
        try {
            android.util.Log.d("StepCounterService", "Starting emulator mode with initial steps: $totalSteps")

            // Send initial update again to ensure it's received
            sendStepUpdate(totalSteps)

            // Start a coroutine to simulate steps
            serviceScope?.launch(Dispatchers.Default) {
                android.util.Log.d("StepCounterService", "Emulator mode coroutine started")
                var lastUpdateTime = System.currentTimeMillis()
                while (true) {
                    try {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime >= EMULATOR_STEP_INTERVAL) {
                            // Simulate a step
                            totalSteps++
                            android.util.Log.d("StepCounterService", "Emulator mode - New step count: $totalSteps")
                            withContext(Dispatchers.Main) {
                                sendStepUpdate(totalSteps)
                            }
                            lastUpdateTime = currentTime
                        }
                        // Small delay to prevent CPU overuse
                        kotlinx.coroutines.delay(100)
                    } catch (e: Exception) {
                        android.util.Log.e("StepCounterService", "Error in emulator mode loop", e)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error starting emulator mode", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            android.util.Log.d("StepCounterService", "Service onStartCommand called")
            
            // Check if we need to reset for a new day
            val currentDate = getCurrentDate()
            if (currentDate != lastResetDate) {
                android.util.Log.d("StepCounterService", "New day detected in onStartCommand, resetting step count")
                totalSteps = 0
                initialSteps = 0
                isFirstStep = true
                lastSensorValue = 0
                lastResetDate = currentDate
                resetNotificationStates()
            } else {
                // Only restore steps if it's the same day
                intent?.let {
                    if (it.hasExtra(EXTRA_PRESERVED_STEPS) && it.hasExtra(EXTRA_PRESERVED_INITIAL_STEPS)) {
                        totalSteps = it.getIntExtra(EXTRA_PRESERVED_STEPS, 0)
                        initialSteps = it.getIntExtra(EXTRA_PRESERVED_INITIAL_STEPS, 0)
                        isFirstStep = false
                        lastSensorValue = initialSteps + totalSteps
                        android.util.Log.d("StepCounterService", "Restored steps - Total: $totalSteps, Initial: $initialSteps, Last Sensor: $lastSensorValue")
                    }
                }
            }

            // Send an immediate update with the current step count
            sendStepUpdate(totalSteps)

            if (!isEmulatorMode) {
                stepSensor?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                    android.util.Log.d("StepCounterService", "Step sensor registered in onStartCommand")
                }
            }
            return START_STICKY
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error in onStartCommand", e)
            stopSelf()
            return START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        try {
            if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                val currentSensorValue = event.values[0].toInt()
                
                if (isFirstStep) {
                    // For the first step of the day, use the current sensor value as initial
                    initialSteps = currentSensorValue
                    lastSensorValue = currentSensorValue
                    isFirstStep = false
                    android.util.Log.d("StepCounterService", "First step of the day - Initial steps: $initialSteps, Current sensor: $currentSensorValue")
                } else {
                    // Calculate steps since last update
                    val stepsSinceLastUpdate = currentSensorValue - lastSensorValue
                    if (stepsSinceLastUpdate > 0) {
                        totalSteps += stepsSinceLastUpdate
                        lastSensorValue = currentSensorValue
                        android.util.Log.d("StepCounterService", "Step update - Steps since last: $stepsSinceLastUpdate, Total: $totalSteps, Current sensor: $currentSensorValue")
                        sendStepUpdate(totalSteps)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error in onSensorChanged", e)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }

    private fun sendStepUpdate(steps: Int) {
        try {
            val distance = steps / STEPS_PER_KM
            val calories = (steps * CALORIES_PER_STEP).toInt()

            android.util.Log.d("StepCounterService", "Sending step update - Steps: $steps, Distance: $distance, Calories: $calories")

            // Save daily steps
            UserPreferences.saveDailySteps(this, steps)

            // Send local broadcast
            val updateIntent = Intent("LOCAL_STEP_COUNT_UPDATE").apply {
                putExtra("steps", steps)
                putExtra("distance", distance)
                putExtra("calories", calories)
            }
            localBroadcastManager.sendBroadcast(updateIntent)
            android.util.Log.d("StepCounterService", "Local broadcast sent")

            // Update notification
            updateNotification(steps, distance, calories)
            android.util.Log.d("StepCounterService", "Notification updated")

            // Check if we should send a goal reminder
            checkAndSendGoalReminder(steps)
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error sending step update", e)
        }
    }

    private fun checkAndSendGoalReminder(currentSteps: Int) {
        try {
            val target = UserPreferences.getStepTarget(this)
            val percentage = (currentSteps.toFloat() / target.toFloat()) * 100

            // Send notification when user reaches 75%, 90%, 95%, and 100% of their goal
            when {
                currentSteps >= target && !hasNotified100 -> {
                    sendGoalReminder(currentSteps, target, 100)
                    hasNotified100 = true
                }
                percentage >= 95 && !hasNotified95 && !hasNotified100 -> {
                    sendGoalReminder(currentSteps, target, 95)
                    hasNotified95 = true
                }
                percentage >= 90 && !hasNotified90 && !hasNotified100 -> {
                    sendGoalReminder(currentSteps, target, 90)
                    hasNotified90 = true
                }
                percentage >= 75 && !hasNotified75 && !hasNotified100 -> {
                    sendGoalReminder(currentSteps, target, 75)
                    hasNotified75 = true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error checking goal reminder", e)
        }
    }

    private fun sendGoalReminder(currentSteps: Int, target: Int, percentage: Int) {
        val remainingSteps = target - currentSteps
        val title = when (percentage) {
            100 -> "Step Goal Achieved! 🎉"
            else -> "Step Goal Progress: $percentage%"
        }
        val message = when {
            percentage == 100 -> "Congratulations! You've reached your daily step goal! 🎉"
            remainingSteps <= 100 -> "You're so close! Only $remainingSteps steps left to reach your goal!"
            else -> "You're at $percentage% of your goal! Only $remainingSteps steps to go!"
        }
        AddReminders.sendStepGoalNotification(this, title, message)
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error creating notification channel", e)
        }
    }

    private fun createNotification(): android.app.Notification {
        val contentText = if (isEmulatorMode) {
            "Step counter is running (Emulator Mode)"
        } else {
            "Step counter is running"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StepUp App")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createMinimalNotification(): android.app.Notification {
        // Create a minimal notification that's not visible to the user
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun updateNotification(steps: Int, distance: Double, calories: Int) {
        try {
            if (!UserPreferences.shouldShowStepCounterNotification(this)) {
                // If notifications are disabled, update with minimal notification
                val notification = createMinimalNotification()
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)
                return
            }

            val contentText = if (isEmulatorMode) {
                "Steps: $steps | Distance: ${String.format("%.2f", distance)} km | Calories: $calories (Emulator Mode)"
            } else {
                "Steps: $steps | Distance: ${String.format("%.2f", distance)} km | Calories: $calories"
            }

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("StepUp App")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error updating notification", e)
        }
    }

    // Make resetNotificationStates public and accessible
    fun resetNotificationStates() {
        hasNotified75 = false
        hasNotified90 = false
        hasNotified95 = false
        hasNotified100 = false
        android.util.Log.d("StepCounterService", "Notification states reset")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        try {
            android.util.Log.d("StepCounterService", "Service onDestroy called")
            if (!isEmulatorMode) {
                sensorManager.unregisterListener(this)
                android.util.Log.d("StepCounterService", "Step sensor unregistered")
            }
            serviceScope = null
            // Reset notification states when service is destroyed
            resetNotificationStates()
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error in onDestroy", e)
        }
    }
}