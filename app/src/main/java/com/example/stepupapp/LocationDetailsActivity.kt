package com.example.stepupapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LocationDetailsActivity : AppCompatActivity() {
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

        findViewById<TextView>(R.id.locationName).text = name
        findViewById<TextView>(R.id.locationType).text = type
        findViewById<TextView>(R.id.locationDescription).text = description
        findViewById<TextView>(R.id.locationRating).text = "⭐ $rating"
        findViewById<TextView>(R.id.locationStepsAway).text = steps
        findViewById<TextView>(R.id.locationOpeningHours).text = openingHours
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