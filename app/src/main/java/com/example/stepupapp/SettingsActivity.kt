package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.stepupapp.databinding.SettingsPageBinding

class SettingsActivity : BaseActivity() {
    private lateinit var binding: SettingsPageBinding
    private lateinit var actionBarLocationManager: ActionBarLocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize and setup ActionBar location
        actionBarLocationManager = ActionBarLocationManager(this)
        actionBarLocationManager.setupActionBarLocation()

        // Load current step target
        val currentTarget = UserPreferences.getStepTarget(this)
        binding.editTextNumber2.setText(currentTarget.toString())

        // Load current interests
        loadCurrentInterests()

        // Set up the save button click listener
        binding.button2.setOnClickListener {
            try {
                val newTarget = binding.editTextNumber2.text.toString().toInt()
                if (newTarget > 0) {
                    UserPreferences.setStepTarget(this, newTarget)
                    
                    // Save interests
                    saveUserInterests()
                    
                    Toast.makeText(this, "Settings updated successfully", Toast.LENGTH_SHORT).show()

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

        // Set up the back button click listener
        binding.backButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadCurrentInterests() {
        val currentInterests = UserPreferences.getUserInterests(this)
        
        binding.checkBox10.isChecked = currentInterests.contains("Amusements")
        binding.checkBox11.isChecked = currentInterests.contains("Architecture")
        binding.checkBox12.isChecked = currentInterests.contains("Cultural")
        binding.checkBox14.isChecked = currentInterests.contains("Shops")
        binding.checkBox16.isChecked = currentInterests.contains("Foods")
        binding.checkBox18.isChecked = currentInterests.contains("Sport")
        binding.checkBox21.isChecked = currentInterests.contains("Historical")
        binding.checkBox20.isChecked = currentInterests.contains("Natural")
        binding.checkBox22.isChecked = currentInterests.contains("Other")
    }

    private fun saveUserInterests() {
        val selectedInterests = mutableSetOf<String>()
        
        if (binding.checkBox10.isChecked) selectedInterests.add("Amusements")
        if (binding.checkBox11.isChecked) selectedInterests.add("Architecture")
        if (binding.checkBox12.isChecked) selectedInterests.add("Cultural")
        if (binding.checkBox14.isChecked) selectedInterests.add("Shops")
        if (binding.checkBox16.isChecked) selectedInterests.add("Foods")
        if (binding.checkBox18.isChecked) selectedInterests.add("Sport")
        if (binding.checkBox21.isChecked) selectedInterests.add("Historical")
        if (binding.checkBox20.isChecked) selectedInterests.add("Natural")
        if (binding.checkBox22.isChecked) selectedInterests.add("Other")
        
        // If no interests selected, default to showing all
        if (selectedInterests.isEmpty()) {
            selectedInterests.add("All")
        }
        
        UserPreferences.saveUserInterests(this, selectedInterests)
    }

    override fun onDestroy() {
        super.onDestroy()
        actionBarLocationManager.onDestroy()
    }
} 