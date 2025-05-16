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
            useCompatPadding = true
            setCardBackgroundColor(android.graphics.Color.parseColor("#F0BB78"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24) // spacing between cards
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24) // move this from card to here
        }

        val nameView = TextView(this).apply {
            text = place.name
            textSize = 18f
            setTextColor(android.graphics.Color.parseColor("#1f823a"))
            setPadding(0, 0, 0, 8)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val typeView = TextView(this).apply {
            text = "Type: ${place.type}"
            setTextColor(android.graphics.Color.parseColor("#1f823a"))
        }

        val ratingView = TextView(this).apply {
            text = "Rating: ${place.rating} ★"
            setTextColor(android.graphics.Color.parseColor("#1f823a"))
        }

        layout.addView(nameView)
        layout.addView(typeView)
        layout.addView(ratingView)

        card.addView(layout)
        return card
    }

}