package com.example.stepupapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepupapp.databinding.ActivityStepsOverviewBinding
import java.text.SimpleDateFormat
import java.util.*

class StepsOverviewActivity : BaseActivity() {
    private lateinit var binding: ActivityStepsOverviewBinding
    private val target by lazy { UserPreferences.getStepTarget(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("StepsOverviewActivity", "onCreate started")
        try {
            Log.d("StepsOverviewActivity", "Inflating layout")
            binding = ActivityStepsOverviewBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("StepsOverviewActivity", "Layout inflated successfully")

            // Set up back button
            binding.backButton.setOnClickListener {
                Log.d("StepsOverviewActivity", "Back button clicked")
                finish()
            }

            // Set up weekly history display
            Log.d("StepsOverviewActivity", "Starting to update weekly history")
            updateWeeklyHistory()
            Log.d("StepsOverviewActivity", "Weekly history update completed")
        } catch (e: Exception) {
            Log.e("StepsOverviewActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error initializing steps overview: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun updateWeeklyHistory() {
        Log.d("StepsOverviewActivity", "updateWeeklyHistory started")
        try {
            // Get weekly steps data
            Log.d("StepsOverviewActivity", "Getting weekly steps data")
            val weeklyData = UserPreferences.getWeeklySteps(this)
            Log.d("StepsOverviewActivity", "Got weekly data with ${weeklyData.size} days")
            
            if (weeklyData.isEmpty()) {
                Log.w("StepsOverviewActivity", "No weekly data available")
                Toast.makeText(this, "No step data available yet", Toast.LENGTH_SHORT).show()
                return
            }

            // Calculate average
            Log.d("StepsOverviewActivity", "Calculating average")
            val average = weeklyData.map { it.steps }.average().toInt()
            Log.d("StepsOverviewActivity", "Average calculated: $average")
            
            try {
                binding.averageStepsText.text = "Average daily steps: $average"
                Log.d("StepsOverviewActivity", "Average text updated")
            } catch (e: Exception) {
                Log.e("StepsOverviewActivity", "Error updating average text", e)
                throw e
            }

            // Update each day's display
            if (weeklyData.size >= 7) {
                Log.d("StepsOverviewActivity", "Updating day displays")
                try {
                    updateDayDisplay(binding.day1Label, binding.day1Progress, binding.day1Steps, weeklyData[0])
                    updateDayDisplay(binding.day2Label, binding.day2Progress, binding.day2Steps, weeklyData[1])
                    updateDayDisplay(binding.day3Label, binding.day3Progress, binding.day3Steps, weeklyData[2])
                    updateDayDisplay(binding.day4Label, binding.day4Progress, binding.day4Steps, weeklyData[3])
                    updateDayDisplay(binding.day5Label, binding.day5Progress, binding.day5Steps, weeklyData[4])
                    updateDayDisplay(binding.day6Label, binding.day6Progress, binding.day6Steps, weeklyData[5])
                    updateDayDisplay(binding.day7Label, binding.day7Progress, binding.day7Steps, weeklyData[6])
                    Log.d("StepsOverviewActivity", "All day displays updated successfully")
                } catch (e: Exception) {
                    Log.e("StepsOverviewActivity", "Error updating day displays", e)
                    throw e
                }
            } else {
                Log.w("StepsOverviewActivity", "Incomplete weekly data: ${weeklyData.size} days")
                Toast.makeText(this, "Incomplete weekly data available", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("StepsOverviewActivity", "Error updating weekly history", e)
            Toast.makeText(this, "Error loading step history: ${e.message}", Toast.LENGTH_LONG).show()
            throw e // Re-throw to see the full stack trace
        }
    }

    private fun updateDayDisplay(
        labelView: android.widget.TextView,
        progressBar: android.widget.ProgressBar,
        stepsView: android.widget.TextView,
        dailyData: UserPreferences.DailyStepsData
    ) {
        try {
            Log.d("StepsOverviewActivity", "Updating display for ${dailyData.day}")
            labelView.text = dailyData.day
            progressBar.max = dailyData.target
            progressBar.progress = dailyData.steps
            stepsView.text = dailyData.steps.toString()
            Log.d("StepsOverviewActivity", "Display updated for ${dailyData.day}")
        } catch (e: Exception) {
            Log.e("StepsOverviewActivity", "Error updating day display for ${dailyData.day}", e)
            throw e
        }
    }

    private data class DailySteps(
        val day: String,
        val steps: Int,
        val target: Int
    )
}