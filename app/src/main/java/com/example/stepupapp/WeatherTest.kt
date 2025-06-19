package com.example.stepupapp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking

/**
 * Simple test class to verify weather functionality
 * This can be called from HomeActivity for testing purposes
 */
object WeatherTest {
    private const val TAG = "WeatherTest"
    
    fun testWeatherFunctionality(context: Context) {
        Log.d(TAG, "Starting weather functionality test")
        
        runBlocking {
            try {
                val weatherInfo = WeatherManager.getCurrentWeather(context)
                if (weatherInfo != null) {
                    Log.d(TAG, "Weather test successful:")
                    Log.d(TAG, "Temperature: ${weatherInfo.temperature}°C")
                    Log.d(TAG, "Weather Code: ${weatherInfo.weatherCode}")
                    Log.d(TAG, "Description: ${weatherInfo.weatherDescription}")
                    Log.d(TAG, "Icon Resource: ${weatherInfo.weatherIcon}")
                    
                    val message = WeatherManager.getWeatherMessage(weatherInfo.temperature, weatherInfo.weatherCode)
                    Log.d(TAG, "Weather Message: $message")
                } else {
                    Log.e(TAG, "Weather test failed: No weather data received")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Weather test failed with exception", e)
            }
        }
    }
} 