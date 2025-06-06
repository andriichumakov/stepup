package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.stepupapp.databinding.ActivityMemoryBinding

class MemoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add a memory item manually (you can add more this way)
        addMemory(
            imageRes = R.drawable.ic_launcher_background,
            date = "2025-05-09",
            location = "Hanoi, Vietnam"
        )

        binding.btnAddPlace.setOnClickListener {
            val intent = Intent(this, AddMemoryActivity::class.java)
            startActivity(intent)
        }

        // Set up back button click listener
        binding.backButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun addMemory(imageRes: Int, date: String, location: String) {
        val context = this

        val memoryLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 48)
        }

        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(imageRes)
        }

        val dateView = TextView(context).apply {
            text = "Date: $date"
            textSize = 16f
        }

        val locationView = TextView(context).apply {
            text = "Location: $location"
            textSize = 16f
        }

        memoryLayout.addView(imageView)
        memoryLayout.addView(dateView)
        memoryLayout.addView(locationView)

        binding.memoryContainer.addView(memoryLayout)
    }
}
