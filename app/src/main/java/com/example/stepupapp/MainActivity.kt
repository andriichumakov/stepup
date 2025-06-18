package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.stepupapp.databinding.SetupPageBinding
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {
    private lateinit var binding: SetupPageBinding
    private var hasUserSetTarget = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            if (!ProfileService.isSignedIn()) {
                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@MainActivity, AuthOptionsActivity::class.java))
                    finish()
                    return@withContext
                }
            }

            // Get current user's profile
            val profile = ProfileService.getCurrentProfile(this@MainActivity)
            
            if (profile?.setupCompleted == true) {
                withContext(Dispatchers.Main) {
                    // Skip setup and go directly to HomeActivity
                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    finish()
                    return@withContext
                }
            }

            // If we get here, show the setup page
            withContext(Dispatchers.Main) {
                val serviceIntent = Intent(this@MainActivity, StepCounterService::class.java)
                startService(serviceIntent)
                binding = SetupPageBinding.inflate(layoutInflater)
                setContentView(binding.root)

                // Set up preset step target buttons
                binding.button.setOnClickListener {
                    setStepTarget(5000)
                    binding.editTextNumber.setText("5000")
                    hasUserSetTarget = true
                }
                binding.button3.setOnClickListener {
                    setStepTarget(6500)
                    binding.editTextNumber.setText("6500")
                    hasUserSetTarget = true
                }
                binding.button4.setOnClickListener {
                    setStepTarget(8000)
                    binding.editTextNumber.setText("8000")
                    hasUserSetTarget = true
                }

                // Set up continue button
                binding.button10.setOnClickListener {
                    try {
                        // Validate that user has entered a name
                        val userName = binding.editTextText2.text.toString().trim()
                        if (userName.isEmpty()) {
                            Toast.makeText(this@MainActivity, getString(R.string.error_name_required), Toast.LENGTH_SHORT).show()
                            binding.editTextText2.requestFocus()
                            return@setOnClickListener
                        }
                        
                        // Additional validation: ensure name has at least 2 characters
                        if (userName.length < 2) {
                            Toast.makeText(this@MainActivity, getString(R.string.error_name_too_short), Toast.LENGTH_SHORT).show()
                            binding.editTextText2.requestFocus()
                            return@setOnClickListener
                        }

                        // Save user's name
                        UserPreferences.saveUserName(this@MainActivity, userName)

                        // Check if custom step target is entered
                        val customSteps = binding.editTextNumber.text.toString()
                        if (customSteps != getString(R.string.custom_steps)) {
                            val target = customSteps.toInt()
                            if (target > 0) {
                                setStepTarget(target)
                                hasUserSetTarget = true
                            } else {
                                Toast.makeText(this@MainActivity, "Please enter a valid step target", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                        }

                        // If user hasn't set a target (either through buttons or custom input), use default
                        if (!hasUserSetTarget) {
                            setStepTarget(6000) // Default target
                        }

                        // Save user interests
                        saveUserInterests()

                        // Mark setup as completed in Supabase
                        CoroutineScope(Dispatchers.IO).launch {
                            val currentProfile = ProfileService.getCurrentProfile(this@MainActivity)
                            if (currentProfile != null) {
                                val updatedProfile = currentProfile.copy(setupCompleted = true)
                                ProfileService.updateServerProfile(updatedProfile)
                            }
                            
                            withContext(Dispatchers.Main) {
                                // Proceed to home activity
                                val intent = Intent(this@MainActivity, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this@MainActivity, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setStepTarget(target: Int) {
        UserPreferences.setStepTarget(this, target)
        Toast.makeText(this, "Step target set to $target", Toast.LENGTH_SHORT).show()
    }

    private fun saveUserInterests() {
        val selectedInterests = mutableSetOf<String>()
        
        if (binding.checkAmusements.isChecked) selectedInterests.add("Amusements")
        if (binding.checkArchitecture.isChecked) selectedInterests.add("Architecture")
        if (binding.checkCultural.isChecked) selectedInterests.add("Cultural")
        if (binding.checkShops.isChecked) selectedInterests.add("Shops")
        if (binding.checkFoods.isChecked) selectedInterests.add("Foods")
        if (binding.checkSport.isChecked) selectedInterests.add("Sport")
        if (binding.checkHistorical.isChecked) selectedInterests.add("Historical")
        if (binding.checkNatural.isChecked) selectedInterests.add("Natural")
        if (binding.checkOther.isChecked) selectedInterests.add("Other")
        
        // If no interests selected, default to showing all
        if (selectedInterests.isEmpty()) {
            selectedInterests.add("All")
        }
        
        UserPreferences.saveUserInterests(this, selectedInterests)
        Toast.makeText(this, "Interests saved: ${selectedInterests.size} selected", Toast.LENGTH_SHORT).show()
    }
}
