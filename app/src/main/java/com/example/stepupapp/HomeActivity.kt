package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import com.example.stepupapp.databinding.ActivityHomeBinding

class HomeActivity : BaseActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hardcoded example
        val steps = 3727
        val target = 6000
        val distance = 2.94 // in km
        val calories = 119 // in kcal

        binding.stepCountText.text = "$steps steps"
        binding.stepProgressBar.max = target
        binding.stepProgressBar.progress = steps
        binding.distanceText.text = "$distance km"
        binding.caloriesText.text = "$calories Cal"

        binding.imageButton3.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.imageButton4.setOnClickListener {
            val intent = Intent(this, ExploreActivity::class.java)
            startActivity(intent)
        }

    }
}
