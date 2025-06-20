package com.example.stepupapp.managers

import android.app.Activity
import android.content.Intent
import com.example.stepupapp.AuthOptionsActivity
import com.example.stepupapp.HomeActivity
import com.example.stepupapp.services.ProfileService

class SetupNavigationManager(private val activity: Activity) {
    
    suspend fun checkAuthenticationAndNavigate(): Boolean {
        return if (!ProfileService.isSignedIn()) {
            navigateToAuth()
            true // Navigation handled
        } else if (ProfileService.hasSetStepGoal()) {
            navigateToHome()
            true // Navigation handled
        } else {
            false // Continue with setup
        }
    }
    
    fun navigateToHome() {
        activity.startActivity(Intent(activity, HomeActivity::class.java))
        activity.finish()
    }
    
    private fun navigateToAuth() {
        activity.startActivity(Intent(activity, AuthOptionsActivity::class.java))
        activity.finish()
    }
} 