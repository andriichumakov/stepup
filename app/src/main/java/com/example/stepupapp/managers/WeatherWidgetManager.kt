package com.example.stepupapp.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.stepupapp.WeatherManager
import com.example.stepupapp.databinding.ActivityHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherWidgetManager(
    private val context: Context,
    private val binding: ActivityHomeBinding
) {
    
    interface WeatherUpdateCallback {
        fun onWeatherUpdated(weatherInfo: WeatherManager.WeatherInfo)
        fun onWeatherError(error: String)
    }
    
    private var callback: WeatherUpdateCallback? = null
    
    fun initialize(callback: WeatherUpdateCallback?) {
        this.callback = callback
        setupWeatherCardClick()
    }
    
    suspend fun fetchWeather() {
        try {
            Log.d("WeatherWidgetManager", "Fetching weather data...")
            val weatherInfo = withContext(Dispatchers.IO) {
                WeatherManager.getCurrentWeather(context)
            }
            
            if (weatherInfo != null) {
                updateWeatherUI(weatherInfo)
                callback?.onWeatherUpdated(weatherInfo)
                Log.d("WeatherWidgetManager", "Weather updated successfully")
            } else {
                Log.w("WeatherWidgetManager", "Failed to fetch weather data")
                callback?.onWeatherError("Failed to fetch weather data")
            }
        } catch (e: Exception) {
            Log.e("WeatherWidgetManager", "Error fetching weather", e)
            callback?.onWeatherError("Error fetching weather: ${e.message}")
        }
    }
    
    private fun updateWeatherUI(weatherInfo: WeatherManager.WeatherInfo) {
        try {
            // Update temperature
            binding.weatherTemp.text = "${weatherInfo.temperature.toInt()}°C"
            
            // Update weather icon
            binding.weatherIcon.setImageResource(weatherInfo.weatherIcon)
            
            // Update weather message
            val weatherMessage = WeatherManager.getWeatherMessage(weatherInfo.temperature, weatherInfo.weatherCode)
            binding.weatherMessage.text = weatherMessage
            
            Log.d("WeatherWidgetManager", "Weather UI updated: ${weatherInfo.temperature}°C, ${weatherInfo.weatherDescription}")
        } catch (e: Exception) {
            Log.e("WeatherWidgetManager", "Error updating weather UI", e)
        }
    }
    
    private fun setupWeatherCardClick() {
        binding.weatherCard.setOnClickListener {
            openWeatherApp()
        }
    }
    
    private fun openWeatherApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.com/weather/today"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("WeatherWidgetManager", "Error opening weather app", e)
        }
    }
} 