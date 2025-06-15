package com.example.stepupapp.filter

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the current state of all filters and provides persistence.
 * This replaces the complex manual filter mode system with a cleaner approach.
 */
data class FilterState(
    val userInterests: Set<String> = emptySet(),
    val selectedCategories: Set<String> = emptySet(),
    val selectedSubcategory: String = "",
    val maxDistance: Double = 15000.0,
    val includeAdultContent: Boolean = false,
    val isUsingUserInterests: Boolean = true
) {
    
    fun hasActiveFilters(): Boolean {
        return selectedCategories.isNotEmpty() || 
               selectedSubcategory.isNotEmpty() || 
               maxDistance < 15000.0 ||
               !isUsingUserInterests
    }
    
    fun getActiveFiltersDescription(): String {
        val descriptions = mutableListOf<String>()
        
        if (isUsingUserInterests && userInterests.isNotEmpty() && !userInterests.contains("All")) {
            descriptions.add("Interests: ${userInterests.joinToString(", ")}")
        }
        
        if (selectedCategories.isNotEmpty()) {
            descriptions.add("Categories: ${selectedCategories.joinToString(", ")}")
        }
        
        if (selectedSubcategory.isNotEmpty()) {
            descriptions.add("Type: $selectedSubcategory")
        }
        
        if (maxDistance < 15000.0) {
            val distanceKm = (maxDistance / 1000).toInt()
            descriptions.add("Within ${distanceKm}km")
        }
        
        return if (descriptions.isEmpty()) "All places" else descriptions.joinToString(" • ")
    }
    
    companion object {
        private const val PREFS_NAME = "filter_preferences"
        private const val KEY_SELECTED_CATEGORIES = "selected_categories"
        private const val KEY_SELECTED_SUBCATEGORY = "selected_subcategory"
        private const val KEY_MAX_DISTANCE = "max_distance"
        private const val KEY_INCLUDE_ADULT_CONTENT = "include_adult_content"
        private const val KEY_USE_USER_INTERESTS = "use_user_interests"
        
        fun load(context: Context, userInterests: Set<String>): FilterState {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            return FilterState(
                userInterests = userInterests,
                selectedCategories = prefs.getStringSet(KEY_SELECTED_CATEGORIES, emptySet()) ?: emptySet(),
                selectedSubcategory = prefs.getString(KEY_SELECTED_SUBCATEGORY, "") ?: "",
                maxDistance = prefs.getFloat(KEY_MAX_DISTANCE, 15000f).toDouble(),
                includeAdultContent = prefs.getBoolean(KEY_INCLUDE_ADULT_CONTENT, false),
                isUsingUserInterests = prefs.getBoolean(KEY_USE_USER_INTERESTS, true)
            )
        }
        
        fun save(context: Context, filterState: FilterState) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putStringSet(KEY_SELECTED_CATEGORIES, filterState.selectedCategories)
                .putString(KEY_SELECTED_SUBCATEGORY, filterState.selectedSubcategory)
                .putFloat(KEY_MAX_DISTANCE, filterState.maxDistance.toFloat())
                .putBoolean(KEY_INCLUDE_ADULT_CONTENT, filterState.includeAdultContent)
                .putBoolean(KEY_USE_USER_INTERESTS, filterState.isUsingUserInterests)
                .apply()
        }
    }
}

/**
 * Predefined filter presets for quick access
 */
enum class FilterPreset(val displayName: String, val description: String) {
    ALL("All Places", "Show all places without filtering"),
    NEARBY("Nearby Only", "Places within 2km"),
    RESTAURANTS("Food & Drinks", "Restaurants, cafes, and food places"),
    CULTURE("Culture & Arts", "Museums, galleries, theaters"),
    OUTDOOR("Outdoor & Nature", "Parks, gardens, outdoor activities"),
    SHOPPING("Shopping", "Malls, shops, markets");
    
    fun toFilterState(userInterests: Set<String>): FilterState {
        return when (this) {
            ALL -> FilterState(
                userInterests = userInterests,
                isUsingUserInterests = false
            )
            NEARBY -> FilterState(
                userInterests = userInterests,
                maxDistance = 2000.0,
                isUsingUserInterests = true
            )
            RESTAURANTS -> FilterState(
                userInterests = userInterests,
                selectedCategories = setOf("Foods"),
                isUsingUserInterests = false
            )
            CULTURE -> FilterState(
                userInterests = userInterests,
                selectedCategories = setOf("Cultural", "Architecture"),
                isUsingUserInterests = false
            )
            OUTDOOR -> FilterState(
                userInterests = userInterests,
                selectedCategories = setOf("Natural"),
                isUsingUserInterests = false
            )
            SHOPPING -> FilterState(
                userInterests = userInterests,
                selectedCategories = setOf("Shops"),
                isUsingUserInterests = false
            )
        }
    }
} 