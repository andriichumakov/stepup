package com.example.stepupapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.os.Build
import android.content.Context
import androidx.core.app.NotificationCompat
import android.app.Application
import android.content.Intent
import android.app.PendingIntent

class AddReminders : Application() {

    companion object {
        const val CHANNEL_ID = "step_goal_channel"
        private const val ACHIEVEMENT_NOTIFICATION_ID = 100
        private const val ACHIEVEMENT_DISPLAY_DURATION = 2 * 60 * 1000L // 2 minutes in milliseconds

        // Send the notification from here
        fun sendStepGoalNotification(context: Context, title: String, message: String) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create an intent to open the app when notification is tapped
            val intent = Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.stepup_logo_bunny_small)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .build()

            // Use a unique notification ID based on the percentage
            val notificationId = when {
                message.contains("Congratulations") -> {
                    // For achievement notification, use special ID and schedule cancellation
                    scheduleNotificationCancellation(context, notificationManager, ACHIEVEMENT_NOTIFICATION_ID)
                    ACHIEVEMENT_NOTIFICATION_ID
                }
                title.contains("95%") -> 95
                title.contains("90%") -> 90
                title.contains("75%") -> 75
                else -> 0
            }
            notificationManager.notify(notificationId, notification)
        }

        private fun scheduleNotificationCancellation(
            context: Context,
            notificationManager: NotificationManager,
            notificationId: Int
        ) {
            // Create a handler to remove the notification after the specified duration
            android.os.Handler(context.mainLooper).postDelayed({
                notificationManager.cancel(notificationId)
            }, ACHIEVEMENT_DISPLAY_DURATION)
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Goal Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications to track your progress towards your daily step goal."
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}