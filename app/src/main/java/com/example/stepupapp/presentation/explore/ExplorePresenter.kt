package com.example.stepupapp.presentation.explore

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.stepupapp.api.OpenTripMapResponse
import com.example.stepupapp.api.PlaceDetails
import com.example.stepupapp.filter.AdultContentFilter
import com.example.stepupapp.filter.FilterManager
import com.example.stepupapp.filter.InterestFilter
import com.example.stepupapp.filter.SubcategoryFilter
import com.example.stepupapp.PlaceRepository
// UserPreferences will be accessed through the view interface
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Presenter for the Explore feature, handling business logic and state management
 * Follows MVP pattern to separate business logic from UI
 */
class ExplorePresenter(
    private val placeRepository: PlaceRepository,
    private val filterManager: FilterManager,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    
    interface ExploreView {
        fun showLoadingState()
        fun hideLoadingState()
        fun showMiniLoadingState()
        fun showPlaces(places: List<OpenTripMapResponse>, details: Map<String, PlaceDetails?>)
        fun showEmptyState(title: String, message: String)
        fun showErrorState(title: String, message: String)
        fun updateUserInterestsDisplay(text: String)
        fun updateFilterModeUI(isManualMode: Boolean)
        fun updateClearButtonVisibility(visible: Boolean)
        fun showToast(message: String)
        fun getUserInterests(): Set<String>
    }
    
    private var view: ExploreView? = null
    private val TAG = "ExplorePresenter"
    
    // State management
    private var allPlacesList = listOf<OpenTripMapResponse>()
    private var filteredPlacesList = listOf<OpenTripMapResponse>()
    private var currentSubcategory = ""
    private var userInterests = setOf<String>()
    private var originalUserInterests = setOf<String>()
    private var manualSelectedCategory = ""
    private var isManualFilterMode = false
    private var filterJob: Job? = null
    
    fun attachView(view: ExploreView) {
        this.view = view
    }
    
    fun detachView() {
        this.view = null
        filterJob?.cancel()
    }
    
    fun initialize() {
        // Load user interests from view
        userInterests = view?.getUserInterests() ?: setOf()
        originalUserInterests = userInterests
        
        Log.d(TAG, "Initialized with user interests: $userInterests")
        updateUserInterestsDisplay()
    }
    
    fun loadPlaces(latitude: Double, longitude: Double) {
        // Cancel any ongoing operations
        filterJob?.cancel()
        
        view?.showLoadingState()
        allPlacesList = emptyList()
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading places for coordinates: lat=$latitude, lon=$longitude")
                val places = placeRepository.searchPlaces(latitude, longitude)
                
                Log.d(TAG, "Loaded ${places.size} places")
                view?.hideLoadingState()
                
                if (places.isEmpty()) {
                    view?.showEmptyState("No places found nearby", "Try adjusting your filters or location")
                    return@launch
                }
                
                allPlacesList = places
                filterPlaces()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading places: ${e.message}", e)
                view?.hideLoadingState()
                view?.showErrorState("Unable to load places", "Check your internet connection and try again")
            }
        }
    }
    
    fun toggleFilterMode() {
        // Cancel ongoing operations
        filterJob?.cancel()
        
        isManualFilterMode = !isManualFilterMode
        
        if (isManualFilterMode) {
            // Switch to manual mode
            if (manualSelectedCategory.isEmpty()) {
                manualSelectedCategory = "All" // Default category
            }
            view?.showToast("Manual filter mode enabled")
        } else {
            // Switch to personal interests mode
            manualSelectedCategory = ""
            
            // Reload fresh interests
            val freshInterests = view?.getUserInterests() ?: setOf()
            userInterests = freshInterests
            originalUserInterests = freshInterests
            
            view?.showToast("Showing places from your interests")
        }
        
        view?.updateFilterModeUI(isManualFilterMode)
        updateUserInterestsDisplay()
        updateClearButtonVisibility()
        filterPlaces()
    }
    
    fun onCategorySelected(category: String) {
        if (isManualFilterMode && category != manualSelectedCategory) {
            Log.d(TAG, "Category selected: $category")
            
            filterJob?.cancel()
            manualSelectedCategory = category
            updateClearButtonVisibility()
            filterPlaces()
        }
    }
    
    fun onSubcategoryChanged(subcategory: String) {
        currentSubcategory = subcategory
        updateClearButtonVisibility()
        filterPlaces()
    }
    
    fun clearAllFilters() {
        filterJob?.cancel()
        
        // Reset all filter states
        currentSubcategory = ""
        isManualFilterMode = false
        manualSelectedCategory = ""
        
        // Reload fresh interests
        val freshInterests = view?.getUserInterests() ?: setOf()
        userInterests = freshInterests
        originalUserInterests = freshInterests
        
        Log.d(TAG, "All filters cleared - restored to fresh interests: $userInterests")
        
        view?.updateFilterModeUI(isManualFilterMode)
        updateUserInterestsDisplay()
        filterPlaces()
        view?.showToast("All filters cleared")
    }
    
    fun onResume() {
        // Refresh user interests if not in manual mode
        if (!isManualFilterMode) {
            val newUserInterests = view?.getUserInterests() ?: setOf()
            Log.d(TAG, "onResume - Current: $userInterests, New: $newUserInterests")
            
            userInterests = newUserInterests
            originalUserInterests = newUserInterests
        }
        
        updateUserInterestsDisplay()
        
        // Refresh places if we have data
        if (allPlacesList.isNotEmpty()) {
            filterPlaces()
        }
    }
    
    fun getFilteredPlaces(): List<OpenTripMapResponse> = filteredPlacesList
    
    private fun filterPlaces() {
        if (allPlacesList.isEmpty()) return
        
        filterJob?.cancel()
        
        // Capture current state to avoid race conditions
        val currentManualMode = isManualFilterMode
        val currentManualCategory = manualSelectedCategory
        val currentUserInterests = userInterests.toSet()
        val currentSubcat = currentSubcategory
        
        Log.d(TAG, "Filtering with state - Manual: $currentManualMode, Category: $currentManualCategory, Interests: $currentUserInterests")
        
        view?.showMiniLoadingState()
        
        // Setup filters
        filterManager.clearFilters()
        filterManager.addFilter(AdultContentFilter())
        
        // Determine which interests to use
        val currentInterests = if (currentManualMode && currentManualCategory.isNotEmpty()) {
            setOf(currentManualCategory)
        } else {
            currentUserInterests
        }
        
        Log.d(TAG, "Using interests for filtering: $currentInterests")
        
        filterManager.addFilter(InterestFilter(currentInterests))
        
        if (currentSubcat.isNotEmpty()) {
            filterManager.addFilter(SubcategoryFilter(currentSubcat))
        }
        
        filterJob = lifecycleScope.launch {
            try {
                val filteredPlaces = filterManager.applyFilters(allPlacesList)
                
                // Race condition check
                if (currentManualMode != isManualFilterMode || 
                    currentManualCategory != manualSelectedCategory ||
                    currentUserInterests != userInterests ||
                    currentSubcat != currentSubcategory) {
                    Log.d(TAG, "State changed during filtering, ignoring results")
                    return@launch
                }
                
                view?.hideLoadingState()
                filteredPlacesList = filteredPlaces
                
                if (filteredPlaces.isEmpty()) {
                    val message = when {
                        currentSubcat.isNotEmpty() -> "No places found for: $currentSubcat"
                        currentInterests.isNotEmpty() && !currentInterests.contains("All") -> 
                            "No places found for your interests: ${currentInterests.joinToString(", ")}"
                        else -> "No places found nearby"
                    }
                    view?.showEmptyState("No matching places", message)
                    return@launch
                }
                
                // Load place details
                val detailsMap = mutableMapOf<String, PlaceDetails?>()
                filteredPlaces.forEach { place ->
                    try {
                        detailsMap[place.xid] = placeRepository.getPlaceDetails(place.xid)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load details for place ${place.name}: ${e.message}")
                        detailsMap[place.xid] = null
                    }
                }
                
                view?.showPlaces(filteredPlaces, detailsMap)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during filtering: ${e.message}", e)
                view?.hideLoadingState()
                view?.showErrorState("Error filtering places", "Please try again")
            }
        }
    }
    
    private fun updateUserInterestsDisplay() {
        val interestsText = when {
            isManualFilterMode -> "Manual filter mode • Showing selected category only"
            userInterests.isEmpty() || userInterests.contains("All") -> 
                "All categories • Showing places from all interests • Tap to change in Settings"
            else -> "Showing places from: ${userInterests.joinToString(", ")} • Tap to change in Settings"
        }
        
        view?.updateUserInterestsDisplay(interestsText)
    }
    
    private fun updateClearButtonVisibility() {
        val hasActiveFilters = currentSubcategory.isNotEmpty() || isManualFilterMode
        view?.updateClearButtonVisibility(hasActiveFilters)
    }
} 