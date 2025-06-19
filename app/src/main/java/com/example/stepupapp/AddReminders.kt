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
        const val STREAK_CHANNEL_ID = "streak_achievement_channel"
        private const val ACHIEVEMENT_NOTIFICATION_ID = 100
        private const val STREAK_NOTIFICATION_ID = 200
        private const val ACHIEVEMENT_DISPLAY_DURATION = 2 * 60 * 1000L // 2 minutes in milliseconds
        private const val STREAK_DISPLAY_DURATION = 3 * 60 * 1000L // 3 minutes in milliseconds

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

        // Send streak achievement notification
        fun sendStreakNotification(context: Context, streak: Int) {
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

            val title = "🔥 $streak Day Streak! 🔥"
            val message = when (streak) {
                1 -> "Amazing! You've started your fitness journey with a 1-day streak! Keep it up! 💪"
                2 -> "Fantastic! You're on a 2-day streak! You're building great habits! 🌟"
                3 -> "Incredible! 3-day streak! You're on fire! 🔥"
                4 -> "Outstanding! 4-day streak! You're unstoppable! ⚡"
                5 -> "Phenomenal! 5-day streak! You're a fitness champion! 🏆"
                6 -> "Extraordinary! 6-day streak! Almost a full week! 🌈"
                7 -> "Legendary! You've been active for a full week! 🎉"
                else -> "Incredible! $streak-day streak! You're absolutely crushing it! 🚀"
            }

            val notification = NotificationCompat.Builder(context, STREAK_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.stepup_logo_bunny_small)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibration pattern for celebration
                .build()

            // Use unique notification ID for each streak level
            val notificationId = STREAK_NOTIFICATION_ID + streak
            notificationManager.notify(notificationId, notification)
            
            // Schedule cancellation after display duration
            scheduleNotificationCancellation(context, notificationManager, notificationId, STREAK_DISPLAY_DURATION)
        }

        private fun scheduleNotificationCancellation(
            context: Context,
            notificationManager: NotificationManager,
            notificationId: Int,
            duration: Long = ACHIEVEMENT_DISPLAY_DURATION
        ) {
            // Create a handler to remove the notification after the specified duration
            android.os.Handler(context.mainLooper).postDelayed({
                notificationManager.cancel(notificationId)
            }, duration)
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

            val streakChannel = NotificationChannel(
                STREAK_CHANNEL_ID,
                "Streak Achievement Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Celebrations for achieving step goal streaks."
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(streakChannel)
        }
    }
}