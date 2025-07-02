package com.example.stepupapp.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.stepupapp.WeatherManager
import com.example.stepupapp.databinding.ActivityHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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
                fetchForecastAndUpdateUI()
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
            binding.weatherTemp.text = "${weatherInfo.temperature.toInt()}°C"
            binding.weatherIcon.setImageResource(weatherInfo.weatherIcon)

            val weatherMessage = WeatherManager.getWeatherMessage(
                weatherInfo.temperature,
                weatherInfo.weatherCode
            )
            binding.weatherMessage.text = weatherMessage

            Log.d("WeatherWidgetManager", "Weather UI updated: ${weatherInfo.temperature}°C, ${weatherInfo.weatherDescription}")
        } catch (e: Exception) {
            Log.e("WeatherWidgetManager", "Error updating weather UI", e)
        }
    }

    suspend fun fetchForecastAndUpdateUI() {
        try {
            val forecastList = withContext(Dispatchers.IO) {
                WeatherManager.getThreeDayForecast(context)
            }

            if (forecastList.isNotEmpty()) {
                val styledText = android.text.SpannableStringBuilder()

                for ((index, forecast) in forecastList.withIndex()) {
                    // Date (with weekday)
                    val formattedDate = formatDateToWeekday(forecast.date)
                    val dateStart = styledText.length
                    styledText.append("$formattedDate:\n")
                    styledText.setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        dateStart,
                        styledText.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // Weather description
                    styledText.append("   ${forecast.weatherDescription}, ${forecast.minTemp}°C–${forecast.maxTemp}°C\n")

                    // Clothing suggestion
                    styledText.append("   ${forecast.clothingSuggestion}\n")

                    // Divider after first day
                    if (index == 0) {
                        styledText.append("\n")
                        val dividerStart = styledText.length
                        styledText.append("―――――――――――――――――――――――――――――――\n\n")
                        styledText.setSpan(
                            android.text.style.ForegroundColorSpan(android.graphics.Color.WHITE),
                            dividerStart,
                            styledText.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    } else {
                        styledText.append("\n")
                    }
                }

                binding.forecastTextView.text = styledText
            } else {
                binding.forecastTextView.text = "Forecast unavailable"
            }
        } catch (e: Exception) {
            Log.e("WeatherWidgetManager", "Forecast error: ${e.message}")
            binding.forecastTextView.text = "Error loading forecast"
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

    private fun formatDateToWeekday(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val forecastDate = inputFormat.parse(dateString)

            val today = Calendar.getInstance()
            val forecastCal = Calendar.getInstance().apply { time = forecastDate }

            val isToday = today.get(Calendar.YEAR) == forecastCal.get(Calendar.YEAR)
                    && today.get(Calendar.DAY_OF_YEAR) == forecastCal.get(Calendar.DAY_OF_YEAR)

            return if (isToday) {
                "Today, $dateString"
            } else {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                "${dayFormat.format(forecastDate)}, $dateString"
            }
        } catch (e: Exception) {
            dateString
        }
    }
}
