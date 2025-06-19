package com.example.stepupapp

import com.example.stepupapp.api.OpenTripMapResponse
import com.example.stepupapp.api.OpenTripMapService
import com.example.stepupapp.api.PlaceDetails

/**
 * PlaceRepository handles API calls and data management for places.
 * Responsible for fetching, cleaning, and deduplicating place data.
 */
class PlaceRepository(private val openTripMapService: OpenTripMapService) {
    private val apiKey = "5ae2e3f221c38a28845f05b66b2ebd0c0a4a7428f0803525b45f11d8"
    
    suspend fun searchPlaces(latitude: Double, longitude: Double): List<OpenTripMapResponse> {
        val places = openTripMapService.searchPlaces(
            longitude = longitude,
            latitude = latitude,
            apiKey = apiKey
        )
        
        return cleanAndDeduplicatePlaces(places)
    }
    
    suspend fun getPlaceDetails(xid: String): PlaceDetails? {
        return try {
            openTripMapService.getPlaceDetails(xid = xid, apiKey = apiKey)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun cleanAndDeduplicatePlaces(places: List<OpenTripMapResponse>): List<OpenTripMapResponse> {
        // Filter out dummy/test data
        val filteredPlaces = places.filter { place ->
            val name = place.name.lowercase()
            // Exclude Android codenames and other obvious dummy data
            !name.contains("eclair") && 
            !name.contains("marshmallow") &&
            !name.contains("lollipop") &&
            !name.contains("play music") &&
            !name.contains("google sign") &&
            !name.contains("android") &&
            !name.contains("test") &&
            name.isNotBlank() &&
            place.dist <= 15000 // Ensure within 15km
        }
        
        // Remove duplicates based on xid (unique identifier)
        val uniquePlaces = filteredPlaces.distinctBy { it.xid }
        
        // Also remove duplicates by name (in case same place has different xids)
        return uniquePlaces.groupBy { it.name.lowercase().trim() }
            .mapValues { entry -> entry.value.minByOrNull { it.dist } } // Keep closest if duplicates by name
            .values
            .filterNotNull()
            .sortedBy { it.dist } // Sort by distance (closest first)
    }
} 