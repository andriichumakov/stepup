package com.example.stepupapp.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import com.example.stepupapp.MemoryActivity
import com.example.stepupapp.Place
import com.example.stepupapp.PlaceDatabase
import com.example.stepupapp.R
import com.example.stepupapp.UserPreferences
import com.example.stepupapp.databinding.ActivityHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemoryWidgetManager(
    private val context: Context,
    private val binding: ActivityHomeBinding
) {
    
    interface MemoryUpdateCallback {
        fun onNewMemoryDetected(place: Place)
        fun onMemoryWidgetUpdated()
    }
    
    private var callback: MemoryUpdateCallback? = null
    
    fun initialize(callback: MemoryUpdateCallback?) {
        this.callback = callback
        setupMemoryNavigation()
    }
    
    suspend fun updateMemoriesWidget() {
        val db = PlaceDatabase.getDatabase(context)
        val latestMemory = db.placeDao().getLatestPlace()
        withContext(Dispatchers.Main) {
            if (latestMemory == null) {
                showNoMemoriesState()
            } else {
                showMemoryData(latestMemory)
            }
            callback?.onMemoryWidgetUpdated()
        }
    }
    
    suspend fun checkAndNotifyNewMemory() {
        val db = PlaceDatabase.getDatabase(context)
        val latestMemory = db.placeDao().getLatestPlace()
        latestMemory?.let { place ->
            val lastNotifiedId = UserPreferences.getLastMemoryId(context)
            if (place.id != lastNotifiedId) {
                UserPreferences.setLastMemoryId(context, place.id)
                withContext(Dispatchers.Main) {
                    callback?.onNewMemoryDetected(place)
                }
            }
        }
    }
    
    private fun showNoMemoriesState() {
        binding.memoryText.text = "No memories yet. Add your first memory!"
        binding.memoryImage.setImageResource(R.drawable.memory_zoo)
        binding.memoryDate.text = ""
        binding.memorySteps.text = ""
        setMemoryStars(0f)
    }
    
    private fun showMemoryData(memory: Place) {
        try {
            val uri = Uri.parse(memory.imageUri)
            binding.memoryImage.setImageURI(uri)
        } catch (e: Exception) {
            binding.memoryImage.setImageResource(R.drawable.memory_zoo)
        }
        binding.memoryDate.text = memory.date_saved
        binding.memoryText.text = memory.description.ifBlank { memory.name }
        binding.memorySteps.text = memory.steps_taken + " steps"
        setMemoryStars(memory.rating)
    }
    
    private fun setMemoryStars(rating: Float) {
        val starIds = listOf(
            R.id.memoryStar1,
            R.id.memoryStar2,
            R.id.memoryStar3,
            R.id.memoryStar4,
            R.id.memoryStar5
        )
        for (i in 0 until 5) {
            val starView = binding.root.findViewById<ImageView>(starIds[i])
            when {
                rating >= i + 1 -> {
                    starView.setImageResource(R.drawable.ic_star)
                    starView.setColorFilter(0xFFFFD700.toInt()) // Gold
                }
                rating > i && rating < i + 1 -> {
                    starView.setImageResource(R.drawable.ic_star)
                    starView.setColorFilter(0x80FFD700.toInt()) // Faded gold
                }
                else -> {
                    starView.setImageResource(R.drawable.ic_star)
                    starView.setColorFilter(0x33FFD700) // More faded
                }
            }
        }
    }
    
    private fun setupMemoryNavigation() {
        val memoryClickListener = {
            openMemoryActivity()
        }
        
        binding.imageButtonMemory.setOnClickListener { memoryClickListener() }
        binding.memoriesCard.setOnClickListener { memoryClickListener() }
    }
    
    private fun openMemoryActivity() {
        val intent = Intent(context, MemoryActivity::class.java)
        val currentSteps = getCurrentStepsFromBinding()
        intent.putExtra("currentSteps", currentSteps)
        context.startActivity(intent)
    }
    
    fun openMemoryActivityWithHighlight(memoryId: Long) {
        val intent = Intent(context, MemoryActivity::class.java).apply {
            putExtra("highlightMemoryId", memoryId)
        }
        context.startActivity(intent)
    }
    
    private fun getCurrentStepsFromBinding(): Int {
        return binding.stepCountText.text.toString().split(" ")[0].toIntOrNull() ?: 0
    }
} 