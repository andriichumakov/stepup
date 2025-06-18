package com.example.stepupapp.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeather,
    val current_units: CurrentUnits
)

data class CurrentWeather(
    val time: String,
    val temperature_2m: Double,
    val weather_code: Int
)

data class CurrentUnits(
    val temperature_2m: String,
    val weather_code: String
) 