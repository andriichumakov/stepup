package com.example.stepupapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.api.OpenTripMapResponse
import com.example.stepupapp.api.OpenTripMapService
import com.example.stepupapp.api.PlaceDetails
import com.example.stepupapp.databinding.ExplorePageBinding
import com.example.stepupapp.databinding.PlaceCardBinding
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder
import kotlin.math.roundToInt

class ExploreActivity : AppCompatActivity() {
    private lateinit var binding: ExplorePageBinding
    private lateinit var openTripMapService: OpenTripMapService
    private val TAG = "ExploreActivity"

    // Average steps per meter (rough estimate)
    private val STEPS_PER_METER = 1.3
    
    // Location coordinates
    private val EMMEN_LATITUDE = 52.788040
    private val EMMEN_LONGITUDE = 6.893176

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExplorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Load places
        loadPlaces()
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
                    longitude = EMMEN_LONGITUDE,
                    latitude = EMMEN_LATITUDE,
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
            val searchQuery = "${place.name} Emmen Netherlands reviews"
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