package com.example.stepupapp

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.stepupapp.databinding.ActivityStepsOverviewBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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

            // Set up ViewPager and TabLayout
            setupTabs()
            
            // Update average steps
            updateAverageSteps()
            
            // Set up weekly progress graph using Compose
            setupWeeklyProgressGraph()
            
        } catch (e: Exception) {
            Log.e("StepsOverviewActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error initializing steps overview: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupTabs() {
        // Set up ViewPager adapter
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> WeeklyHistoryTabFragment.newInstance(WeeklyHistoryTabFragment.TabType.STEPS)
                    1 -> WeeklyHistoryTabFragment.newInstance(WeeklyHistoryTabFragment.TabType.CALORIES)
                    2 -> WeeklyHistoryTabFragment.newInstance(WeeklyHistoryTabFragment.TabType.DISTANCE)
                    else -> throw IllegalStateException("Invalid position $position")
                }
            }
        }

        // Connect TabLayout with ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Steps"
                1 -> "Calories"
                2 -> "Distance"
                else -> throw IllegalStateException("Invalid position $position")
            }
        }.attach()
    }

    private fun updateAverageSteps() {
        try {
            val weeklyData = UserPreferences.getWeeklySteps(this)
            if (weeklyData.isNotEmpty()) {
                val average = weeklyData.map { it.steps }.average().toInt()
                binding.averageStepsText.text = "Average daily steps: $average"
            }
        } catch (e: Exception) {
            Log.e("StepsOverviewActivity", "Error updating average steps", e)
        }
    }

    private fun setupWeeklyProgressGraph() {
        try {
            val weeklyData = UserPreferences.getWeeklySteps(this)
            Log.d("StepsOverviewActivity", "Weekly data size: ${weeklyData.size}")
            weeklyData.forEach { data ->
                Log.d("StepsOverviewActivity", "Day: ${data.day}, Steps: ${data.steps}, Target: ${data.target}")
            }

            // Create a Compose view for the chart
            binding.weeklyProgressGraph.weeklyProgressChart.setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF1F823A) // Match the card background color
                    ) {
                        WeeklyProgressChart(weeklyData)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StepsOverviewActivity", "Error setting up weekly progress graph", e)
        }
    }

    @Composable
    fun WeeklyProgressChart(weeklyData: List<UserPreferences.DailyStepsData>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Calculate days where target was met
            val daysMetTarget = weeklyData.count { it.steps >= it.target }
            val totalDays = weeklyData.size
            val targetMetPercentage = if (totalDays > 0) (daysMetTarget.toFloat() / totalDays.toFloat()) * 100 else 0f

            // Calculate current streak
            val currentStreak = calculateCurrentStreak(weeklyData)
            val streakMessage = getStreakMessage(currentStreak)

            Log.d("StepsOverviewActivity", "Days met target: $daysMetTarget, Total days: $totalDays, Percentage: $targetMetPercentage")

            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .padding(4.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width.coerceAtMost(size.height) / 2 * 0.85f
                
                // Draw background circle (unfilled portion)
                drawArc(
                    color = Color(0x80FFFFFF),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = true,
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )

                // Draw filled portion (days target was met)
                if (targetMetPercentage > 0) {
                    drawArc(
                        color = Color(0xFF4CAF50),
                        startAngle = -90f,
                        sweepAngle = (targetMetPercentage / 100f) * 360f,
                        useCenter = true,
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )
                }

                // Draw percentage text
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 80f
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }

                    // Draw percentage in the center
                    canvas.nativeCanvas.drawText(
                        "${targetMetPercentage.toInt()}%",
                        center.x,
                        center.y + paint.textSize / 4,
                        paint
                    )

                    // Draw days met target below
                    paint.textSize = 44f
                    canvas.nativeCanvas.drawText(
                        "$daysMetTarget / $totalDays days",
                        center.x,
                        center.y + paint.textSize * 2.2f,
                        paint
                    )
                }
            }

            // Add streak message below the chart
            Text(
                text = streakMessage,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    private fun calculateCurrentStreak(weeklyData: List<UserPreferences.DailyStepsData>): Int {
        var streak = 0
        // Sort data by date in descending order (most recent first)
        val sortedData = weeklyData.sortedByDescending { it.date }
        
        // Check consecutive days from most recent
        for (data in sortedData) {
            if (data.steps >= data.target) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun getStreakMessage(streak: Int): String {
        return when (streak) {
            0 -> "No streak yet"
            1 -> "1 day streak, keep going!"
            2 -> "2 day streak, keep it up!"
            3 -> "3 day streak, you're on fire!"
            4 -> "4 day streak, amazing!"
            5 -> "5 day streak, incredible!"
            6 -> "6 day streak, almost there!"
            7 -> "You've been active all week!"
            else -> "$streak day streak, outstanding!"
        }
    }
}