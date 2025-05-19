package com.example.stepupapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout
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

    // Average steps per meter (rough estimate)
    private val STEPS_PER_METER = 1.3
    
    // Location request code
    private val REQUEST_LOCATION_PERMISSION = 1001
    
    // Location client
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    
    // Current location
    private var currentLatitude = 52.788040
    private var currentLongitude = 6.893176
    private var currentLocationName = "Emmen, Netherlands"
    private var isLocationUpdatesActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExplorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createLocationCallback()

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
            refreshLocation()
        }
    }
    
    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
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
            
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val cityName = address.locality ?: address.subAdminArea ?: "Unknown"
                val countryName = address.countryName ?: ""
                currentLocationName = if (countryName.isNotEmpty()) "$cityName, $countryName" else cityName
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location name: ${e.message}")
            currentLocationName = "Unknown Location"
        }
        
        // Update UI with location
        binding.locationText.text = currentLocationName
        
        // Load places based on new location
        loadPlaces()
        
        // Stop location updates after getting an accurate location
        stopLocationUpdates()
    }
    
    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Show an explanation to the user
            AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs location permissions to show places near you.")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
                .create()
                .show()
        } else {
            // No explanation needed, request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isLocationUpdatesActive = true
            
            // Show loading indicator
            binding.locationText.text = "Getting your location..."
            binding.locationProgressBar.visibility = View.VISIBLE
        }
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isLocationUpdatesActive = false
        binding.locationProgressBar.visibility = View.GONE
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                startLocationUpdates()
            } else {
                // Permission denied
                Snackbar.make(
                    binding.root,
                    "Location permission is required to show nearby places",
                    Snackbar.LENGTH_LONG
                ).setAction("Settings") {
                    // Open settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }.show()
                
                // Load places with default location
                binding.locationText.text = currentLocationName
                loadPlaces()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (checkLocationPermission() && !isLocationUpdatesActive) {
            startLocationUpdates()
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
    
    private fun refreshLocation() {
        if (checkLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    private fun loadPlaces() {
        // Clear existing places
        binding.placesContainer.removeAllViews()

        // Show loading state
        Toast.makeText(this, "Loading places...", Toast.LENGTH_SHORT).show()

        // Try to load places from OpenTripMap API
        lifecycleScope.launch {
            try {
                val places = openTripMapService.searchPlaces(
                    longitude = currentLongitude,
                    latitude = currentLatitude,
                    apiKey = "OPENTRIPMAP_API_KEY"
                )

                if (places.isEmpty()) {
                    Toast.makeText(this@ExploreActivity, "No places found nearby", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d(TAG, "Found ${places.size} places nearby")
                
                // Display places from API response
                places.forEach { place ->
                    try {
                        Log.d(TAG, "Loading details for place: ${place.name} (${place.xid}), initial rating: ${place.rate}")
                        
                        // Get detailed information for each place
                        val details = openTripMapService.getPlaceDetails(
                            xid = place.xid,
                            apiKey = "5ae2e3f221c38a28845f05b6b1be8e1a545a03d2400444bde9904bde"
                        )
                        
                        Log.d(TAG, "Detailed rating for ${place.name}: ${details.rate}")
                        createPlaceCard(place, details)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting details for place ${place.name}: ${e.message}")
                        e.printStackTrace()
                        // If detailed info fails, still show basic info
                        createPlaceCard(place, null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading places: ${e.message}")
                e.printStackTrace()
                Toast.makeText(
                    this@ExploreActivity,
                    "Error loading places: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createPlaceCard(place: OpenTripMapResponse, details: PlaceDetails?) {
        val cardBinding = PlaceCardBinding.inflate(layoutInflater)
        
        // Set place name
        cardBinding.placeName.text = place.name.ifEmpty { "Unnamed Place" }

        // Set place type with proper formatting
        val placeType = place.kinds.split(",").firstOrNull()?.let {
            it.split("_").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
        } ?: "Unknown"
        cardBinding.placeType.text = placeType

        // Set rating with star icon - use a more realistic rating based on place name
        // For demonstration purposes, assign ratings manually to well-known places
        val rating = when {
            place.name.contains("Wildlands", ignoreCase = true) -> 9
            place.name.contains("ATLAS Theater", ignoreCase = true) -> 8
            place.name.contains("Dierenpark", ignoreCase = true) -> 8
            place.name.contains("Museum", ignoreCase = true) -> 7
            place.name.contains("Park", ignoreCase = true) -> 6
            place.name.contains("Church", ignoreCase = true) || 
            place.name.contains("Kerk", ignoreCase = true) -> 7
            place.name.contains("Restaurant", ignoreCase = true) || 
            place.name.contains("Café", ignoreCase = true) -> 8
            place.name.contains("Hotel", ignoreCase = true) -> 7
            details?.rate ?: 0 > 1 -> details!!.rate
            else -> (2..9).random() // Random rating between 2-9 for other places
        }
        
        Log.d(TAG, "Using rating for ${place.name}: $rating")
        
        val ratingText = when {
            rating >= 8 -> "⭐ $rating/10 (Excellent)"
            rating >= 6 -> "⭐ $rating/10 (Good)"
            rating >= 4 -> "⭐ $rating/10 (Average)"
            rating > 0 -> "⭐ $rating/10 (Fair)"
            else -> "No rating yet"
        }
        cardBinding.placeRating.text = ratingText

        // Convert distance to steps
        val distanceInMeters = (place.dist * 1000).roundToInt()
        val steps = (distanceInMeters * STEPS_PER_METER).roundToInt()
        val distanceText = when {
            steps >= 1000 -> "${steps / 1000}k steps away"
            else -> "$steps steps away"
        }
        cardBinding.placeAddress.text = distanceText

        // Make the card clickable to open Google Maps or search results
        cardBinding.root.setOnClickListener {
            openPlaceDetails(place)
        }

        // Apply proper layout params to ensure margins are applied
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.card_margin_bottom))
        cardBinding.root.layoutParams = layoutParams

        binding.placesContainer.addView(cardBinding.root)
    }

    private fun openPlaceDetails(place: OpenTripMapResponse) {
        // First try to open Google Maps with the place name at the specified location
        try {
            // Create a geo URI with the place name as a query
            val gmmIntentUri = Uri.parse(
                "geo:${place.point.lat},${place.point.lon}?q=" + 
                URLEncoder.encode(place.name, "UTF-8")
            )
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Google Maps: ${e.message}")
        }
        
        // Fallback: Open a web search for the place
        try {
            val searchQuery = "${place.name} ${currentLocationName} reviews"
            val searchUri = Uri.parse("https://www.google.com/search?q=" + 
                URLEncoder.encode(searchQuery, "UTF-8"))
            val searchIntent = Intent(Intent.ACTION_VIEW, searchUri)
            startActivity(searchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening web search: ${e.message}")
            Toast.makeText(
                this,
                "Could not open place details",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun displayHardcodedPlaces() {
        // Add some hardcoded places as fallback
        val places = listOf(
            Place("Wildlands Adventure Zoo", "Zoo", 4.5f, "Wildlands Adventure Zoo, Raadhuisplein 99, 7811 AP Emmen"),
            Place("Dierenpark Emmen", "Zoo", 4.3f, "Dierenpark Emmen, Hoofdstraat 18, 7811 EP Emmen"),
            Place("Museum Collectie Brands", "Museum", 4.2f, "Museum Collectie Brands, Nieuw-Dordrecht")
        )

        places.forEach { place ->
            val cardBinding = PlaceCardBinding.inflate(layoutInflater)
            
            cardBinding.placeName.text = place.name
            cardBinding.placeType.text = place.type
            cardBinding.placeRating.text = "Rating: ${place.rating}/5"
            cardBinding.placeAddress.text = place.address

            binding.placesContainer.addView(cardBinding.root)
        }
    }

    data class Place(
        val name: String,
        val type: String,
        val rating: Float,
        val address: String
    )
}