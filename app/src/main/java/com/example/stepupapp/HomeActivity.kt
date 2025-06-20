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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.widget.ImageView

import com.example.stepupapp.services.ProfileService

class HomeActivity : BaseActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var target: Int = 6000 // Will be updated in onCreate
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var actionBarGreetingManager: ActionBarGreetingManager
    private lateinit var actionBarProfileManager: ActionBarProfileManager

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

    private fun refreshStepCount() {
        try {
            android.util.Log.d("HomeActivity", "Refreshing step count")
            // Get current step count from preferences and update UI
            val currentSteps = UserPreferences.getDailySteps(this, java.util.Date())
            val currentDistance = currentSteps / 1312.33595801 // Convert steps to kilometers
            val currentCalories = (currentSteps * 0.04).toInt() // Convert steps to calories
            updateUI(currentSteps, currentDistance, currentCalories)
            
            // Also refresh the quote and weather while we're at it
            updateQuote()
            fetchWeather()
            
            android.util.Log.d("HomeActivity", "Step count refreshed successfully")
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "Error refreshing step count", e)
            Toast.makeText(this, "Error refreshing step count", Toast.LENGTH_SHORT).show()
        } finally {
            // Stop the refresh animation if it's running
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun fetchWeather() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("HomeActivity", "Fetching weather data...")
                val weatherInfo = withContext(Dispatchers.IO) {
                    WeatherManager.getCurrentWeather(this@HomeActivity)
                }
                
                if (weatherInfo != null) {
                    updateWeatherUI(weatherInfo)
                    Log.d("HomeActivity", "Weather updated successfully")
                } else {
                    Log.w("HomeActivity", "Failed to fetch weather data")
                    // Keep the default weather display
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error fetching weather", e)
                // Keep the default weather display on error
            }
        }
    }

    private fun updateWeatherUI(weatherInfo: WeatherManager.WeatherInfo) {
        try {
            // Update temperature
            binding.weatherTemp.text = "${weatherInfo.temperature.toInt()}°C"
            
            // Update weather icon
            binding.weatherIcon.setImageResource(weatherInfo.weatherIcon)
            
            // Update weather message
            val weatherMessage = WeatherManager.getWeatherMessage(weatherInfo.temperature, weatherInfo.weatherCode)
            binding.weatherMessage.text = weatherMessage
            
            Log.d("HomeActivity", "Weather UI updated: ${weatherInfo.temperature}°C, ${weatherInfo.weatherDescription}")
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error updating weather UI", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refreshStepCount()
        }
        // Set the refresh colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_green,
            R.color.dark_green,
            R.color.light_yellow
        )

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

        // Initialize action bar managers
        actionBarGreetingManager = ActionBarGreetingManager(this)
        actionBarGreetingManager.updateGreeting()
        
        actionBarProfileManager = ActionBarProfileManager(this)
        actionBarProfileManager.updateProfilePicture()

        // Initialize UI with zeros and a random quote
        updateQuote()
        
        // Fetch weather data
        fetchWeather()

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

        // Add location permissions for weather functionality
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

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
                    .setMessage("This app needs permission to count your steps, show notifications, and access location for weather information. Without these permissions, some features won't work.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        requestPermissionLauncher.launch(permissionsToRequest)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        Toast.makeText(this, "Permissions are required for full functionality", Toast.LENGTH_LONG).show()
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
                .setMessage("This app requires permissions to track your activity and access location for weather information. Please grant the permissions in Settings.")
                .setPositiveButton("Open Settings") { _, _ ->
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(this, "Permissions are required for full functionality", Toast.LENGTH_LONG).show()
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

    private fun tryBackgroundSync() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var syncedSomething = false
                
                // Sync pending interests
                if (UserPreferences.doInterestsNeedSync(this@HomeActivity)) {
                    android.util.Log.d("HomeActivity", "Attempting to sync pending interests...")
                    val interestsSuccess = ProfileService.syncPendingInterests(this@HomeActivity)
                    
                    if (interestsSuccess) {
                        android.util.Log.d("HomeActivity", "Successfully synced pending interests")
                        syncedSomething = true
                    } else {
                        android.util.Log.w("HomeActivity", "Failed to sync pending interests")
                    }
                }
                
                // Sync pending profile picture
                if (UserPreferences.doesProfileImageNeedSync(this@HomeActivity)) {
                    android.util.Log.d("HomeActivity", "Attempting to sync pending profile picture...")
                    val pictureSuccess = ProfileService.syncPendingProfilePicture(this@HomeActivity)
                    
                    if (pictureSuccess) {
                        android.util.Log.d("HomeActivity", "Successfully synced pending profile picture")
                        syncedSomething = true
                    } else {
                        android.util.Log.w("HomeActivity", "Failed to sync pending profile picture")
                    }
                }

                // Sync pending nickname
                if (UserPreferences.doesNicknameNeedSync(this@HomeActivity)) {
                    android.util.Log.d("HomeActivity", "Attempting to sync pending nickname...")
                    val nicknameSuccess = ProfileService.syncPendingNickname(this@HomeActivity)
                    
                    if (nicknameSuccess) {
                        android.util.Log.d("HomeActivity", "Successfully synced pending nickname")
                        syncedSomething = true
                    } else {
                        android.util.Log.w("HomeActivity", "Failed to sync pending nickname")
                    }
                }

                // Sync pending name
                if (UserPreferences.doesNameNeedSync(this@HomeActivity)) {
                    android.util.Log.d("HomeActivity", "Attempting to sync pending name...")
                    val nameSuccess = ProfileService.syncPendingName(this@HomeActivity)
                    
                    if (nameSuccess) {
                        android.util.Log.d("HomeActivity", "Successfully synced pending name")
                        syncedSomething = true
                    } else {
                        android.util.Log.w("HomeActivity", "Failed to sync pending name")
                    }
                }
                
                if (!syncedSomething) {
                    android.util.Log.d("HomeActivity", "No data needs syncing")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeActivity", "Error during background sync: ${e.message}", e)
            }
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
