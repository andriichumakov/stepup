package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.stepupapp.databinding.SettingsPageBinding

class SettingsActivity : BaseActivity() {
    private lateinit var binding: SettingsPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load current step target
        val currentTarget = UserPreferences.getStepTarget(this)
        binding.editTextNumber2.setText(currentTarget.toString())

        // Set up the save button click listener
        binding.button2.setOnClickListener {
            try {
                val newTarget = binding.editTextNumber2.text.toString().toInt()
                if (newTarget > 0) {
                    UserPreferences.setStepTarget(this, newTarget)
                    Toast.makeText(this, "Step target updated successfully", Toast.LENGTH_SHORT).show()

                    // Go back to home activity
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Please enter a valid step target", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 