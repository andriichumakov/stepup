package com.example.stepupapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.api.OpenTripMapResponse
import com.example.stepupapp.api.OpenTripMapService
import com.example.stepupapp.api.PlaceDetails
import com.example.stepupapp.databinding.ExplorePageBinding
import com.example.stepupapp.databinding.PlaceCardBinding
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder
import java.util.*
import kotlin.math.roundToInt

class ExploreActivity : AppCompatActivity() {
    private lateinit var binding: ExplorePageBinding
    private lateinit var openTripMapService: OpenTripMapService
    private val TAG = "ExploreActivity"

    private val STEP_LENGTH = 0.50 // Average step length in meters
    private val REQUEST_LOCATION_PERMISSION = 1001

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var currentLatitude = 52.788040
    private var currentLongitude = 6.893176
    private var currentLocationName = "Emmen, Netherlands"
    private var isLocationUpdatesActive = false

    private var allPlacesList = listOf<OpenTripMapResponse>()
    private var currentSubcategory = ""
    private var userInterests = setOf<String>()
    private var isManualFilterMode = false
    private var filterJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExplorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load user interests
        userInterests = UserPreferences.getUserInterests(this)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createLocationCallback()

        // Initialize category spinner (hidden by default)
        setupCategorySpinner()
        
        // Initialize subcategory search
        setupSubcategorySearch()

        // Setup toggle button for manual filtering
        setupToggleButton()

        // Display user interests
        displayUserInterests()

        // Set up home button to navigate back to home screen
        binding.homeButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Initialize OpenTripMap service
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

        openTripMapService = retrofit.create(OpenTripMapService::class.java)

        // Show default loading state
        binding.locationText.text = "Getting your location..."

        // Check for location permissions
        if (checkLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }

        // Make location card clickable to refresh location
        binding.locationCard.setOnClickListener {
            if (checkLocationPermission()) {
                binding.locationProgressBar.visibility = View.VISIBLE
                binding.locationText.text = "Refreshing location..."
                startLocationUpdates()
                Toast.makeText(this, "Refreshing nearby places...", Toast.LENGTH_SHORT).show()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocationUI(location)
                }
            }
        }
    }

    private fun updateLocationUI(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude

        // Get location name using Geocoder
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val cityName = address.locality ?: address.subAdminArea ?: "Unknown"
                val countryName = address.countryName ?: ""
                currentLocationName = if (countryName.isNotEmpty()) "$cityName, $countryName" else cityName
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location name: ${e.message}")
            currentLocationName = "Unknown Location"
        }

        // Update UI
        binding.locationText.text = currentLocationName

        // Load nearby places
        loadPlaces()

        // Stop updates once we have it
        stopLocationUpdates()
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs location permissions to show places near you.")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
                .create()
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isLocationUpdatesActive = true
            binding.locationText.text = "Getting your location..."
            binding.locationProgressBar.visibility = View.VISIBLE
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isLocationUpdatesActive = false
        binding.locationProgressBar.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
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
                loadPlaces()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Refresh user interests display and filter
        userInterests = UserPreferences.getUserInterests(this)
        displayUserInterests()
        
        // Refresh places with potentially updated interests
        if (allPlacesList.isNotEmpty()) {
            filterPlaces()
        }
        
        if (checkLocationPermission() && !isLocationUpdatesActive) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
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
                    // Override user interests with manual selection
                    userInterests = setOf(selectedCategory)
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
                if (s.toString().isNotEmpty()) {
                    currentSubcategory = s.toString()
                    filterPlaces()
                } else {
                    currentSubcategory = ""
                    filterPlaces()
                }
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
                Toast.makeText(this, "Showing places from your interests", Toast.LENGTH_SHORT).show()
            }
            filterPlaces()
        }
    }

    private fun loadPlaces() {
        // Cancel any ongoing filter operation
        filterJob?.cancel()
        
        binding.placesContainer.removeAllViews()
        allPlacesList = emptyList()
        Toast.makeText(this, "Loading places...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Making API call with coordinates: lat=$currentLatitude, lon=$currentLongitude")
                val places = openTripMapService.searchPlaces(
                    longitude = currentLongitude,
                    latitude = currentLatitude,
                    apiKey = "5ae2e3f221c38a28845f05b66b2ebd0c0a4a7428f0803525b45f11d8"
                )

                Log.d(TAG, "API response received: ${places.size} places")
                
                // Filter out dummy/test data
                val filteredPlaces = places.filter { place ->
                    val name = place.name.lowercase()
                    // Exclude Android codenames and other obvious dummy data
                    !name.contains("eclair") && 
                    !name.contains("marshmallow") &&
                    !name.contains("lollipop") &&
                    !name.contains("play music") &&
                    !name.contains("google sign") &&
                    !name.contains("android") &&
                    !name.contains("test") &&
                    name.isNotBlank() &&
                    place.dist <= 15000 // Ensure within 15km
                }
                
                // Remove duplicates based on xid (unique identifier)
                val uniquePlaces = filteredPlaces.distinctBy { it.xid }
                
                // Also remove duplicates by name (in case same place has different xids)
                val finalPlaces = uniquePlaces.groupBy { it.name.lowercase().trim() }
                    .mapValues { entry -> entry.value.minByOrNull { it.dist } } // Keep closest if duplicates by name
                    .values
                    .filterNotNull()
                    .sortedBy { it.dist } // Sort by distance (closest first)
                
                finalPlaces.forEachIndexed { index, place ->
                    Log.d(TAG, "Unique Place $index: name='${place.name}', xid='${place.xid}', kinds='${place.kinds}', dist=${place.dist}")
                }

                if (finalPlaces.isEmpty()) {
                    Toast.makeText(this@ExploreActivity, "No places found nearby", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                allPlacesList = finalPlaces
                filterPlaces() // Apply interest-based filtering immediately
            } catch (e: Exception) {
                Log.e(TAG, "Error loading places: ${e.message}", e)
                Toast.makeText(this@ExploreActivity, "Error loading places: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun filterPlaces() {
        if (allPlacesList.isEmpty()) return
        
        // Cancel any ongoing filter operation
        filterJob?.cancel()
        
        // Clear the container immediately
        binding.placesContainer.removeAllViews()

        // Create filters based on user interests instead of single category
        val interestFilters = if (userInterests.isEmpty() || userInterests.contains("All")) {
            emptyList() // Show all if no specific interests
        } else {
            userInterests.mapNotNull { interest ->
                when (interest) {
                    "Amusements" -> "amusements"
                    "Architecture" -> "architecture"
                    "Cultural" -> "cultural"
                    "Shops" -> "shops"
                    "Foods" -> "foods,cuisine"
                    "Sport" -> "sport"
                    "Historical" -> "historic"
                    "Natural" -> "natural"
                    "Other" -> "other"
                    else -> null
                }
            }
        }

        // Get subcategory filter from search
        val subcategoryFilter = getSubcategoryFilter(currentSubcategory)

        filterJob = lifecycleScope.launch {
            val filteredPlaces = allPlacesList.filter { place ->
                val kinds = place.kinds.lowercase()
                val name = place.name.lowercase()
                
                // First filter: Block all adult content (FAMILY SAFETY)
                val isAdultContent = kinds.contains("adult") || 
                                   kinds.contains("strip") ||
                                   kinds.contains("nightclub") ||
                                   kinds.contains("casino") ||
                                   kinds.contains("gambling") ||
                                   kinds.contains("brewery") ||
                                   kinds.contains("bar") ||
                                   kinds.contains("pub") ||
                                   name.contains("casino") ||
                                   name.contains("strip") ||
                                   name.contains("adult")
                
                if (isAdultContent) return@filter false
                
                // Second filter: Interest-based categories (show places from ALL user interests)
                val matchesInterests = if (interestFilters.isEmpty()) {
                    true // Show all if no specific interests
                } else {
                    interestFilters.any { filter ->
                        filter.split(",").any { kinds.contains(it.trim()) }
                    }
                }
                
                // Third filter: Subcategory filter from search
                val matchesSubcategory = if (subcategoryFilter.isEmpty() || 
                                           currentSubcategory.isEmpty()) {
                    true
                } else {
                    subcategoryFilter.split(",").any { kinds.contains(it.trim()) }
                }
                
                matchesInterests && matchesSubcategory
            }

            if (filteredPlaces.isEmpty()) {
                val message = if (currentSubcategory.isNotEmpty()) {
                    "No places found for: $currentSubcategory"
                } else if (userInterests.isNotEmpty() && !userInterests.contains("All")) {
                    "No places found for your interests: ${userInterests.joinToString(", ")}"
                } else {
                    "No places found nearby"
                }
                Toast.makeText(this@ExploreActivity, message, Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Clear container again before adding new cards (double safety)
            binding.placesContainer.removeAllViews()

            for (place in filteredPlaces) {
                try {
                    val details = openTripMapService.getPlaceDetails(
                        xid = place.xid,
                        apiKey = "5ae2e3f221c38a28845f05b66b2ebd0c0a4a7428f0803525b45f11d8"
                    )
                    createPlaceCard(place, details)
                } catch (e: Exception) {
                    createPlaceCard(place, null)
                }
            }
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

    private fun openPlaceDetails(place: OpenTripMapResponse, details: PlaceDetails?, rating: String, steps: String) {
        val intent = Intent(this, LocationDetailsActivity::class.java).apply {
            putExtra("name", place.name.ifEmpty { "Unnamed Place" })
            putExtra("type", place.kinds.split(",").firstOrNull()?.replace("_", " ") ?: "Unknown")
            putExtra("description", details?.wikipedia_extracts?.text ?: "No description available for this location.")
            putExtra("rating", rating)
            putExtra("steps", steps)
            putExtra("openingHours", details?.sources?.opening_hours ?: "Opening hours not available")
        }
        startActivity(intent)
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

    private fun getSubcategoryFilter(subcategory: String): String {
        return when (subcategory.lowercase()) {
            // Basic categories
            "restaurant" -> "restaurant"
            "cafe" -> "cafe"
            "museum" -> "museum"
            "park" -> "park"
            "shopping mall" -> "mall"
            "hotel" -> "hotel"
            "church" -> "church"
            "beach" -> "beach"
            "monument" -> "monument"
            "zoo" -> "zoo"
            "aquarium" -> "aquarium"
            "theatre" -> "theatre"
            "castle" -> "castle"
            "bridge" -> "bridge"
            "tower" -> "tower"
            "garden" -> "garden"
            "lake" -> "lake"
            "viewpoint" -> "viewpoint"
            "stadium" -> "stadium"
            "swimming pool" -> "swimming_pool"
            "library" -> "library"
            "art gallery" -> "gallery"
            "cinema" -> "cinema"
            "supermarket" -> "supermarket"
            "hospital" -> "hospital"
            "school" -> "school"
            "bank" -> "bank"
            "gas station" -> "fuel"
            "theme park" -> "amusement_park"
            "historic building" -> "historic"
            else -> ""
        }
    }
}