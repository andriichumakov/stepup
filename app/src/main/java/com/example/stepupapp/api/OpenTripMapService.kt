package com.example.stepupapp.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class OpenTripMapResponse(
    val xid: String,
    val name: String,
    val dist: Double,
    val rate: Int,
    val osm: String?,
    val wikidata: String?,
    val kinds: String,
    val point: Point
)

data class Point(
    val lon: Double,
    val lat: Double
)

data class PlaceDetails(
    val xid: String,
    val name: String,
    val rate: Int,
    val osm: String?,
    val wikidata: String?,
    val kinds: String,
    val wikipedia_extracts: WikipediaExtracts?,
    val image: String?,
    val url: String?,
    val sources: Sources?,
    val otm: String?,
    val point: Point
)

data class WikipediaExtracts(
    val title: String,
    val text: String,
    val html: String
)

data class Sources(
    val geometry: String,
    val attributes: List<String>
)

interface OpenTripMapService {
    @GET("0.1/en/places/radius")
    suspend fun searchPlaces(
        @Query("radius") radius: Int = 2000,
        @Query("lon") longitude: Double,
        @Query("lat") latitude: Double,
        @Query("apikey") apiKey: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("rate") rate: Int = 1,  // Only get places with rating >= 1
        @Query("kinds") kinds: String = "interesting_places,cultural,natural,museums,theatres_and_entertainments,urban_environment,historic,architecture,religion,amusements,tourist_facilities"
    ): List<OpenTripMapResponse>

    @GET("0.1/en/places/xid/{xid}")
    suspend fun getPlaceDetails(
        @Path("xid") xid: String,
        @Query("apikey") apiKey: String
    ): PlaceDetails
} 