package com.example.stepupapp.api

import retrofit2.http.GET
import retrofit2.http.Query

data class TripAdvisorResponse(
    val data: List<Place>
)

data class Place(
    val location_id: String,
    val name: String,
    val category: Category,
    val rating: Double,
    val num_reviews: Int,
    val photo: Photo?,
    val address_string: String
)

data class Category(
    val name: String
)

data class Photo(
    val images: Images
)

data class Images(
    val small: ImageUrl,
    val medium: ImageUrl,
    val large: ImageUrl
)

data class ImageUrl(
    val url: String
)

interface TripAdvisorService {
    @GET("locations/search")
    suspend fun searchLocations(
        @Query("location_string") location: String,
        @Query("key") apiKey: String
    ): TripAdvisorResponse
} 