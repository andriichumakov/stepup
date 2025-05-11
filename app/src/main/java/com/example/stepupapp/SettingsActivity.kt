package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import com.example.stepupapp.databinding.SettingsPageBinding

class SettingsActivity : BaseActivity() {
    private lateinit var binding: SettingsPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the save button click listener
        binding.button2.setOnClickListener {
            // Create an intent to go back to BlankPageActivity
            val intent = Intent(this, BlankPageActivity::class.java)
            startActivity(intent)
            // Finish this activity so it's removed from the back stack
            finish()
        }
    }
} 