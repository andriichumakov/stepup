package com.example.stepupapp

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.example.stepupapp.R

/**
 * Base activity class that all activities should extend to apply the custom ActionBar
 */
open class BaseActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
    }
    
    private fun setupActionBar() {
        // Hide the default title
        supportActionBar?.let { actionBar ->
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayShowCustomEnabled(true)
            
            // Set the custom layout
            val customView = LayoutInflater.from(this).inflate(R.layout.actionbar_custom, null)
            val layoutParams = ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
            )
            actionBar.setCustomView(customView, layoutParams)
        }
    }
} 