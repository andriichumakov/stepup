package com.example.stepupapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class LocationDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_details)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Location Details"

        // Get data from intent
        val name = intent.getStringExtra("name") ?: ""
        val type = intent.getStringExtra("type") ?: ""
        val openingHours = intent.getStringExtra("openingHours") ?: ""
        val description = intent.getStringExtra("description") ?: "No description available"

        findViewById<TextView>(R.id.locationName).text = name
        findViewById<TextView>(R.id.locationType).text = type
        findViewById<TextView>(R.id.locationOpeningHours).text = openingHours
        findViewById<TextView>(R.id.locationDescription).text = description
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 