package com.example.stepupapp.filter

import com.example.stepupapp.api.OpenTripMapResponse

/**
 * Enhanced FilterManager that works with FilterState for better state management.
 * Follows the Composite pattern to handle multiple filters as a single unit.
 */
class FilterManager {
    private val filters = mutableListOf<PlaceFilter>()
    
    fun addFilter(filter: PlaceFilter) {
        filters.add(filter)
    }
    
    fun clearFilters() {
        filters.clear()
    }
    
    fun applyFilters(places: List<OpenTripMapResponse>): List<OpenTripMapResponse> {
        return places.filter { place ->
            filters.all { filter -> filter.matches(place) }
        }
    }
    
    fun getActiveFiltersDescription(): String {
        return filters.joinToString(" • ") { it.getDescription() }
    }
    
    fun hasFilters(): Boolean = filters.isNotEmpty()
    
    /**
     * Configure filters based on FilterState
     */
    fun configureFromState(filterState: FilterState) {
        clearFilters()
        
        // Always add adult content filter for family safety
        if (!filterState.includeAdultContent) {
            addFilter(AdultContentFilter())
        }
        
        // Add distance filter if specified
        if (filterState.maxDistance < 15000.0) {
            addFilter(DistanceFilter(filterState.maxDistance))
        }
        
        // Build combined interests (user interests + selected categories)
        val effectiveInterests = when {
            filterState.isUsingUserInterests && filterState.selectedCategories.isNotEmpty() -> {
                // Combine user interests with selected categories
                filterState.userInterests + filterState.selectedCategories
            }
            filterState.selectedCategories.isNotEmpty() -> {
                // Only use selected categories
                filterState.selectedCategories
            }
            filterState.isUsingUserInterests -> {
                // Only use user interests
                filterState.userInterests
            }
            else -> {
                // Show all categories
                emptySet()
            }
        }
        
        // Add interest/category filter
        if (effectiveInterests.isNotEmpty()) {
            addFilter(InterestFilter(effectiveInterests))
        }
        
        // Add subcategory filter if specified
        if (filterState.selectedSubcategory.isNotEmpty()) {
            addFilter(SubcategoryFilter(filterState.selectedSubcategory))
        }
    }
} 