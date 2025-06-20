package com.example.stepupapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.presentation.explore.MapController
import org.osmdroid.views.MapView
import java.net.URLEncoder

class LocationDetailsActivity : AppCompatActivity(), MapController.MapControllerListener {

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var locationName: String = ""
    private lateinit var mapController: MapController
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_details)

        // Enable back button in the toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Location Details"

        // Get data from intent
        val name = intent.getStringExtra("name") ?: "Unnamed Place"
        val type = intent.getStringExtra("type") ?: "Unknown"
        val description = intent.getStringExtra("description") ?: "No description available"
        val rating = intent.getStringExtra("rating") ?: "No rating available"
        val steps = intent.getStringExtra("steps") ?: ""
        val openingHours = intent.getStringExtra("openingHours") ?: ""
        val facebookUrl = intent.getStringExtra("facebookUrl")
        val instagramUrl = intent.getStringExtra("instagramUrl")
        val tiktokUrl = intent.getStringExtra("tiktokUrl")
        val xUrl = intent.getStringExtra("xUrl")

        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        locationName = name

        Log.d("LocationDetails", "Coordinates: $latitude, $longitude")

        // Initialize map
        initializeMap()

        // Set text content
        findViewById<TextView>(R.id.locationName).text = name
        findViewById<TextView>(R.id.locationType).text = type
        findViewById<TextView>(R.id.locationDescription).text = description
        findViewById<TextView>(R.id.locationRating).text = "⭐ $rating"
        findViewById<TextView>(R.id.locationStepsAway).text = steps
        findViewById<TextView>(R.id.locationOpeningHours).text = openingHours

        // Handle social media links
        findViewById<ImageView>(R.id.facebookIcon).setOnClickListener {
            openSocialMedia("https://www.facebook.com/${name}")
        }
        findViewById<ImageView>(R.id.instagramIcon).setOnClickListener {
            openSocialMedia("https://www.instagram.com/${name}")
        }
        findViewById<ImageView>(R.id.tiktokIcon).setOnClickListener {
            openSocialMedia("https://www.tiktok.com/@${name}")
        }
        findViewById<ImageView>(R.id.xIcon).setOnClickListener {
            openSocialMedia("https://twitter.com/${name}")
        }

        // Buttons
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnOpenMaps).setOnClickListener {
            openLocationInMaps()
        }
    }

    private fun initializeMap() {
        mapView = findViewById(R.id.mapPreview)
        mapController = MapController(this, lifecycleScope)
        mapController.setListener(this)
        mapController.initializeMapForLocationDetails(mapView)
        
        // Center map on the place location and add marker
        if (latitude != 0.0 && longitude != 0.0) {
            mapController.centerOnLocation(latitude, longitude, locationName)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) {
            mapView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) {
            mapView.onPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // MapController.MapControllerListener Implementation
    override fun onRouteCalculated(steps: Int, placeName: String) {
        // Not used in details view
    }

    override fun onRouteCleared() {
        // Not used in details view
    }

    override fun onMapError(message: String) {
        Toast.makeText(this, "Map error: $message", Toast.LENGTH_SHORT).show()
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onMapLocationClicked(latitude: Double, longitude: Double, placeName: String?, category: String?, distance: Int?) {
        // Not used in details view - map is read-only here
    }

    private fun setupSocialLink(viewId: Int, url: String?) {
        val icon = findViewById<ImageView>(viewId)
        if (url.isNullOrBlank()) {
            icon.visibility = View.GONE
        } else {
            icon.visibility = View.VISIBLE
            icon.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openLocationInMaps() {
        try {
            Log.d("LocationDetails", "Attempting to open maps with coordinates: lat=$latitude, lng=$longitude")
            
            // Check if coordinates are valid
            if (latitude == 0.0 && longitude == 0.0) {
                Log.e("LocationDetails", "Invalid coordinates: both latitude and longitude are 0.0")
                Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Try 1: Google Maps Navigation URI with walking mode
            try {
                val navigationUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=w")
                val navigationIntent = Intent(Intent.ACTION_VIEW, navigationUri)
                startActivity(navigationIntent)
                Log.d("LocationDetails", "Successfully opened Google Maps navigation (walking)")
                Toast.makeText(this, "Opening walking navigation", Toast.LENGTH_SHORT).show()
                return
            } catch (e: Exception) {
                Log.d("LocationDetails", "Walking navigation intent failed: ${e.message}")
            }
            
            // Try 2: Standard geo URI (works with any mapping app)
            try {
                val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
                startActivity(geoIntent)
                Log.d("LocationDetails", "Successfully opened with geo URI")
                Toast.makeText(this, "Opening location in maps", Toast.LENGTH_SHORT).show()
                return
            } catch (e: Exception) {
                Log.d("LocationDetails", "Geo URI intent failed: ${e.message}")
            }
            
            // Try 3: Google Maps URL with walking directions
            try {
                val mapsUrl = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                startActivity(webIntent)
                Log.d("LocationDetails", "Successfully opened web maps with walking directions")
                Toast.makeText(this, "Opening walking directions in browser", Toast.LENGTH_SHORT).show()
                return
            } catch (e: Exception) {
                Log.d("LocationDetails", "Web walking maps intent failed: ${e.message}")
            }
            
            // Try 4: Alternative Google Maps walking URL format
            try {
                val walkingUrl = "https://maps.google.com/maps?saddr=My+Location&daddr=$latitude,$longitude&dirflg=w"
                val walkingIntent = Intent(Intent.ACTION_VIEW, Uri.parse(walkingUrl))
                startActivity(walkingIntent)
                Log.d("LocationDetails", "Successfully opened alternative walking maps")
                Toast.makeText(this, "Opening walking route in browser", Toast.LENGTH_SHORT).show()
                return
            } catch (e: Exception) {
                Log.d("LocationDetails", "Alternative walking intent failed: ${e.message}")
            }
            
            // If all else fails
            Log.e("LocationDetails", "All mapping intents failed")
            Toast.makeText(this, "Unable to open maps. Please check if you have a maps app installed.", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e("LocationDetails", "Error in openLocationInMaps: ${e.message}")
            Toast.makeText(this, "Error opening maps: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openLocationInGoogleSearch() {
        try {
            val searchQuery = URLEncoder.encode("$locationName location", "UTF-8")
            val uri = Uri.parse("https://www.google.com/search?q=$searchQuery")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open location search", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSocialMedia(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}