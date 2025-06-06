package com.example.stepupapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder

class LocationDetailsActivity : AppCompatActivity() {
    
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var locationName: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_details)

        // Enable back button in the toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Location Details"

        val name = intent.getStringExtra("name") ?: "Unnamed Place"
        val type = intent.getStringExtra("type") ?: "Unknown"
        val description = intent.getStringExtra("description") ?: "No description available"
        val rating = intent.getStringExtra("rating") ?: "No rating"
        val steps = intent.getStringExtra("steps") ?: ""
        val openingHours = intent.getStringExtra("openingHours") ?: ""
        
        // Get coordinates from intent
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        locationName = name

        findViewById<TextView>(R.id.locationName).text = name
        findViewById<TextView>(R.id.locationType).text = type
        findViewById<TextView>(R.id.locationDescription).text = description
        findViewById<TextView>(R.id.locationRating).text = "⭐ $rating"
        findViewById<TextView>(R.id.locationStepsAway).text = steps
        findViewById<TextView>(R.id.locationOpeningHours).text = openingHours
        
        // Set up button click listeners
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish() // Go back to previous activity
        }
        
        findViewById<Button>(R.id.btnOpenMaps).setOnClickListener {
            openLocationInMaps()
        }
    }
    
    private fun openLocationInMaps() {
        try {
            // First try: Google Maps with geo URI and coordinates
            val geoUri = Uri.parse("geo:$latitude,$longitude?q=" +
                    URLEncoder.encode(locationName, "UTF-8"))
            val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri)
            mapsIntent.setPackage("com.google.android.apps.maps")
            
            // Check if Google Maps is installed and can handle this intent
            if (mapsIntent.resolveActivity(packageManager) != null) {
                startActivity(mapsIntent)
                return
            }
            
            // Second try: Google Maps with market URI (different format)
            val marketUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude(" +
                    URLEncoder.encode(locationName, "UTF-8") + ")")
            val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
            marketIntent.setPackage("com.google.android.apps.maps")
            
            if (marketIntent.resolveActivity(packageManager) != null) {
                startActivity(marketIntent)
                return
            }
            
            // Third try: Any app that can handle maps (including web browser for Google Maps)
            val webMapsUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude(" +
                    URLEncoder.encode(locationName, "UTF-8") + ")")
            val webIntent = Intent(Intent.ACTION_VIEW, webMapsUri)
            
            if (webIntent.resolveActivity(packageManager) != null) {
                startActivity(webIntent)
                return
            }
            
            // Last resort: Fallback to Google Search
            openLocationInGoogleSearch()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open location in maps", Toast.LENGTH_SHORT).show()
            // Try Google Search as backup
            openLocationInGoogleSearch()
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

    // Handle back button press
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}