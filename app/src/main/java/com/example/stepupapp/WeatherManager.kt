package com.example.stepupapp

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.example.stepupapp.api.WeatherResponse
import com.example.stepupapp.api.WeatherService
import com.example.stepupapp.api.ForecastResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object WeatherManager {
    private const val TAG = "WeatherManager"
    private const val BASE_URL = "https://api.open-meteo.com/"

    // Default location if user location is unavailable
    private var defaultLatitude = 52.7792
    private var defaultLongitude = 6.9069

    // Retrofit instance to connect to Open-Meteo API
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherService = retrofit.create(WeatherService::class.java)

    // Used to display current weather in the UI
    data class WeatherInfo(
        val temperature: Double,
        val weatherCode: Int,
        val weatherDescription: String,
        val weatherIcon: Int
    )

    // Used to display 2-day forecast
    data class ForecastInfo(
        val date: String,
        val minTemp: Double,
        val maxTemp: Double,
        val weatherCode: Int,
        val weatherDescription: String,
        val clothingSuggestion: String
    )

    // Fetches current weather from API
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

    // Fetches the 3-day forecast and clothing suggestions
    suspend fun getThreeDayForecast(context: Context): List<ForecastInfo> {
        return try {
            val location = getCurrentLocation(context)
            val lat = location?.latitude ?: defaultLatitude
            val lon = location?.longitude ?: defaultLongitude

            val forecast = withContext(Dispatchers.IO) {
                weatherService.getWeatherForecast(lat, lon)
            }

            forecast.daily.time.indices.take(3).map { i ->
                val code = forecast.daily.weathercode[i]
                ForecastInfo(
                    date = forecast.daily.time[i],
                    minTemp = forecast.daily.temperature_2m_min[i],
                    maxTemp = forecast.daily.temperature_2m_max[i],
                    weatherCode = code,
                    weatherDescription = getWeatherDescription(code),
                    clothingSuggestion = getClothingSuggestion(forecast.daily.temperature_2m_max[i], code)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Forecast fetch error", e)
            emptyList()
        }
    }

    // Gets user location if permission granted, otherwise uses default
    private fun getCurrentLocation(context: Context): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGPSEnabled && !isNetworkEnabled) {
                Log.w(TAG, "Location providers are disabled")
                return null
            }

            if (isNetworkEnabled) {
                val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (networkLocation != null) {
                    Log.d(TAG, "Using network location: ${networkLocation.latitude}, ${networkLocation.longitude}")
                    return networkLocation
                }
            }

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

    // Converts weather code into readable description
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

    // Chooses appropriate weather icon
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

    // Simple clothing recommendation based on temperature & weather code
    fun getClothingSuggestion(temp: Double, code: Int): String {
        return when {
            code in listOf(61, 63, 65, 80, 81, 82) -> "Rain expected – wear waterproof clothes."
            code in listOf(71, 73, 75, 77, 85, 86) -> "Snowy – winter jacket and boots needed."
            temp < 0 -> "Freezing – wear thermal layers."
            temp < 10 -> "Cold – wear a coat or jacket."
            temp < 20 -> "Mild – a hoodie or light jacket is fine."
            temp < 30 -> "Warm – t-shirt weather."
            else -> "Hot – wear light clothes and drink water."
        }
    }

    // Message displayed under the temperature
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