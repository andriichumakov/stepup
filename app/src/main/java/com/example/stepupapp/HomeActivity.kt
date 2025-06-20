package com.example.stepupapp

import android.os.Bundle
import android.widget.Toast
import com.example.stepupapp.databinding.ActivityHomeBinding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.stepupapp.managers.*

class HomeActivity : BaseActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // Manager instances following dependency injection pattern
    private lateinit var stepTrackingManager: StepTrackingManager
    private lateinit var weatherWidgetManager: WeatherWidgetManager
    private lateinit var memoryWidgetManager: MemoryWidgetManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var notificationManager: AppNotificationManager
    private lateinit var quoteWidgetManager: QuoteWidgetManager
    private lateinit var backgroundSyncManager: BackgroundSyncManager
    private lateinit var homeNavigationManager: HomeNavigationManager
    private lateinit var actionBarGreetingManager: ActionBarGreetingManager
    private lateinit var actionBarProfileManager: ActionBarProfileManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers first
        initializeManagers()
        
        // Register permission launcher early
        permissionManager.initialize()
        
        // Initialize UI components
        initializeUI()
        
        // Check permissions and start services
        checkPermissionsAndSetup()
    }

    override fun onResume() {
        super.onResume()
        stepTrackingManager.updateStepTarget()
        actionBarGreetingManager.updateGreeting()
        actionBarProfileManager.updateProfilePicture()
        
        CoroutineScope(Dispatchers.IO).launch {
            memoryWidgetManager.checkAndNotifyNewMemory()
            memoryWidgetManager.updateMemoriesWidget()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stepTrackingManager.cleanup()
    }

    private fun initializeManagers() {
        stepTrackingManager = StepTrackingManager(this, binding)
        weatherWidgetManager = WeatherWidgetManager(this, binding)
        memoryWidgetManager = MemoryWidgetManager(this, binding)
        permissionManager = PermissionManager(this)
        notificationManager = AppNotificationManager(this)
        quoteWidgetManager = QuoteWidgetManager(binding)
        backgroundSyncManager = BackgroundSyncManager(this)
        homeNavigationManager = HomeNavigationManager(this, binding)
        actionBarGreetingManager = ActionBarGreetingManager(this)
        actionBarProfileManager = ActionBarProfileManager(this)
    }

    private fun initializeUI() {
        setupSwipeRefresh()
        initializeWidgets()
        notificationManager.createNotificationChannel()
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refreshAllData()
        }
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_green,
            R.color.dark_green,
            R.color.light_yellow
        )
    }

    private fun initializeWidgets() {
        // Initialize step tracking
        stepTrackingManager.initialize(createStepUpdateCallback())
        
        // Initialize weather widget
        weatherWidgetManager.initialize(createWeatherUpdateCallback())
        
        // Initialize memory widget
        memoryWidgetManager.initialize(createMemoryUpdateCallback())
        
        // Initialize quote widget
        quoteWidgetManager.initialize()
        
        // Initialize navigation
        homeNavigationManager.initialize()
        
        // Initialize action bar components
        actionBarGreetingManager.updateGreeting()
        actionBarProfileManager.updateProfilePicture()
        
        // Fetch initial data
        CoroutineScope(Dispatchers.Main).launch {
            weatherWidgetManager.fetchWeather()
            CoroutineScope(Dispatchers.IO).launch {
                memoryWidgetManager.updateMemoriesWidget()
            }
        }
    }

    private fun checkPermissionsAndSetup() {
        permissionManager.checkAndRequestPermissions(createPermissionCallback())
    }

    private fun refreshAllData() {
        try {
            // Refresh step count
            stepTrackingManager.refreshStepCount()
            
            // Refresh quote
            quoteWidgetManager.updateQuote()
            
            // Refresh weather and memory data
            CoroutineScope(Dispatchers.Main).launch {
                weatherWidgetManager.fetchWeather()
                CoroutineScope(Dispatchers.IO).launch {
                    memoryWidgetManager.updateMemoriesWidget()
                    backgroundSyncManager.performBackgroundSync()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error refreshing data", Toast.LENGTH_SHORT).show()
        } finally {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    // Callback implementations following Interface Segregation Principle
    private fun createStepUpdateCallback() = object : StepTrackingManager.StepUpdateCallback {
        override fun onStepDataUpdated(steps: Int, distance: Double, calories: Int) {
            // Additional handling if needed
        }

        override fun onStepTrackingError(error: String) {
            Toast.makeText(this@HomeActivity, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createWeatherUpdateCallback() = object : WeatherWidgetManager.WeatherUpdateCallback {
        override fun onWeatherUpdated(weatherInfo: WeatherManager.WeatherInfo) {
            // Additional handling if needed
        }

        override fun onWeatherError(error: String) {
            // Keep default weather display on error
        }
    }

    private fun createMemoryUpdateCallback() = object : MemoryWidgetManager.MemoryUpdateCallback {
        override fun onNewMemoryDetected(place: Place) {
            notificationManager.showInAppSnackbar(binding.root, place) { memory ->
                memoryWidgetManager.openMemoryActivityWithHighlight(memory.id.toLong())
            }
            notificationManager.sendMemoryNotification(place)
        }

        override fun onMemoryWidgetUpdated() {
            // Additional handling if needed
        }
    }

    private fun createPermissionCallback() = object : PermissionManager.PermissionCallback {
        override fun onPermissionsGranted() {
            stepTrackingManager.startStepCounterService()
        }

        override fun onPermissionsDenied() {
            Toast.makeText(this@HomeActivity, "Permissions are required for full functionality", Toast.LENGTH_LONG).show()
        }

        override fun onPermissionsError(error: String) {
            Toast.makeText(this@HomeActivity, error, Toast.LENGTH_SHORT).show()
        }
    }

    

}
