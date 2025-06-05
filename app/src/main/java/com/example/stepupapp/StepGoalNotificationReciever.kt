package com.example.stepupapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class StepGoalNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Define the content of your notification
        val title = "Step Goal Reminder"
        val message = "Hey, you're almost there! Keep going and reach your daily step goal."

        // Send notification
        AddReminders.sendStepGoalNotification(context, title, message)

        // Optionally, show a toast to confirm the notification is triggered
        Toast.makeText(context, "Step goal reminder sent!", Toast.LENGTH_SHORT).show()
    }
}
