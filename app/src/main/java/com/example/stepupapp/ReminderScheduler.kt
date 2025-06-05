package com.example.stepupapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast

class ReminderScheduler {

    companion object {
        fun scheduleStepGoalNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Create an intent that will be triggered when the alarm goes off
            val intent = Intent(context, StepGoalNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            // Set the alarm to trigger the notification at a specific time
            val triggerTime = SystemClock.elapsedRealtime() + 5 * 60 * 1000 // For testing: 5 minutes from now

            // Schedule the alarm to repeat every day at the specified time
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY, // Repeat every day
                pendingIntent
            )

            Toast.makeText(context, "Step goal reminder set!", Toast.LENGTH_SHORT).show()
        }

        fun cancelScheduledNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, StepGoalNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.cancel(pendingIntent)

            Toast.makeText(context, "Step goal reminder canceled!", Toast.LENGTH_SHORT).show()
        }
    }
}
