package com.example.stepupapp

import android.Manifest
import android.app.PendingIntent
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.widget.ImageView


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

        android.util.Log.d("HomeActivity", "onCreate called")

        // Initialize target from preferences
        target = UserPreferences.getStepTarget(this)
        updateTargetText()

        // Initialize LocalBroadcastManager
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // Set up the progress bar with dynamic target
        binding.stepProgressBar.max = target

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
            val currentSteps = binding.stepCountText.text.toString().split(" ")[0].toIntOrNull() ?: 0
            intent.putExtra("currentSteps", currentSteps)
            startActivity(intent)
        }

        // Set up memory card click
        binding.memoriesCard.setOnClickListener {
            val intent = Intent(this, MemoryActivity::class.java)
            val currentSteps = binding.stepCountText.text.toString().split(" ")[0].toIntOrNull() ?: 0
            intent.putExtra("currentSteps", currentSteps)
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
        updateUI(0, 0.0, 0)
        updateQuote()

        // Register for local broadcasts
        val filter = IntentFilter("LOCAL_STEP_COUNT_UPDATE")
        localBroadcastManager.registerReceiver(stepUpdateReceiver, filter)
        android.util.Log.d("HomeActivity", "Registered for local broadcasts")

        // Check permissions and start service
        checkAndRequestPermissions()

        createNotificationChannel()

        // Update the memories widget with real data
        updateMemoriesWidget()
    }

    override fun onResume() {
        super.onResume()
        // Update target in case it was changed in settings
        target = UserPreferences.getStepTarget(this)
        binding.stepProgressBar.max = target
        updateTargetText()

        checkAndNotifyNewMemory()
        // Update the memories widget with real data
        updateMemoriesWidget()
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Memory Notifications"
            val descriptionText = "Notifications for newly added memories"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel("memory_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkAndNotifyNewMemory() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = PlaceDatabase.getDatabase(applicationContext)
            val latestMemory = db.placeDao().getLatestPlace()
            latestMemory?.let { place ->
                val lastNotifiedId = UserPreferences.getLastMemoryId(applicationContext)
                if (place.id != lastNotifiedId) {
                    UserPreferences.setLastMemoryId(applicationContext, place.id)
                    withContext(Dispatchers.Main) {
                        showInAppSnackbar(place)
                        sendMemoryNotification(place)
                    }
                }
            }
        }
    }


    private fun sendMemoryNotification(place: Place) {
        val intent = Intent(this, MemoryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("highlightMemoryId", place.id) // optional
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "memory_channel")
            .setSmallIcon(R.drawable.stepup_logo_bunny) // Your own memory icon
            .setContentTitle("New Memory Added")
            .setContentText("You added ${place.name} on ${place.date_saved}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(1001, builder.build())
    }

    private fun showInAppSnackbar(place: Place) {
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "New memory added: ${place.name} (${place.date_saved})",
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        )
        snackbar.setAction("View") {
            val intent = Intent(this, MemoryActivity::class.java).apply {
                putExtra("highlightMemoryId", place.id) // optional
            }
            startActivity(intent)
        }
        snackbar.show()
    }

    private fun updateMemoriesWidget() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = PlaceDatabase.getDatabase(applicationContext)
            val latestMemory = db.placeDao().getLatestPlace()
            withContext(Dispatchers.Main) {
                if (latestMemory == null) {
                    // No memories yet: show a message, keep the rest of the widget as is
                    binding.memoryText.text = "No memories yet. Add your first memory!"
                    binding.memoryImage.setImageResource(R.drawable.memory_zoo) // fallback/default image
                    binding.memoryDate.text = ""
                    binding.memorySteps.text = ""
                    // Set all stars to empty or faded (optional)
                    setMemoryStars(0f)
                } else {
                    // Show real memory data
                    try {
                        val uri = Uri.parse(latestMemory.imageUri)
                        binding.memoryImage.setImageURI(uri)
                    } catch (e: Exception) {
                        binding.memoryImage.setImageResource(R.drawable.memory_zoo)
                    }
                    binding.memoryDate.text = latestMemory.date_saved
                    binding.memoryText.text = latestMemory.description.ifBlank { latestMemory.name }
                    binding.memorySteps.text = latestMemory.steps_taken + " steps"
                    setMemoryStars(latestMemory.rating)
                }
            }
        }
    }

    private fun setMemoryStars(rating: Float) {
        // The widget has 5 ImageViews for stars, all are always present in the layout
        val starIds = listOf(
            R.id.memoryStar1,
            R.id.memoryStar2,
            R.id.memoryStar3,
            R.id.memoryStar4,
            R.id.memoryStar5
        )
        for (i in 0 until 5) {
            val starView = findViewById<ImageView>(starIds[i])
            if (rating >= i + 1) {
                starView.setImageResource(R.drawable.ic_star)
                starView.setColorFilter(0xFFFFD700.toInt()) // Gold
            } else if (rating > i && rating < i + 1) {
                // Half star logic if you want (not implemented, just use full/empty)
                starView.setImageResource(R.drawable.ic_star)
                starView.setColorFilter(0x80FFD700.toInt()) // Faded gold
            } else {
                starView.setImageResource(R.drawable.ic_star)
                starView.setColorFilter(0x33FFD700) // More faded
            }
        }
    }

}
