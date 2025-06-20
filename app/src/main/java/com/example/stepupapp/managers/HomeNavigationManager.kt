package com.example.stepupapp.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.stepupapp.ExploreActivity
import com.example.stepupapp.SettingsActivity
import com.example.stepupapp.StepsOverviewActivity
import com.example.stepupapp.databinding.ActivityHomeBinding

class HomeNavigationManager(
    private val context: Context,
    private val binding: ActivityHomeBinding
) {
    
    fun initialize() {
        setupNavigationButtons()
    }
    
    private fun setupNavigationButtons() {
        binding.imageButton3.setOnClickListener {
            openSettings()
        }

        binding.imageButton4.setOnClickListener {
            openExplore()
        }

        binding.historyButton.setOnClickListener {
            openStepsOverview()
        }
    }
    
    private fun openSettings() {
        try {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("HomeNavigationManager", "Error opening settings", e)
            showNavigationError("Error opening settings")
        }
    }
    
    private fun openExplore() {
        try {
            val intent = Intent(context, ExploreActivity::class.java)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("HomeNavigationManager", "Error opening explore", e)
            showNavigationError("Error opening explore")
        }
    }
    
    private fun openStepsOverview() {
        try {
            Log.d("HomeNavigationManager", "History button clicked, starting StepsOverviewActivity")
            val intent = Intent(context, StepsOverviewActivity::class.java)
            context.startActivity(intent)
            Log.d("HomeNavigationManager", "StepsOverviewActivity started successfully")
        } catch (e: Exception) {
            Log.e("HomeNavigationManager", "Error starting StepsOverviewActivity", e)
            showNavigationError("Error opening steps overview")
        }
    }
    
    private fun showNavigationError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
} 