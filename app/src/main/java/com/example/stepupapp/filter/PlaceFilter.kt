package com.example.stepupapp.filter

import com.example.stepupapp.api.OpenTripMapResponse

/**
 * Abstract base class for place filters following the Open/Closed Principle.
 * New filters can be added by extending this class without modifying existing code.
 */
abstract class PlaceFilter {
    abstract fun matches(place: OpenTripMapResponse): Boolean
    abstract fun getDescription(): String
}

/**
 * Filter to block adult content for family safety.
 */
class AdultContentFilter : PlaceFilter() {
    override fun matches(place: OpenTripMapResponse): Boolean {
        val kinds = place.kinds.lowercase()
        val name = place.name.lowercase()
        
        val isAdultContent = kinds.contains("adult") || 
                           kinds.contains("strip") ||
                           kinds.contains("nightclub") ||
                           kinds.contains("casino") ||
                           kinds.contains("gambling") ||
                           kinds.contains("brewery") ||
                           kinds.contains("bar") ||
                           kinds.contains("pub") ||
                           name.contains("casino") ||
                           name.contains("strip") ||
                           name.contains("adult")
        
        return !isAdultContent // Return true if NOT adult content (should be included)
    }
    
    override fun getDescription(): String = "Family Safe Content"
}

/**
 * Filter places based on user interests.
 */
class InterestFilter(private val interests: Set<String>) : PlaceFilter() {
    private val interestMapping = mapOf(
        "Amusements" to "amusements",
        "Architecture" to "architecture",
        "Cultural" to "cultural",
        "Shops" to "shops",
        "Foods" to "foods,cuisine",
        "Sport" to "sport",
        "Historical" to "historic",
        "Natural" to "natural",
        "Other" to "other"
    )
    
    override fun matches(place: OpenTripMapResponse): Boolean {
        if (interests.isEmpty() || interests.contains("All")) {
            return true // Show all if no specific interests
        }
        
        val kinds = place.kinds.lowercase()
        val interestFilters = interests.mapNotNull { interestMapping[it] }
        
        return interestFilters.any { filter ->
            filter.split(",").any { kinds.contains(it.trim()) }
        }
    }
    
    override fun getDescription(): String = "User Interests: ${interests.joinToString(", ")}"
}

/**
 * Filter places based on specific subcategories.
 */
class SubcategoryFilter(private val subcategory: String) : PlaceFilter() {
    private val subcategoryMapping = mapOf(
        "restaurant" to "restaurant",
        "cafe" to "cafe",
        "museum" to "museum",
        "park" to "park",
        "shopping mall" to "mall",
        "hotel" to "hotel",
        "church" to "church",
        "beach" to "beach",
        "monument" to "monument",
        "zoo" to "zoo",
        "aquarium" to "aquarium",
        "theatre" to "theatre",
        "castle" to "castle",
        "bridge" to "bridge",
        "tower" to "tower",
        "garden" to "garden",
        "lake" to "lake",
        "viewpoint" to "viewpoint",
        "stadium" to "stadium",
        "swimming pool" to "swimming_pool",
        "library" to "library",
        "art gallery" to "gallery",
        "cinema" to "cinema",
        "supermarket" to "supermarket",
        "hospital" to "hospital",
        "school" to "school",
        "bank" to "bank",
        "gas station" to "fuel",
        "theme park" to "amusement_park",
        "historic building" to "historic"
    )
    
    override fun matches(place: OpenTripMapResponse): Boolean {
        if (subcategory.isEmpty()) return true
        
        val kinds = place.kinds.lowercase()
        val subcategoryFilter = subcategoryMapping[subcategory.lowercase()] ?: return true
        
        return subcategoryFilter.split(",").any { kinds.contains(it.trim()) }
    }
    
    override fun getDescription(): String = if (subcategory.isEmpty()) "All Subcategories" else "Subcategory: $subcategory"
}

/**
 * Filter places based on maximum distance.
 */
class DistanceFilter(private val maxDistance: Double) : PlaceFilter() {
    override fun matches(place: OpenTripMapResponse): Boolean {
        return place.dist <= maxDistance
    }
    
    override fun getDescription(): String {
        val distanceKm = (maxDistance / 1000).toInt()
        return "Within ${distanceKm}km"
    }
} 