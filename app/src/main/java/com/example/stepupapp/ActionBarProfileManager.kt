package com.example.stepupapp

import android.app.Activity
import android.content.Intent
import android.widget.ImageView

class ActionBarProfileManager(private val activity: Activity) {
    
    fun updateProfilePicture() {
        val profileImageView = activity.findViewById<ImageView>(R.id.actionbar_profile_picture)
        
        if (profileImageView != null) {
            // Use ProfilePictureLoader to load the profile picture
            ProfilePictureLoader.loadProfilePicture(
                context = activity,
                imageView = profileImageView,
                showDefault = true
            )
            
            // Add click listener to take user to settings
            profileImageView.setOnClickListener {
                val intent = Intent(activity, SettingsActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }
} 