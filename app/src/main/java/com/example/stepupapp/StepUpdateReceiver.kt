package com.example.stepupapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StepUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("StepUpdateReceiver", "Broadcast received with action: ${intent?.action}")
        if (intent?.action == "STEP_COUNT_UPDATE") {
            val steps = intent.getIntExtra("steps", 0)
            val distance = intent.getDoubleExtra("distance", 0.0)
            val calories = intent.getIntExtra("calories", 0)
            
            Log.d("StepUpdateReceiver", "Received update - Steps: $steps, Distance: $distance, Calories: $calories")
            
            // Forward the update to HomeActivity if it's active
            val updateIntent = Intent(context, HomeActivity::class.java).apply {
                action = "STEP_COUNT_UPDATE"
                putExtra("steps", steps)
                putExtra("distance", distance)
                putExtra("calories", calories)
            }
            context?.startActivity(updateIntent)
        }
    }
} 