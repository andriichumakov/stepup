package com.example.stepupapp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.api.OpenTripMapResponse
import com.example.stepupapp.api.OpenTripMapService
import com.example.stepupapp.api.PlaceDetails
import com.example.stepupapp.databinding.ExplorePageBinding
import com.example.stepupapp.databinding.PlaceCardBinding
import com.example.stepupapp.filter.AdultContentFilter
import com.example.stepupapp.filter.FilterManager
import com.example.stepupapp.filter.InterestFilter
import com.example.stepupapp.filter.SubcategoryFilter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class ExploreActivity : AppCompatActivity() {
    private lateinit var binding: ExplorePageBinding
    private lateinit var locationManager: LocationManager
    private lateinit var placeRepository: PlaceRepository
    private lateinit var filterManager: FilterManager
    private val TAG = "ExploreActivity"

    private val STEP_LENGTH = 0.50 // Average step length in meters

    private var currentLatitude = 52.788040
    private var currentLongitude = 6.893176
    private var currentLocationName = "Emmen, Netherlands"

    private var allPlacesList = listOf<OpenTripMapResponse>()
    private var filteredPlacesList = listOf<OpenTripMapResponse>()
    private var currentSubcategory = ""
    private var userInterests = setOf<String>()
    private var originalUserInterests = setOf<String>()
    private var manualSelectedCategory = ""
    private var isManualFilterMode = false
    private var filterJob: kotlinx.coroutines.Job? = null

    // Map related properties
    private var isMapViewActive = false
    private lateinit var osmMapView: MapView
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var currentRouteOverlay: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExplorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load user interests
        userInterests = UserPreferences.getUserInterests(this)
        originalUserInterests = userInterests

        // Initialize managers
        initializeManagers()

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        // Initialize UI components
        setupCategorySpinner()
        setupSubcategorySearch()
        setupToggleButton()
        setupMapToggle()
        setupOSMMap()
        setupCenterLocationButton()
        displayUserInterests()

        // Set up home button to navigate back to home screen
        binding.homeButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Show default loading state
        binding.locationText.text = "Getting your location..."

        // Check for location permissions and start location updates
        if (locationManager.checkLocationPermission()) {
            startLocationUpdates()
        } else {
            locationManager.requestLocationPermission()
        }

        // Make location card clickable to refresh location
        binding.locationCard.setOnClickListener {
            if (locationManager.checkLocationPermission()) {
                binding.locationProgressBar.visibility = View.VISIBLE
                binding.locationText.text = "Refreshing location..."
                startLocationUpdates()
                Toast.makeText(this, "Refreshing nearby places...", Toast.LENGTH_SHORT).show()
            } else {
                locationManager.requestLocationPermission()
            }
        }
    }

    private fun initializeManagers() {
        // Initialize location manager
        locationManager = LocationManager(this) { location ->
            updateLocationUI(location)
        }

        // Initialize API service and repository
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

        val openTripMapService = retrofit.create(OpenTripMapService::class.java)
        placeRepository = PlaceRepository(openTripMapService)

        // Initialize filter manager
        filterManager = FilterManager()
    }

    private fun startLocationUpdates() {
        locationManager.startLocationUpdates()
        binding.locationText.text = "Getting your location..."
        binding.locationProgressBar.visibility = View.VISIBLE
    }

    private fun updateLocationUI(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude

        // Get location name using LocationManager
        currentLocationName = locationManager.getLocationName(currentLatitude, currentLongitude)

        // Update UI
        binding.locationText.text = currentLocationName

        // Load nearby places
        loadPlaces()

        // Stop updates once we have it
        locationManager.stopLocationUpdates()
        binding.locationProgressBar.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LocationManager.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Snackbar.make(binding.root, "Location permission is required to show nearby places", Snackbar.LENGTH_LONG)
                    .setAction("Settings") {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .show()

                binding.locationText.text = currentLocationName
                binding.locationProgressBar.visibility = View.GONE
                loadPlaces()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Resume OSM map
        if (::osmMapView.isInitialized) {
            osmMapView.onResume()
        }
        
        // Refresh user interests display and filter
        userInterests = UserPreferences.getUserInterests(this)
        originalUserInterests = userInterests
        displayUserInterests()
        
        // Refresh places with potentially updated interests
        if (allPlacesList.isNotEmpty()) {
            filterPlaces()
        }
        
        if (locationManager.checkLocationPermission() && !locationManager.isUpdatesActive()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        
        // Pause OSM map
        if (::osmMapView.isInitialized) {
            osmMapView.onPause()
        }
        
        locationManager.stopLocationUpdates()
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.place_categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.categorySpinner.adapter = adapter
        }

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position).toString()
                (view as? TextView)?.apply {
                    setTextColor(resources.getColor(android.R.color.black, theme))
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                }
                
                // Only use spinner selection in manual filter mode
                if (isManualFilterMode) {
                    // Store manual selection without overriding user interests
                    manualSelectedCategory = selectedCategory
                    // Clear subcategory search when category changes
                    binding.subcategorySearch.setText("")
                    currentSubcategory = ""
                    filterPlaces()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSubcategorySearch() {
        // Set up autocomplete with all subcategories
        val subcategories = resources.getStringArray(R.array.all_subcategories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, subcategories)
        binding.subcategorySearch.setAdapter(adapter)
        
        // Handle text changes for filtering
        binding.subcategorySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSubcategory = s.toString()
                filterPlaces()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Handle item selection from dropdown
        binding.subcategorySearch.setOnItemClickListener { _, _, _, _ ->
            // Filter will be triggered by text change
            binding.subcategorySearch.clearFocus()
        }
        
        // Set up clear button
        binding.clearSearchButton.setOnClickListener {
            binding.subcategorySearch.setText("")
            binding.subcategorySearch.clearFocus()
            currentSubcategory = ""
            filterPlaces()
        }
    }

    private fun setupToggleButton() {
        binding.toggleFilterButton.setOnClickListener {
            isManualFilterMode = !isManualFilterMode
            if (isManualFilterMode) {
                binding.categorySpinnerCard.visibility = View.VISIBLE
                binding.toggleFilterButton.text = "Use My Interests"
                Toast.makeText(this, "Manual filter mode enabled", Toast.LENGTH_SHORT).show()
            } else {
                binding.categorySpinnerCard.visibility = View.GONE
                binding.toggleFilterButton.text = "Manual Filter"
                // Restore original user interests when switching back
                userInterests = originalUserInterests
                manualSelectedCategory = ""
                Toast.makeText(this, "Showing places from your interests", Toast.LENGTH_SHORT).show()
            }
            displayUserInterests()
            filterPlaces()
        }
    }

    private fun loadPlaces() {
        // Cancel any ongoing filter operation
        filterJob?.cancel()
        
        // Show skeleton loading state
        showSkeletonLoading()
        allPlacesList = emptyList()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Making API call with coordinates: lat=$currentLatitude, lon=$currentLongitude")
                val places = placeRepository.searchPlaces(currentLatitude, currentLongitude)

                Log.d(TAG, "API response processed: ${places.size} places")
                
                places.forEachIndexed { index, place ->
                    Log.d(TAG, "Unique Place $index: name='${place.name}', xid='${place.xid}', kinds='${place.kinds}', dist=${place.dist}")
                }

                // Hide skeleton loading
                hideSkeletonLoading()

                if (places.isEmpty()) {
                    showEmptyState("No places found nearby", "Try adjusting your filters or location")
                    return@launch
                }

                allPlacesList = places
                filterPlaces() // Apply interest-based filtering immediately
            } catch (e: Exception) {
                Log.e(TAG, "Error loading places: ${e.message}", e)
                hideSkeletonLoading()
                showErrorState("Unable to load places", "Check your internet connection and try again")
            }
        }
    }
    
    private fun showSkeletonLoading() {
        binding.placesContainer.removeAllViews()
        
        // Add skeleton cards
        repeat(5) {
            val skeletonView = layoutInflater.inflate(R.layout.skeleton_place_card, binding.placesContainer, false)
            binding.placesContainer.addView(skeletonView)
        }
    }
    
    private fun hideSkeletonLoading() {
        // Remove skeleton cards (they'll be replaced with real content)
        binding.placesContainer.removeAllViews()
    }
    
    private fun showEmptyState(title: String, message: String) {
        binding.placesContainer.removeAllViews()
        
        val emptyStateView = layoutInflater.inflate(R.layout.empty_state_places, binding.placesContainer, false)
        // Configure empty state view with title and message
        emptyStateView.findViewById<TextView>(R.id.emptyTitle)?.text = title
        emptyStateView.findViewById<TextView>(R.id.emptyMessage)?.text = message
        
        // Set up button actions
        emptyStateView.findViewById<com.google.android.material.button.MaterialButton>(R.id.expandSearchButton)?.setOnClickListener {
            // Expand search radius - you could modify the API call to use a larger radius
            Toast.makeText(this, "Expanding search radius...", Toast.LENGTH_SHORT).show()
            loadPlaces() // Reload with potentially larger radius
        }
        
        emptyStateView.findViewById<com.google.android.material.button.MaterialButton>(R.id.clearFiltersButton)?.setOnClickListener {
            // Clear current filters and show all places
            clearAllFilters()
            Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
        }
        
        binding.placesContainer.addView(emptyStateView)
    }
    
    private fun clearAllFilters() {
        currentSubcategory = ""
        isManualFilterMode = false
        manualSelectedCategory = ""
        userInterests = originalUserInterests
        binding.subcategorySearch.setText("")
        binding.subcategorySearch.clearFocus()
        binding.categorySpinnerCard.visibility = View.GONE
        binding.toggleFilterButton.text = "Manual Filter"
        binding.toggleFilterButton.setBackgroundColor(getColor(R.color.dark_blue))
        displayUserInterests()
        filterPlaces()
    }
    
    private fun showErrorState(title: String, message: String) {
        binding.placesContainer.removeAllViews()
        
        val errorStateView = layoutInflater.inflate(R.layout.error_state_places, binding.placesContainer, false)
        // Configure error state view
        errorStateView.findViewById<TextView>(R.id.errorTitle)?.text = title
        errorStateView.findViewById<TextView>(R.id.errorMessage)?.text = message
        errorStateView.findViewById<com.google.android.material.button.MaterialButton>(R.id.retryButton)?.setOnClickListener {
            loadPlaces()
        }
        binding.placesContainer.addView(errorStateView)
    }

    private fun filterPlaces() {
        if (allPlacesList.isEmpty()) return
        
        // Cancel any ongoing filter operation
        filterJob?.cancel()
        
        // Show mini skeleton loading during filtering
        showMiniSkeletonLoading()

        // Setup filters using the FilterManager
        filterManager.clearFilters()
        
        // Always add adult content filter for family safety
        filterManager.addFilter(AdultContentFilter())
        
        // Add interest filter
        val currentInterests = if (isManualFilterMode && manualSelectedCategory.isNotEmpty()) {
            setOf(manualSelectedCategory)
        } else {
            userInterests
        }
        filterManager.addFilter(InterestFilter(currentInterests))
        
        // Add subcategory filter if specified
        if (currentSubcategory.isNotEmpty()) {
            filterManager.addFilter(SubcategoryFilter(currentSubcategory))
        }

        filterJob = lifecycleScope.launch {
            val filteredPlaces = filterManager.applyFilters(allPlacesList)

            // Hide skeleton loading
            hideSkeletonLoading()

            // Update filtered places list for map
            filteredPlacesList = filteredPlaces

            if (filteredPlaces.isEmpty()) {
                val message = if (currentSubcategory.isNotEmpty()) {
                    "No places found for: $currentSubcategory"
                } else if (currentInterests.isNotEmpty() && !currentInterests.contains("All")) {
                    "No places found for your interests: ${currentInterests.joinToString(", ")}"
                } else {
                    "No places found nearby"
                }
                showEmptyState("No matching places", message)
                return@launch
            }

            // Update map markers if map view is active
            if (isMapViewActive) {
                updateOSMMap()
            } else {
                // Create place cards for list view
                for (place in filteredPlaces) {
                    val details = placeRepository.getPlaceDetails(place.xid)
                    createPlaceCard(place, details)
                }
            }
        }
    }
    
    private fun showMiniSkeletonLoading() {
        binding.placesContainer.removeAllViews()
        
        // Add fewer skeleton cards for filtering (faster loading)
        repeat(3) {
            val skeletonView = layoutInflater.inflate(R.layout.skeleton_place_card, binding.placesContainer, false)
            binding.placesContainer.addView(skeletonView)
        }
    }

    private fun createPlaceCard(place: OpenTripMapResponse, details: PlaceDetails?) {
        val cardBinding = PlaceCardBinding.inflate(layoutInflater)

        val name = place.name.ifEmpty { "Unnamed Place" }
        val type = place.kinds.split(",").firstOrNull()?.replace("_", " ") ?: "Unknown"
        val rating = (2..9).random()
        val distanceInMeters = if (place.dist < 50) (place.dist * 1000).roundToInt() else place.dist.roundToInt()
        val steps = (distanceInMeters / STEP_LENGTH).roundToInt()
        val stepsText = if (steps >= 1000) "${steps / 1000}k steps away" else "$steps steps away"

        cardBinding.placeName.text = name
        cardBinding.placeType.text = type
        cardBinding.placeRating.text = "⭐ $rating/10"
        cardBinding.placeAddress.text = stepsText

        // Set click listener to navigate to LocationDetailsActivity
        cardBinding.root.setOnClickListener {
            Log.d(TAG, "Place card clicked: $name")
            try {
                val intent = Intent(this, LocationDetailsActivity::class.java).apply {
                    putExtra("name", name)
                    putExtra("type", type)
                    putExtra("description", details?.wikipedia_extracts?.text ?: "No description available for this location.")
                    putExtra("rating", "$rating/10")
                    putExtra("steps", stepsText)
                    putExtra("openingHours", details?.sources?.opening_hours ?: "Opening hours not available")
                    putExtra("latitude", place.point.lat)
                    putExtra("longitude", place.point.lon)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening location details: ${e.message}")
                Toast.makeText(this, "Error opening location details", Toast.LENGTH_SHORT).show()
            }
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 16)
        cardBinding.root.layoutParams = layoutParams

        binding.placesContainer.addView(cardBinding.root)
    }

    private fun displayUserInterests() {
        val interestsText = if (isManualFilterMode) {
            "Manual filter mode • Showing selected category only"
        } else if (userInterests.isEmpty() || userInterests.contains("All")) {
            "All categories • Showing places from all interests • Tap to change in Settings"
        } else {
            "Showing places from: ${userInterests.joinToString(", ")} • Tap to change in Settings"
        }
        binding.interestsText.text = interestsText
        
        // Make the interests card clickable to open settings (only when not in manual mode)
        binding.interestsCard.setOnClickListener {
            if (!isManualFilterMode) {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Disable manual filter to change interests", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Map functionality
    private fun setupOSMMap() {
        osmMapView = binding.osmMapView
        
        // Set tile source to OpenStreetMap
        osmMapView.setTileSource(TileSourceFactory.MAPNIK)
        
        // Enable zoom controls
        osmMapView.setBuiltInZoomControls(true)
        osmMapView.setMultiTouchControls(true)
        
        // Set initial zoom level
        osmMapView.controller.setZoom(15.0)
        
        // Add my location overlay with better visibility
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), osmMapView)
        myLocationOverlay?.let { overlay ->
            overlay.enableMyLocation()
            overlay.enableFollowLocation()
            
            // Make location marker more visible
            overlay.setPersonIcon(createUserLocationBitmap())
            overlay.setDirectionIcon(createUserLocationBitmap())
            
            // Add center on location button
            overlay.enableFollowLocation()
            overlay.isDrawAccuracyEnabled = true
        }
        
        osmMapView.overlays.add(myLocationOverlay)
    }
    
    private fun createUserLocationBitmap(): android.graphics.Bitmap {
        // Use the StepUp logo as the user location icon
        val size = 100
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw white circular background for visibility
        val backgroundPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, backgroundPaint)
        
        // Draw blue border
        val borderPaint = android.graphics.Paint().apply {
            color = resources.getColor(android.R.color.holo_blue_bright, theme)
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, borderPaint)
        
        try {
            // Load the small StepUp logo
            val logoDrawable = resources.getDrawable(R.drawable.stepup_logo_bunny_small, theme)
            
            // Calculate logo size (smaller than the circle)
            val logoSize = (size * 0.7f).toInt()
            val logoLeft = (size - logoSize) / 2
            val logoTop = (size - logoSize) / 2
            
            // Save canvas state for rotation
            canvas.save()
            
            // Rotate canvas to fix orientation (quarter turn to fix sideways logo)
            canvas.rotate(90f, size / 2f, size / 2f)
            
            // Set bounds and draw the logo
            logoDrawable.setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
            logoDrawable.draw(canvas)
            
            // Restore canvas state
            canvas.restore()
            
        } catch (e: Exception) {
            // Fallback: draw a simple blue circle if logo fails to load
            val fallbackPaint = android.graphics.Paint().apply {
                color = resources.getColor(android.R.color.holo_blue_bright, theme)
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(size / 2f, size / 2f, 20f, fallbackPaint)
        }
        
        return bitmap
    }

    private fun setupCenterLocationButton() {
        binding.centerLocationFab.setOnClickListener {
            centerMapOnUser()
        }
        
        binding.clearRouteFab.setOnClickListener {
            clearRoute()
            binding.clearRouteFab.visibility = View.GONE
            Toast.makeText(this, "Route cleared", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun centerMapOnUser() {
        val userLocation = GeoPoint(currentLatitude, currentLongitude)
        osmMapView.controller.animateTo(userLocation)
        osmMapView.controller.setZoom(16.0)
        Toast.makeText(this, "Centered on your location", Toast.LENGTH_SHORT).show()
    }

    private fun setupMapToggle() {
        binding.mapToggleButton.setOnClickListener {
            toggleMapView()
        }
    }

    private fun toggleMapView() {
        isMapViewActive = !isMapViewActive
        
        if (isMapViewActive) {
            // Show map, hide list
            binding.mapContainer.visibility = View.VISIBLE
            binding.placesContainer.visibility = View.GONE
            binding.mapToggleButton.text = "📋 List"
            binding.mapToggleButton.setBackgroundColor(getColor(R.color.dark_blue))
            
            // Update map with current places
            updateOSMMap()
            
        } else {
            // Show list, hide map
            binding.mapContainer.visibility = View.GONE
            binding.placesContainer.visibility = View.VISIBLE
            binding.mapToggleButton.text = "🗺️ Map"
            binding.mapToggleButton.setBackgroundColor(getColor(R.color.primary_green))
            
            // Clear any active route when switching to list view
            clearRoute()
            binding.clearRouteFab.visibility = View.GONE
        }
        
        Toast.makeText(this, if (isMapViewActive) "Map view" else "List view", Toast.LENGTH_SHORT).show()
    }

    private fun updateOSMMap() {
        // Clear existing markers (except my location)
        osmMapView.overlays.clear()
        
        // Re-add and update my location overlay
        myLocationOverlay?.let { overlay ->
            overlay.enableMyLocation()
            overlay.enableFollowLocation()
            overlay.setPersonIcon(createUserLocationBitmap())
            overlay.setDirectionIcon(createUserLocationBitmap())
            overlay.isDrawAccuracyEnabled = true
            osmMapView.overlays.add(overlay)
        }
        
        // Set map center to user location
        val userLocation = GeoPoint(currentLatitude, currentLongitude)
        osmMapView.controller.setCenter(userLocation)
        
        // Add markers for filtered places
        filteredPlacesList.forEach { place ->
            val marker = Marker(osmMapView)
            marker.position = GeoPoint(place.point.lat, place.point.lon)
            marker.title = place.name.ifEmpty { "Unnamed Place" }
            marker.snippet = "${place.kinds.split(",").firstOrNull()?.replace("_", " ") ?: "Unknown"} • ${(place.dist/STEP_LENGTH).roundToInt()} steps away"
            
            // Set custom marker icon
            marker.icon = getMarkerDrawableForCategory(place.kinds)
            
            // Make marker more interactive
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            // Add click listener for route drawing
            marker.setOnMarkerClickListener { clickedMarker, mapView ->
                drawRouteToPlace(place)
                true // Return true to consume the event
            }
            
            osmMapView.overlays.add(marker)
        }
        
        // Refresh the map
        osmMapView.invalidate()
    }
    
    private fun getMarkerDrawableForCategory(kinds: String): android.graphics.drawable.Drawable? {
        val kindsLower = kinds.lowercase()
        val colorRes = when {
            kindsLower.contains("food") || kindsLower.contains("restaurant") -> android.R.color.holo_orange_dark
            kindsLower.contains("cultural") || kindsLower.contains("museum") -> android.R.color.holo_purple
            kindsLower.contains("natural") || kindsLower.contains("park") -> android.R.color.holo_green_dark
            kindsLower.contains("shop") || kindsLower.contains("mall") -> android.R.color.holo_orange_light
            kindsLower.contains("historic") -> android.R.color.holo_red_dark
            kindsLower.contains("sport") -> android.R.color.holo_blue_dark
            else -> android.R.color.holo_red_light
        }
        
        return createCustomMarker(colorRes)
    }
    
    private fun createCustomMarker(colorRes: Int): android.graphics.drawable.Drawable {
        // Create a more visible custom marker
        val size = 80
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw outer circle (white border)
        val outerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, outerPaint)
        
        // Draw inner circle (colored)
        val innerPaint = android.graphics.Paint().apply {
            color = resources.getColor(colorRes, theme)
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, innerPaint)
        
        // Draw center dot
        val centerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, 8f, centerPaint)
        
        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }
    
    private fun drawRouteToPlace(place: OpenTripMapResponse) {
        // Remove existing route if any
        currentRouteOverlay?.let { route ->
            osmMapView.overlays.remove(route)
        }
        
        // Show loading feedback
        Toast.makeText(this, "Calculating route...", Toast.LENGTH_SHORT).show()
        
        // Get route following roads
        lifecycleScope.launch {
            try {
                val routePoints = getWalkingRoute(currentLatitude, currentLongitude, place.point.lat, place.point.lon)
                
                if (routePoints.isNotEmpty()) {
                    // Create polyline for the route
                    val routeLine = Polyline().apply {
                        setPoints(routePoints)
                        color = resources.getColor(android.R.color.holo_blue_bright, theme)
                        width = 10f
                        isGeodesic = false // We have detailed points, no need for geodesic
                    }
                    
                    // Add route to map
                    currentRouteOverlay = routeLine
                    osmMapView.overlays.add(routeLine)
                    
                    // Calculate bounds to show the route
                    val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(routePoints)
                    
                    // Zoom to show the route with some padding
                    osmMapView.post {
                        osmMapView.zoomToBoundingBox(boundingBox, true, 100)
                    }
                    
                    // Show clear route button
                    binding.clearRouteFab.visibility = View.VISIBLE
                    
                    // Calculate walking distance from route
                    val routeDistance = calculateRouteDistance(routePoints)
                    val walkingSteps = (routeDistance / STEP_LENGTH).roundToInt()
                    
                    // Show feedback to user
                    val placeName = place.name.ifEmpty { "Selected place" }
                    Toast.makeText(this@ExploreActivity, "Route to $placeName • $walkingSteps steps", Toast.LENGTH_LONG).show()
                    
                } else {
                    // Fallback to straight line if routing fails
                    drawStraightLineRoute(place)
                }
                
                // Refresh the map
                osmMapView.invalidate()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting route: ${e.message}")
                // Fallback to straight line route
                drawStraightLineRoute(place)
            }
        }
    }
    
    private suspend fun getWalkingRoute(startLat: Double, startLon: Double, endLat: Double, endLon: Double): List<GeoPoint> {
        return withContext(Dispatchers.IO) {
            try {
                // Using OSRM (OpenStreetMap Routing Machine) - completely free, no API key needed
                val url = "https://router.project-osrm.org/route/v1/foot/" +
                        "$startLon,$startLat;$endLon,$endLat?" +
                        "overview=full&geometries=geojson"
                
                val response = URL(url).readText()
                val json = JSONObject(response)
                
                // Check if route was found
                if (json.getString("code") != "Ok") {
                    Log.w(TAG, "OSRM routing failed: ${json.optString("message", "Unknown error")}")
                    return@withContext emptyList()
                }
                
                val coordinates = json.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates")
                
                val routePoints = mutableListOf<GeoPoint>()
                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    val lon = coord.getDouble(0)
                    val lat = coord.getDouble(1)
                    routePoints.add(GeoPoint(lat, lon))
                }
                
                Log.d(TAG, "OSRM route found with ${routePoints.size} points")
                routePoints
                
            } catch (e: Exception) {
                Log.e(TAG, "OSRM routing failed: ${e.message}")
                // Return empty list to trigger fallback
                emptyList()
            }
        }
    }
    
    private fun drawStraightLineRoute(place: OpenTripMapResponse) {
        // Fallback: straight line route
        val userLocation = GeoPoint(currentLatitude, currentLongitude)
        val placeLocation = GeoPoint(place.point.lat, place.point.lon)
        val routePoints = listOf(userLocation, placeLocation)
        
        val routeLine = Polyline().apply {
            setPoints(routePoints)
            color = resources.getColor(android.R.color.holo_orange_dark, theme) // Orange for straight line
            width = 8f
            isGeodesic = true
        }
        
        currentRouteOverlay = routeLine
        osmMapView.overlays.add(routeLine)
        
        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(routePoints)
        osmMapView.post {
            osmMapView.zoomToBoundingBox(boundingBox, true, 100)
        }
        
        binding.clearRouteFab.visibility = View.VISIBLE
        
        val placeName = place.name.ifEmpty { "Selected place" }
        val distance = (place.dist / STEP_LENGTH).roundToInt()
        Toast.makeText(this, "Straight line to $placeName • $distance steps", Toast.LENGTH_LONG).show()
        
        osmMapView.invalidate()
    }
    
    private fun calculateRouteDistance(routePoints: List<GeoPoint>): Double {
        var totalDistance = 0.0
        for (i in 0 until routePoints.size - 1) {
            val point1 = routePoints[i]
            val point2 = routePoints[i + 1]
            totalDistance += point1.distanceToAsDouble(point2)
        }
        return totalDistance
    }
    
    private fun clearRoute() {
        currentRouteOverlay?.let { route ->
            osmMapView.overlays.remove(route)
            currentRouteOverlay = null
            osmMapView.invalidate()
        }
    }
}