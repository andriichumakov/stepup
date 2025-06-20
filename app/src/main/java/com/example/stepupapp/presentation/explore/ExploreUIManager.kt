package com.example.stepupapp.presentation.explore

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.stepupapp.LocationDetailsActivity
import com.example.stepupapp.R
import com.example.stepupapp.SettingsActivity
import com.example.stepupapp.api.OpenTripMapResponse
import com.example.stepupapp.api.PlaceDetails
import com.example.stepupapp.databinding.ExplorePageBinding
import com.example.stepupapp.databinding.PlaceCardBinding
import kotlin.math.roundToInt

/**
 * UI Manager for the Explore feature
 * Handles all UI setup, interactions, and card creation
 */
class ExploreUIManager(
    private val context: Context,
    private val binding: ExplorePageBinding
) {
    
    interface UIManagerListener {
        fun onCategorySelected(category: String)
        fun onSubcategoryChanged(subcategory: String)
        fun onToggleFilterClicked()
        fun onClearAllFiltersClicked()
        fun onMapToggleClicked()
        fun onCenterLocationClicked()
        fun onClearRouteClicked()
        fun onLocationCardClicked()
        fun onInterestsCardClicked()
        fun onRetryClicked()
        fun onExpandSearchClicked()
        fun onClearFiltersFromEmptyStateClicked()
    }
    
    private var listener: UIManagerListener? = null
    private val STEP_LENGTH = 0.50 // Average step length in meters
    
    fun setListener(listener: UIManagerListener) {
        this.listener = listener
    }
    
    fun setupUI() {
        setupCategorySpinner()
        setupSubcategorySearch()
        setupToggleButton()
        setupClearAllButton()
        setupMapToggle()
        setupCenterLocationButton()
        setupLocationCard()
        setupInterestsCard()
    }
    
    fun showLoadingState() {
        binding.placesContainer.removeAllViews()
        
        // Add skeleton cards
        repeat(5) {
            val skeletonView = context.layoutInflater.inflate(R.layout.skeleton_place_card, binding.placesContainer, false)
            binding.placesContainer.addView(skeletonView)
        }
    }
    
    fun hideLoadingState() {
        binding.placesContainer.removeAllViews()
    }
    
    fun showMiniLoadingState() {
        binding.placesContainer.removeAllViews()
        
        // Add fewer skeleton cards for filtering
        repeat(3) {
            val skeletonView = context.layoutInflater.inflate(R.layout.skeleton_place_card, binding.placesContainer, false)
            binding.placesContainer.addView(skeletonView)
        }
    }
    
    fun showPlaces(places: List<OpenTripMapResponse>, details: Map<String, PlaceDetails?>) {
        binding.placesContainer.removeAllViews()
        
        places.forEach { place ->
            val placeDetails = details[place.xid]
            createPlaceCard(place, placeDetails)
        }
    }
    
    fun showEmptyState(title: String, message: String) {
        binding.placesContainer.removeAllViews()
        
        val emptyStateView = context.layoutInflater.inflate(R.layout.empty_state_places, binding.placesContainer, false)
        
        emptyStateView.findViewById<TextView>(R.id.emptyTitle)?.text = title
        emptyStateView.findViewById<TextView>(R.id.emptyMessage)?.text = message
        
        emptyStateView.findViewById<com.google.android.material.button.MaterialButton>(R.id.expandSearchButton)?.setOnClickListener {
            listener?.onExpandSearchClicked()
        }
        
        emptyStateView.findViewById<com.google.android.material.button.MaterialButton>(R.id.clearFiltersButton)?.setOnClickListener {
            listener?.onClearFiltersFromEmptyStateClicked()
        }
        
        binding.placesContainer.addView(emptyStateView)
    }
    
    fun showErrorState(title: String, message: String) {
        binding.placesContainer.removeAllViews()
        
        val errorStateView = context.layoutInflater.inflate(R.layout.error_state_places, binding.placesContainer, false)
        
        errorStateView.findViewById<TextView>(R.id.errorTitle)?.text = title
        errorStateView.findViewById<TextView>(R.id.errorMessage)?.text = message
        errorStateView.findViewById<com.google.android.material.button.MaterialButton>(R.id.retryButton)?.setOnClickListener {
            listener?.onRetryClicked()
        }
        
        binding.placesContainer.addView(errorStateView)
    }
    
    fun updateUserInterestsDisplay(text: String) {
        binding.interestsText.text = text
    }
    
    fun updateFilterModeUI(isManualMode: Boolean) {
        if (isManualMode) {
            binding.categorySpinnerCard.visibility = View.VISIBLE
            binding.toggleFilterButton.text = "Use My Interests"
            binding.toggleFilterButton.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.primary_green))
            binding.clearAllFiltersButton.visibility = View.VISIBLE
        } else {
            binding.categorySpinnerCard.visibility = View.GONE
            binding.toggleFilterButton.text = "Manual Filter"
            binding.toggleFilterButton.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.dark_blue))
        }
    }
    
    fun updateClearButtonVisibility(visible: Boolean) {
        binding.clearAllFiltersButton.visibility = if (visible) View.VISIBLE else View.GONE
    }
    
    fun updateLocationUI(locationName: String, showProgress: Boolean) {
        binding.locationText.text = locationName
        binding.locationProgressBar.visibility = if (showProgress) View.VISIBLE else View.GONE
    }
    
    fun updateMapUI(isMapViewActive: Boolean) {
        if (isMapViewActive) {
            binding.mapContainer.visibility = View.VISIBLE
            binding.placesContainer.visibility = View.GONE
            binding.mapToggleButton.text = "📋 List"
            binding.mapToggleButton.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.dark_blue))
        } else {
            binding.mapContainer.visibility = View.GONE
            binding.placesContainer.visibility = View.VISIBLE
            binding.mapToggleButton.text = "🗺️ Map"
            binding.mapToggleButton.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.primary_green))
        }
    }
    
    fun showClearRouteButton(show: Boolean) {
        binding.clearRouteFab.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    fun clearSubcategorySearch() {
        binding.subcategorySearch.setText("")
        binding.subcategorySearch.clearFocus()
    }
    
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    fun showLongToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter.createFromResource(
            context,
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
                    setTextColor(context.getColor(android.R.color.black))
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                }
                
                listener?.onCategorySelected(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    
    private fun setupSubcategorySearch() {
        // Set up autocomplete
        val subcategories = context.resources.getStringArray(R.array.all_subcategories)
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, subcategories)
        binding.subcategorySearch.setAdapter(adapter)
        
        // Handle text changes
        binding.subcategorySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                listener?.onSubcategoryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Handle item selection
        binding.subcategorySearch.setOnItemClickListener { _, _, _, _ ->
            binding.subcategorySearch.clearFocus()
        }
        
        // Set up clear button
        binding.clearSearchButton.setOnClickListener {
            clearSubcategorySearch()
            listener?.onSubcategoryChanged("")
        }
    }
    
    private fun setupToggleButton() {
        binding.toggleFilterButton.setOnClickListener {
            binding.toggleFilterButton.isEnabled = false
            
            binding.toggleFilterButton.postDelayed({
                listener?.onToggleFilterClicked()
                binding.toggleFilterButton.isEnabled = true
            }, 300)
        }
    }
    
    private fun setupClearAllButton() {
        binding.clearAllFiltersButton.setOnClickListener {
            listener?.onClearAllFiltersClicked()
        }
    }
    
    private fun setupMapToggle() {
        binding.mapToggleButton.setOnClickListener {
            listener?.onMapToggleClicked()
        }
    }
    
    private fun setupCenterLocationButton() {
        binding.centerLocationFab.setOnClickListener {
            listener?.onCenterLocationClicked()
        }
        
        binding.clearRouteFab.setOnClickListener {
            listener?.onClearRouteClicked()
        }
    }
    
    private fun setupLocationCard() {
        binding.locationCard.setOnClickListener {
            listener?.onLocationCardClicked()
        }
    }
    
    private fun setupInterestsCard() {
        binding.interestsCard.setOnClickListener {
            listener?.onInterestsCardClicked()
        }
    }
    
    private fun createPlaceCard(place: OpenTripMapResponse, details: PlaceDetails?) {
        val cardBinding = PlaceCardBinding.inflate(context.layoutInflater)

        val name = place.name.ifEmpty { "Unnamed Place" }
        val type = place.kinds.split(",").firstOrNull()?.replace("_", " ") ?: "Unknown"
        val rating = (2..9).random()
        val distanceInMeters = if (place.dist < 50) (place.dist * 1000).roundToInt() else place.dist.roundToInt()
        val steps = (distanceInMeters / STEP_LENGTH).roundToInt()
        val stepsText = "$steps steps away"

        cardBinding.placeName.text = name
        cardBinding.placeType.text = type
        cardBinding.placeRating.text = "⭐ $rating/10"
        cardBinding.placeAddress.text = stepsText

        // Set click listener
        cardBinding.root.setOnClickListener {
            openLocationDetails(name, type, details, rating, stepsText, place)
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 16)
        cardBinding.root.layoutParams = layoutParams

        binding.placesContainer.addView(cardBinding.root)
    }
    
    private fun openLocationDetails(
        name: String,
        type: String,
        details: PlaceDetails?,
        rating: Int,
        stepsText: String,
        place: OpenTripMapResponse
    ) {
        try {
            val intent = Intent(context, LocationDetailsActivity::class.java).apply {
                putExtra("name", name)
                putExtra("type", type)
                putExtra("description", details?.wikipedia_extracts?.text ?: "No description available for this location.")
                putExtra("rating", "$rating/10")
                putExtra("steps", stepsText)
                putExtra("openingHours", details?.sources?.opening_hours ?: "Opening hours not available")
                putExtra("latitude", place.point.lat)
                putExtra("longitude", place.point.lon)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Error opening location details")
        }
    }
    
    private val Context.layoutInflater: android.view.LayoutInflater
        get() = android.view.LayoutInflater.from(this)
} 