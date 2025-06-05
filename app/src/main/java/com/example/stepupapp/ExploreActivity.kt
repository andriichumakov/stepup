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
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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

    private var currentCategory = "All"
    private var allPlacesList = listOf<OpenTripMapResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExplorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createLocationCallback()

        // Initialize category spinner
        setupCategorySpinner()

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

        // Make location card clickable to open current location on Google Maps
        binding.locationCard.setOnClickListener {
            openCurrentLocationOnMaps()
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
                if (currentCategory != selectedCategory) {
                    currentCategory = selectedCategory
                    filterPlaces()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadPlaces() {
        binding.placesContainer.removeAllViews()
        allPlacesList = emptyList()
        Toast.makeText(this, "Loading places...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val places = openTripMapService.searchPlaces(
                    longitude = currentLongitude,
                    latitude = currentLatitude,
                    apiKey = "5ae2e3f221c38a28845f05b6b1be8e1a545a03d2400444bde9904bde"
                )

                if (places.isEmpty()) {
                    Toast.makeText(this@ExploreActivity, "No places found nearby", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                allPlacesList = places
                filterPlaces()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading places: ${e.message}")
            }
        }
    }

    private fun filterPlaces() {
        if (allPlacesList.isEmpty()) return
        binding.placesContainer.removeAllViews()

        val kindFilter = when (currentCategory) {
            "All" -> ""
            "Adult" -> "adult"
            "Amusements" -> "amusements"
            "Architecture" -> "architecture"
            "Cultural" -> "cultural"
            "Shops" -> "shops"
            "Foods" -> "foods,cuisine"
            "Sport" -> "sport"
            "Historical" -> "historic"
            "Natural" -> "natural"
            "Other" -> "other"
            else -> ""
        }

        lifecycleScope.launch {
            val filteredPlaces = if (kindFilter.isEmpty()) {
                allPlacesList
            } else {
                allPlacesList.filter { it.kinds.contains(kindFilter) }
            }

            if (filteredPlaces.isEmpty()) {
                Toast.makeText(this@ExploreActivity, "No places found in category: $currentCategory", Toast.LENGTH_SHORT).show()
                return@launch
            }

            for (place in filteredPlaces) {
                try {
                    val details = openTripMapService.getPlaceDetails(
                        xid = place.xid,
                        apiKey = "5ae2e3f221c38a28845f05b6b1be8e1a545a03d2400444bde9904bde"
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

        cardBinding.root.setOnClickListener {
            openPlaceDetails(place, details, rating.toString(), stepsText)
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
            putExtra("name", place.name)
            putExtra("type", place.kinds.split(",").firstOrNull()?.replace("_", " ") ?: "Unknown")
            putExtra("description", details?.wikipedia_extracts?.text ?: "No description available")
            putExtra("rating", rating)
            putExtra("steps", steps)
            putExtra("openingHours", details?.sources?.opening_hours ?: "Opening hours not available")
        }
        startActivity(intent)
    }

    private fun openCurrentLocationOnMaps() {
        try {
            val uri = Uri.parse("geo:$currentLatitude,$currentLongitude?q=" +
                    URLEncoder.encode(currentLocationName, "UTF-8"))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open maps", Toast.LENGTH_SHORT).show()
        }
    }
}