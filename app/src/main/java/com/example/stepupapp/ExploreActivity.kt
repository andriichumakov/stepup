package com.example.stepupapp

import kotlin.collections.listOf
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.stepupapp.databinding.ExplorePageBinding

class ExploreActivity : AppCompatActivity() {
    private lateinit var binding: ExplorePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExplorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val hardcodedLocation = "Emmen, Netherlands"
        binding.locationText.text = "Exploring: $hardcodedLocation"

        val places = listOf(
            Location("Rensenpark", "Park", 4.4),
            Location("Wildlands Adventure Zoo", "Zoo", 4.2),
            Location("Atlas Theater", "Theater", 4.3),
            Location("Stadspark Emmerdennen", "Forest Park", 4.5),
            Location("Café Groothuis", "Bar", 4.0),
        )

        for (place in places) {
            val card = createPlaceCard(place)
            binding.placesContainer.addView(card)
        }
    }

    private fun createPlaceCard(place: Location): CardView {
        val card = CardView(this).apply {
            radius = 16f
            cardElevation = 8f
            setContentPadding(24, 24, 24, 24)
            useCompatPadding = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val nameView = TextView(this).apply {
            text = place.name
            textSize = 18f
            setPadding(0, 0, 0, 8)
        }

        val typeView = TextView(this).apply {
            text = "Type: ${place.type}"
        }

        val ratingView = TextView(this).apply {
            text = "Rating: ${place.rating} ★"
        }

        layout.addView(nameView)
        layout.addView(typeView)
        layout.addView(ratingView)

        card.addView(layout)
        return card
    }
}