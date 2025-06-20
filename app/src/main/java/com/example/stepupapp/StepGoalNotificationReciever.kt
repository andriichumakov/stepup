package com.example.stepupapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class StepGoalNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Check if the user has not reached their step target
        val todaySteps = UserPreferences.getWeeklySteps(context).lastOrNull()?.steps ?: 0
        val target = UserPreferences.getStepTarget(context)
        if (todaySteps < target) {
            val title = "Step Goal Not Reached"
            val message = "You haven't reached your step goal yet, go take a walk!"
            AddReminders.sendStepGoalNotification(context, title, message)
        }
        // Optionally, show a toast to confirm the notification is triggered
        // Toast.makeText(context, "Step goal reminder sent!", Toast.LENGTH_SHORT).show()
    }
}
