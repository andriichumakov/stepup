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

        // Set text content
        findViewById<TextView>(R.id.locationName).text = name
        findViewById<TextView>(R.id.locationType).text = type
        findViewById<TextView>(R.id.locationDescription).text = description
        findViewById<TextView>(R.id.locationRating).text = "⭐ $rating"
        findViewById<TextView>(R.id.locationStepsAway).text = steps
        findViewById<TextView>(R.id.locationOpeningHours).text = openingHours

        // Show 'No image available' text
        findViewById<TextView>(R.id.noImageText).visibility = View.VISIBLE

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
            if (latitude == 0.0 && longitude == 0.0) {
                Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show()
                return
            }

            val mapsUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=w")
            val mapIntent = Intent(Intent.ACTION_VIEW, mapsUri)
            startActivity(mapIntent)

        } catch (e: Exception) {
            Log.e("LocationDetails", "Error opening maps: ${e.message}")
            Toast.makeText(this, "Unable to open maps", Toast.LENGTH_SHORT).show()
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