package com.example.stepupapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.stepupapp.databinding.ActivityMemoryDashboardBinding
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MemoryDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDashboardData()

        binding.btnBackToMemory.setOnClickListener {
            finish()
        }
    }

    private fun loadDashboardData() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = PlaceDatabase.getDatabase(applicationContext)
            val places = db.placeDao().getAll()

            if (places.isEmpty()) return@launch

            val dateMap = linkedMapOf<String, Int>()
            val locationMap = mutableMapOf<String, Int>()
            val lineEntries = mutableListOf<Entry>()
            val barEntries = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()

            val dateFormat = SimpleDateFormat("yy-MM-dd", Locale.getDefault())

            val sortedPlaces = places.sortedBy { dateFormat.parse(it.date_saved) }
            val firstMemory = sortedPlaces.first()
            val lastMemory = sortedPlaces.last()

            var index = 0f
            for (place in sortedPlaces) {
                val date = place.date_saved.trim()
                dateMap[date] = dateMap.getOrDefault(date, 0) + 1
                locationMap[place.name] = locationMap.getOrDefault(place.name, 0) + 1
            }

            dateMap.forEach { (date, count) ->
                labels.add(date)
                barEntries.add(BarEntry(index, count.toFloat()))
                lineEntries.add(Entry(index, count.toFloat()))
                index += 1f
            }

            val barDataSet = BarDataSet(barEntries, "Memories per Date").apply {
                color = getColor(R.color.primary_green)
                valueTextSize = 12f
            }

            val lineDataSet = LineDataSet(lineEntries, "Memory Growth").apply {
                color = getColor(R.color.gold_accent)
                valueTextSize = 12f
                circleRadius = 5f
                setCircleColor(getColor(R.color.dark_green))
            }

            val pieColors = listOf(
                getColor(R.color.primary_green),
                getColor(R.color.gold_accent),
                getColor(R.color.dark_green),
                getColor(R.color.purple_500),
                getColor(android.R.color.holo_orange_light),
                getColor(android.R.color.holo_blue_light)
            ).shuffled()

            val pieEntries = locationMap.entries.mapIndexed { i, entry ->
                PieEntry(entry.value.toFloat(), "") // Empty label to hide on pie surface
            }

            val pieDataSet = PieDataSet(pieEntries, "").apply {
                colors = pieColors
                setDrawValues(false) // No value text on pie
            }

            withContext(Dispatchers.Main) {
                binding.lineChart.apply {
                    data = LineData(lineDataSet)
                    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    xAxis.granularity = 1f
                    axisRight.isEnabled = false
                    description.isEnabled = false
                    invalidate()
                }

                binding.barChart.apply {
                    data = BarData(barDataSet)
                    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    xAxis.granularity = 1f
                    axisRight.isEnabled = false
                    description.isEnabled = false
                    invalidate()
                }

                binding.pieChart.apply {
                    data = PieData(pieDataSet)
                    description.isEnabled = false
                    setUsePercentValues(false)
                    isDrawHoleEnabled = true
                    legend.isEnabled = false // We’ll build our own legend
                    invalidate()
                }

                // Set images + names
                setRoundedImage(binding.imgFirstMemory, firstMemory.imageUri)
                setRoundedImage(binding.imgLatestMemory, lastMemory.imageUri)
                binding.tvFirstMemoryName.text = firstMemory.name
                binding.tvLatestMemoryName.text = lastMemory.name

                // Populate legend manually
                buildPieLegend(locationMap.keys.toList(), pieColors.take(locationMap.size))

                binding.tvSummary.text = "You added ${places.size} memories.\nFrom ${firstMemory.date_saved} to ${lastMemory.date_saved}"
            }
        }
    }

    private fun setRoundedImage(imageView: ImageView, uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            imageView.setImageURI(uri)
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setStroke(4, getColor(R.color.primary_green))
                cornerRadius = 16f
            }
            imageView.background = shape
            imageView.clipToOutline = true
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    private fun buildPieLegend(labels: List<String>, colors: List<Int>) {
        binding.pieLegendContainer.removeAllViews()
        for (i in labels.indices) {
            val colorBox = ViewGroup.LayoutParams(24, 24)
            val colorView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(24, 24).apply {
                    marginEnd = 8
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(colors[i])
                    cornerRadius = 6f
                }
            }

            val labelText = TextView(this).apply {
                text = labels[i]
                setTextColor(ContextCompat.getColor(this@MemoryDashboardActivity, R.color.dark_green))
                textSize = 14f
            }

            val legendItem = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 4, 8, 4)
                }
                addView(colorView)
                addView(labelText)
            }

            binding.pieLegendContainer.addView(legendItem)
        }
    }
}
