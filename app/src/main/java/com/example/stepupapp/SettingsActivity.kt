package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.stepupapp.databinding.SettingsPageBinding
import com.example.stepupapp.storage.LocalProfileStore
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {
    private lateinit var binding: SettingsPageBinding
    private lateinit var actionBarLocationManager: ActionBarLocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ActionBar location
        actionBarLocationManager = ActionBarLocationManager(this)
        actionBarLocationManager.setupActionBarLocation()

        // Load current step target
        val currentTarget = UserPreferences.getStepTarget(this)
        binding.editTextNumber2.setText(currentTarget.toString())

        // Load current interests
        loadCurrentInterests()

        // SAVE STEP GOAL & INTERESTS
        binding.button2.setOnClickListener {
            try {
                val newTarget = binding.editTextNumber2.text.toString().toInt()
                if (newTarget > 0) {
                    UserPreferences.setStepTarget(this, newTarget)
                    saveUserInterests()
                    Toast.makeText(this, "Settings updated successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Please enter a valid step target", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }

        // UPDATE USERNAME & PASSWORD
        binding.saveAccountButton.setOnClickListener {
            val newUsername = binding.editUsername.text.toString().trim()
            val newPassword = binding.editPassword.text.toString().trim()

            if (newUsername.isEmpty() && newPassword.isEmpty()) {
                Toast.makeText(this, "Please enter a username or password to update", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val session = ProfileService.auth.currentSessionOrNull()
                val userId = session?.user?.id

                if (userId == null) {
                    Toast.makeText(this@SettingsActivity, "No user session", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                var success = true

                if (newUsername.isNotEmpty()) {
                    val profile = ProfileService.getCurrentProfile()
                    if (profile != null) {
                        val updatedProfile = profile.copy(name = newUsername)
                        success = ProfileService.updateProfile(updatedProfile)
                    }
                }

                if (newPassword.isNotEmpty()) {
                    try {
                        ProfileService.auth.updateUser {
                            this.password = newPassword
                        }
                    } catch (e: Exception) {
                        success = false
                        Toast.makeText(this@SettingsActivity, "Password update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                if (success) {
                    Toast.makeText(this@SettingsActivity, "Account updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Failed to update account", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // LOGOUT
        binding.logoutButton.setOnClickListener {
            lifecycleScope.launch {
                LocalProfileStore.clearProfiles(applicationContext)
                UserPreferences.clear(this@SettingsActivity)
                val intent = Intent(this@SettingsActivity, AuthOptionsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        // Go back
        binding.backButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
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
        if (selectedInterests.isEmpty()) selectedInterests.add("All")
        UserPreferences.saveUserInterests(this, selectedInterests)
    }

    override fun onDestroy() {
        super.onDestroy()
        actionBarLocationManager.onDestroy()
    }
}