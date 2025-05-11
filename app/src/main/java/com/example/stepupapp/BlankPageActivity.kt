package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.stepupapp.databinding.BlankPageWithButtonBinding

class BlankPageActivity : AppCompatActivity() {
    private lateinit var binding: BlankPageWithButtonBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BlankPageWithButtonBinding.inflate(layoutInflater)
        setContentView(binding.root)

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