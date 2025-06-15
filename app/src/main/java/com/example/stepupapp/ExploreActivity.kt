package com.example.stepupapp

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.api.OpenTripMapResponse
import com.example.stepupapp.api.PlaceDetails
import com.example.stepupapp.databinding.ExplorePageBinding
import com.example.stepupapp.filter.FilterManager
import com.example.stepupapp.presentation.explore.ExplorePresenter
import com.example.stepupapp.presentation.explore.ExploreUIManager
import com.example.stepupapp.presentation.explore.MapController
import com.example.stepupapp.UserPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ExploreActivity : AppCompatActivity(),
    ExplorePresenter.ExploreView,
    MapController.MapControllerListener,
    ExploreUIManager.UIManagerListener {

    private lateinit var binding: ExplorePageBinding
    private lateinit var presenter: ExplorePresenter
    private lateinit var locationManager: LocationManager
    private lateinit var mapController: MapController
    private lateinit var uiManager: ExploreUIManager
    private lateinit var actionBarLocationManager: ActionBarLocationManager
    
    private val TAG = "ExploreActivity"
    
    // State
    private var isMapViewActive = false
    private var currentLatitude = 52.788040
    private var currentLongitude = 6.893176
    private var currentLocationName = "Emmen, Netherlands"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExplorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupComponents()
        
        // Set up home button navigation
        binding.homeButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        presenter.initialize()
        
        // Show default loading state
        uiManager.updateLocationUI("Getting your location...", true)

        // Start location updates
        if (locationManager.checkLocationPermission()) {
            locationManager.startLocationUpdates()
        } else {
            locationManager.requestLocationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        mapController.onResume()
        presenter.onResume()
        
        // Resume location updates if needed
        if (locationManager.checkLocationPermission() && !locationManager.isUpdatesActive()) {
            locationManager.startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        mapController.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
        locationManager.stopLocationUpdates()
        actionBarLocationManager.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LocationManager.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                locationManager.startLocationUpdates()
            } else {
                uiManager.updateLocationUI(currentLocationName, false)
                presenter.loadPlaces(currentLatitude, currentLongitude)
            }
        }
    }

    // ExplorePresenter.ExploreView Implementation
    override fun showLoadingState() {
        uiManager.showLoadingState()
    }

    override fun hideLoadingState() {
        uiManager.hideLoadingState()
    }

    override fun showMiniLoadingState() {
        uiManager.showMiniLoadingState()
    }

    override fun showPlaces(places: List<OpenTripMapResponse>, details: Map<String, PlaceDetails?>) {
        if (isMapViewActive) {
            mapController.updateMapMarkers(places)
        } else {
            uiManager.showPlaces(places, details)
        }
    }

    override fun showEmptyState(title: String, message: String) {
        uiManager.showEmptyState(title, message)
    }

    override fun showErrorState(title: String, message: String) {
        uiManager.showErrorState(title, message)
    }

    override fun updateUserInterestsDisplay(text: String) {
        uiManager.updateUserInterestsDisplay(text)
    }

    override fun updateFilterModeUI(isManualMode: Boolean) {
        uiManager.updateFilterModeUI(isManualMode)
    }

    override fun updateClearButtonVisibility(visible: Boolean) {
        uiManager.updateClearButtonVisibility(visible)
    }

    override fun showToast(message: String) {
        uiManager.showToast(message)
    }

    override fun getUserInterests(): Set<String> {
        return UserPreferences.getUserInterests(this)
    }

    // Location update handling
    private fun handleLocationUpdate(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude
        
        // Get location name using LocationManager
        currentLocationName = locationManager.getLocationName(currentLatitude, currentLongitude)

        uiManager.updateLocationUI(currentLocationName, false)
        actionBarLocationManager.updateLocationFromCoordinates(currentLatitude, currentLongitude)
        
        // Update map location
        mapController.updateUserLocation(currentLatitude, currentLongitude)
        
        // Load places for this location
        presenter.loadPlaces(currentLatitude, currentLongitude)
        
        // Stop updates once we have it
        locationManager.stopLocationUpdates()
    }

    // MapController.MapControllerListener Implementation
    override fun onRouteCalculated(steps: Int, placeName: String) {
        uiManager.showClearRouteButton(true)
        uiManager.showLongToast("Route to $placeName • $steps steps")
    }

    override fun onRouteCleared() {
        uiManager.showClearRouteButton(false)
    }

    override fun onMapError(message: String) {
        uiManager.showToast("Map error: $message")
    }

    // ExploreUIManager.UIManagerListener Implementation
    override fun onCategorySelected(category: String) {
        presenter.onCategorySelected(category)
    }

    override fun onSubcategoryChanged(subcategory: String) {
        presenter.onSubcategoryChanged(subcategory)
    }

    override fun onToggleFilterClicked() {
        presenter.toggleFilterMode()
    }

    override fun onClearAllFiltersClicked() {
        presenter.clearAllFilters()
        uiManager.clearSubcategorySearch()
    }

    override fun onMapToggleClicked() {
        isMapViewActive = !isMapViewActive
        uiManager.updateMapUI(isMapViewActive)
        
        if (isMapViewActive) {
            // Update map with current places
            val filteredPlaces = presenter.getFilteredPlaces()
            mapController.updateMapMarkers(filteredPlaces)
        } else {
            // Clear any active route when switching to list view
            mapController.clearRoute()
        }
        
        uiManager.showToast(if (isMapViewActive) "Map view" else "List view")
    }

    override fun onCenterLocationClicked() {
        mapController.centerOnUser()
    }

    override fun onClearRouteClicked() {
        mapController.clearRoute()
        uiManager.showToast("Route cleared")
    }

    override fun onLocationCardClicked() {
        if (locationManager.checkLocationPermission()) {
            uiManager.updateLocationUI("Refreshing location...", true)
            locationManager.startLocationUpdates()
            uiManager.showToast("Refreshing nearby places...")
        } else {
            locationManager.requestLocationPermission()
        }
    }

    override fun onInterestsCardClicked() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onRetryClicked() {
        presenter.loadPlaces(currentLatitude, currentLongitude)
    }

    override fun onExpandSearchClicked() {
        uiManager.showToast("Expanding search radius...")
        presenter.loadPlaces(currentLatitude, currentLongitude)
    }

    override fun onClearFiltersFromEmptyStateClicked() {
        presenter.clearAllFilters()
        uiManager.showToast("Filters cleared")
    }

    private fun initializeComponents() {
        actionBarLocationManager = ActionBarLocationManager(this)
        
        locationManager = LocationManager(this) { location ->
            handleLocationUpdate(location)
        }
        
        mapController = MapController(this, lifecycleScope)
        
        uiManager = ExploreUIManager(this, binding)
        
        val placeRepository = createPlaceRepository()
        val filterManager = FilterManager()
        
        presenter = ExplorePresenter(placeRepository, filterManager, lifecycleScope)
    }

    private fun setupComponents() {
        presenter.attachView(this)
        mapController.setListener(this)
        uiManager.setListener(this)
        
        actionBarLocationManager.setupActionBarLocation()
        
        mapController.initializeMap(binding.osmMapView)
        
        uiManager.setupUI()
    }

    private fun createPlaceRepository(): PlaceRepository {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.opentripmap.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val openTripMapService = retrofit.create(com.example.stepupapp.api.OpenTripMapService::class.java)
        return PlaceRepository(openTripMapService)
    }
} 