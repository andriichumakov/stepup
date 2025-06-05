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

class StepCounterService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var totalSteps = 0
    private var initialSteps = 0
    private var isFirstStep = true
    private var isEmulatorMode = false
    private var serviceScope: CoroutineScope? = null
    private lateinit var localBroadcastManager: LocalBroadcastManager

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "StepCounterChannel"
        private const val CHANNEL_NAME = "Step Counter Service"
        private const val STEPS_PER_KM = 1312.33595801 // Average steps per kilometer
        private const val CALORIES_PER_STEP = 0.04 // Average calories burned per step
        private const val EMULATOR_STEP_INTERVAL = 5000L // 5 seconds between steps in emulator mode
    }

    override fun onCreate() {
        super.onCreate()
        try {
            android.util.Log.d("StepCounterService", "Service onCreate called")
            serviceScope = CoroutineScope(Dispatchers.Default)
            localBroadcastManager = LocalBroadcastManager.getInstance(this)

            // Send initial update immediately
            sendStepUpdate(totalSteps)

            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

            // Check if we're running on an emulator
            isEmulatorMode = isEmulator()
            android.util.Log.d("StepCounterService", "Running in ${if (isEmulatorMode) "emulator" else "device"} mode")

            createNotificationChannel()
            
            // Start as foreground service with health type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }

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
                if (isFirstStep) {
                    initialSteps = event.values[0].toInt()
                    isFirstStep = false
                    android.util.Log.d("StepCounterService", "First step detected, initial steps: $initialSteps")
                }

                val currentSteps = event.values[0].toInt()
                totalSteps = currentSteps - initialSteps
                android.util.Log.d("StepCounterService", "Step sensor update - Current steps: $currentSteps, Initial steps: $initialSteps, Total steps: $totalSteps")
                sendStepUpdate(totalSteps)
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
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error sending step update", e)
        }
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

    private fun updateNotification(steps: Int, distance: Double, calories: Int) {
        try {
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

    override fun onDestroy() {
        try {
            android.util.Log.d("StepCounterService", "Service onDestroy called")
            if (!isEmulatorMode) {
                sensorManager.unregisterListener(this)
                android.util.Log.d("StepCounterService", "Step sensor unregistered")
            }
            serviceScope = null
            super.onDestroy()
        } catch (e: Exception) {
            android.util.Log.e("StepCounterService", "Error in onDestroy", e)
        }
    }
}