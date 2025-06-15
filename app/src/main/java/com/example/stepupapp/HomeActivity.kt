package com.example.stepupapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.stepupapp.databinding.ActivityHomeBinding

class HomeActivity : BaseActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var target: Int = 6000 // Will be updated in onCreate
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val stepUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("HomeActivity", "Local broadcast received with action: ${intent?.action}")
            if (intent?.action == "LOCAL_STEP_COUNT_UPDATE") {
                val steps = intent.getIntExtra("steps", 0)
                val distance = intent.getDoubleExtra("distance", 0.0)
                val calories = intent.getIntExtra("calories", 0)

                android.util.Log.d("HomeActivity", "Received update - Steps: $steps, Distance: $distance, Calories: $calories")
                runOnUiThread {
                    updateUI(steps, distance, calories)
                    android.util.Log.d("HomeActivity", "UI updated with new values")
                }
            }
        }
    }

    private fun updateQuote() {
        val quote = QuoteManager.getRandomQuote()
        binding.quoteText.text = quote.text
        binding.quoteAuthor.text = "— ${quote.author}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // Get current target and set up progress bar
        target = UserPreferences.getStepTarget(this)
        binding.stepProgressBar.max = target
        updateTargetText()

        // Get current step count from preferences and update UI
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val currentSteps = UserPreferences.getDailySteps(this, java.util.Date())
        val currentDistance = currentSteps / 1312.33595801 // Convert steps to kilometers
        val currentCalories = (currentSteps * 0.04).toInt() // Convert steps to calories
        updateUI(currentSteps, currentDistance, currentCalories)

        // Set up navigation buttons
        binding.imageButton3.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.imageButton4.setOnClickListener {
            val intent = Intent(this, ExploreActivity::class.java)
            startActivity(intent)
        }

        binding.imageButtonMemory.setOnClickListener {
            val intent = Intent(this, MemoryActivity::class.java)
            startActivity(intent)
        }

        // Set up memory card click
        binding.memoriesCard.setOnClickListener {
            val intent = Intent(this, MemoryActivity::class.java)
            startActivity(intent)
        }

        // Set up weather widget click
        binding.weatherCard.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.com/weather/today"))
            startActivity(intent)
        }

        // Set up history button
        binding.historyButton.setOnClickListener {
            try {
                Log.d("HomeActivity", "History button clicked, starting StepsOverviewActivity")
                val intent = Intent(this, StepsOverviewActivity::class.java)
                startActivity(intent)
                Log.d("HomeActivity", "StepsOverviewActivity started successfully")
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error starting StepsOverviewActivity", e)
                Toast.makeText(this, "Error opening steps overview", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up quote refresh button
        binding.refreshQuoteButton.setOnClickListener {
            updateQuote()
        }

        // Initialize UI with zeros and a random quote
        updateQuote()

        // Register for local broadcasts
        val filter = IntentFilter("LOCAL_STEP_COUNT_UPDATE")
        localBroadcastManager.registerReceiver(stepUpdateReceiver, filter)
        android.util.Log.d("HomeActivity", "Registered for local broadcasts")

        // Check permissions and start service
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        // Update target in case it was changed in settings
        target = UserPreferences.getStepTarget(this)
        binding.stepProgressBar.max = target
        updateTargetText()

        // Get current step count from preferences and update UI
        val currentSteps = UserPreferences.getDailySteps(this, java.util.Date())
        val currentDistance = currentSteps / 1312.33595801 // Convert steps to kilometers
        val currentCalories = (currentSteps * 0.04).toInt() // Convert steps to calories
        updateUI(currentSteps, currentDistance, currentCalories)
    }

    private fun updateTargetText() {
        binding.targetText.text = "Target: $target"
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
                Manifest.permission.ACTIVITY_RECOGNITION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            // Check if we should show explanation
            val shouldShowRationale = permissionsToRequest.any {
                ActivityCompat.shouldShowRequestPermissionRationale(this, it)
            }

            if (shouldShowRationale) {
                // Show explanation dialog
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("This app needs permission to count your steps and show notifications. Without these permissions, the step counter won't work.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        requestPermissionLauncher.launch(permissionsToRequest)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        Toast.makeText(this, "Permissions are required for step counting", Toast.LENGTH_LONG).show()
                        updateUI(0, 0.0, 0)
                    }
                    .create()
                    .show()
            } else {
                // Request permission directly
                requestPermissionLauncher.launch(permissionsToRequest)
            }
        } else {
            // Permissions already granted, proceed with setup
            setupStepCounter()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            setupStepCounter()
        } else {
            // Show dialog with option to open settings
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Step counting requires permission to track your activity. Please grant the permission in Settings.")
                .setPositiveButton("Open Settings") { _, _ ->
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(this, "Permissions are required for step counting", Toast.LENGTH_LONG).show()
                    updateUI(0, 0.0, 0)
                }
                .create()
                .show()
        }
    }

    private fun setupStepCounter() {
        try {
            android.util.Log.d("HomeActivity", "Setting up step counter")
            startStepCounterService()
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "Error setting up step counter", e)
            updateUI(0, 0.0, 0)
        }
    }

    private fun startStepCounterService() {
        try {
            android.util.Log.d("HomeActivity", "Starting step counter service")
            val serviceIntent = Intent(this, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            android.util.Log.d("HomeActivity", "Step counter service started successfully")
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "Error starting step counter service", e)
            updateUI(0, 0.0, 0)
        }
    }

    private fun updateUI(steps: Int, distance: Double, calories: Int) {
        try {
            android.util.Log.d("HomeActivity", "Updating UI - Steps: $steps, Distance: $distance, Calories: $calories")
            binding.stepCountText.text = "$steps steps"
            binding.stepProgressBar.progress = steps
            binding.distanceText.text = String.format("%.2f km", distance)
            binding.caloriesText.text = "$calories Cal"
            android.util.Log.d("HomeActivity", "UI update complete")
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "Error updating UI", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            localBroadcastManager.unregisterReceiver(stepUpdateReceiver)
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "Error unregistering receiver", e)
        }
    }
}
