package com.example.stepupapp

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.example.stepupapp.api.WeatherResponse
import com.example.stepupapp.api.WeatherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object WeatherManager {
    private const val TAG = "WeatherManager"
    private const val BASE_URL = "https://api.open-meteo.com/"
    
    // Default coordinates (can be updated with user's location)
    private var defaultLatitude = 52.7792
    private var defaultLongitude = 6.9069
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val weatherService = retrofit.create(WeatherService::class.java)
    
    data class WeatherInfo(
        val temperature: Double,
        val weatherCode: Int,
        val weatherDescription: String,
        val weatherIcon: Int
    )
    
    suspend fun getCurrentWeather(context: Context): WeatherInfo? {
        return try {
            val location = getCurrentLocation(context)
            val latitude = location?.latitude ?: defaultLatitude
            val longitude = location?.longitude ?: defaultLongitude
            
            Log.d(TAG, "Fetching weather for coordinates: $latitude, $longitude")
            
            val response = withContext(Dispatchers.IO) {
                weatherService.getCurrentWeather(latitude, longitude)
            }
            
            val weatherInfo = WeatherInfo(
                temperature = response.current.temperature_2m,
                weatherCode = response.current.weather_code,
                weatherDescription = getWeatherDescription(response.current.weather_code),
                weatherIcon = getWeatherIcon(response.current.weather_code)
            )
            
            Log.d(TAG, "Weather fetched successfully: ${weatherInfo.temperature}°C, ${weatherInfo.weatherDescription}")
            weatherInfo
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching weather", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather", e)
            null
        }
    }
    
    private fun getCurrentLocation(context: Context): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // Check if GPS or network provider is available
            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            
            if (!isGPSEnabled && !isNetworkEnabled) {
                Log.w(TAG, "Location providers are disabled")
                return null
            }
            
            // Try to get last known location from network provider first (faster)
            if (isNetworkEnabled) {
                val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (networkLocation != null) {
                    Log.d(TAG, "Using network location: ${networkLocation.latitude}, ${networkLocation.longitude}")
                    return networkLocation
                }
            }
            
            // Fall back to GPS provider
            if (isGPSEnabled) {
                val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (gpsLocation != null) {
                    Log.d(TAG, "Using GPS location: ${gpsLocation.latitude}, ${gpsLocation.longitude}")
                    return gpsLocation
                }
            }
            
            Log.w(TAG, "No location available, using default coordinates")
            null
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }
    
    private fun getWeatherDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
    
    private fun getWeatherIcon(weatherCode: Int): Int {
        return when (weatherCode) {
            0 -> R.drawable.weather_sunny
            1, 2, 3 -> R.drawable.weather_partly_cloudy
            45, 48 -> R.drawable.weather_foggy
            51, 53, 55, 56, 57 -> R.drawable.weather_drizzle
            61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.weather_rain
            71, 73, 75, 77, 85, 86 -> R.drawable.weather_snow
            95, 96, 99 -> R.drawable.weather_thunderstorm
            else -> R.drawable.weather_cloud
        }
    }
    
    fun getWeatherMessage(temperature: Double, weatherCode: Int): String {
        return when {
            weatherCode in listOf(61, 63, 65, 66, 67, 80, 81, 82) -> "You should take an umbrella"
            weatherCode in listOf(71, 73, 75, 77, 85, 86) -> "Bundle up, it's snowing!"
            weatherCode in listOf(95, 96, 99) -> "Stay indoors, thunderstorm ahead"
            temperature < 0 -> "It's freezing! Stay warm"
            temperature < 10 -> "It's cold, wear a jacket"
            temperature < 20 -> "Nice weather for a walk"
            temperature < 30 -> "Perfect weather for outdoor activities"
            else -> "It's hot! Stay hydrated"
        }
    }
} 