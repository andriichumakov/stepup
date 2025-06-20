package com.example.stepupapp.managers.settings

import android.content.Context
import com.example.stepupapp.databinding.SettingsPageBinding
import com.example.stepupapp.ProfilePictureLoader

interface SettingsUIProvider {
    fun loadProfileImage()
    fun setNickname(nickname: String)
    fun setStepTarget(target: Int)
    fun setNotificationPreference(enabled: Boolean)
    fun setInterests(interests: Set<String>)
    fun getEnteredNickname(): String
    fun getEnteredStepTarget(): Int?
    fun getNotificationPreference(): Boolean
    fun getSelectedInterests(): Set<String>
    fun isValidStepTarget(target: Int): Boolean
    fun isValidNickname(nickname: String): Boolean
}

class SettingsUIManager(
    private val context: Context,
    private val binding: SettingsPageBinding
) : SettingsUIProvider {
    
    companion object {
        private const val MIN_STEP_TARGET = 1
        private const val MAX_STEP_TARGET = 100000
        private const val MIN_NICKNAME_LENGTH = 2
        private const val MAX_NICKNAME_LENGTH = 50
    }
    
    override fun loadProfileImage() {
        ProfilePictureLoader.loadProfilePicture(
            context = context,
            imageView = binding.currentProfileImageView,
            showDefault = true
        )
    }
    
    override fun setNickname(nickname: String) {
        binding.newNameEditText.setText(nickname)
    }
    
    override fun setStepTarget(target: Int) {
        binding.editTextNumber2.setText(target.toString())
    }
    
    override fun setNotificationPreference(enabled: Boolean) {
        binding.stepCounterNotificationSwitch.isChecked = enabled
    }
    
    override fun setInterests(interests: Set<String>) {
        // Reset all checkboxes first
        resetInterestCheckboxes()
        
        // Set the appropriate checkboxes based on interests
        interests.forEach { interest ->
            when (interest) {
                "Amusements" -> binding.checkBox10.isChecked = true
                "Architecture" -> binding.checkBox11.isChecked = true
                "Cultural" -> binding.checkBox12.isChecked = true
                "Shops" -> binding.checkBox14.isChecked = true
                "Foods" -> binding.checkBox16.isChecked = true
                "Sport" -> binding.checkBox18.isChecked = true
                "Historical" -> binding.checkBox21.isChecked = true
                "Natural" -> binding.checkBox20.isChecked = true
                "Other" -> binding.checkBox22.isChecked = true
            }
        }
    }
    
    override fun getEnteredNickname(): String {
        return binding.newNameEditText.text.toString().trim()
    }
    
    override fun getEnteredStepTarget(): Int? {
        return try {
            binding.editTextNumber2.text.toString().toIntOrNull()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    override fun getNotificationPreference(): Boolean {
        return binding.stepCounterNotificationSwitch.isChecked
    }
    
    override fun getSelectedInterests(): Set<String> {
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
        
        // Default to "All" if no interests selected
        if (selectedInterests.isEmpty()) {
            selectedInterests.add("All")
        }
        
        return selectedInterests
    }
    
    override fun isValidStepTarget(target: Int): Boolean {
        return target in MIN_STEP_TARGET..MAX_STEP_TARGET
    }
    
    override fun isValidNickname(nickname: String): Boolean {
        val trimmed = nickname.trim()
        return trimmed.length in MIN_NICKNAME_LENGTH..MAX_NICKNAME_LENGTH
    }
    
    private fun resetInterestCheckboxes() {
        binding.checkBox10.isChecked = false
        binding.checkBox11.isChecked = false
        binding.checkBox12.isChecked = false
        binding.checkBox14.isChecked = false
        binding.checkBox16.isChecked = false
        binding.checkBox18.isChecked = false
        binding.checkBox21.isChecked = false
        binding.checkBox20.isChecked = false
        binding.checkBox22.isChecked = false
    }
    
    // Utility methods for managing UI state
    fun enableUI(enabled: Boolean) {
        binding.editTextNumber2.isEnabled = enabled
        binding.newNameEditText.isEnabled = enabled
        binding.stepCounterNotificationSwitch.isEnabled = enabled
        binding.button2.isEnabled = enabled
        binding.saveAccountButton.isEnabled = enabled
        
        // Enable/disable interest checkboxes
        binding.checkBox10.isEnabled = enabled
        binding.checkBox11.isEnabled = enabled
        binding.checkBox12.isEnabled = enabled
        binding.checkBox14.isEnabled = enabled
        binding.checkBox16.isEnabled = enabled
        binding.checkBox18.isEnabled = enabled
        binding.checkBox21.isEnabled = enabled
        binding.checkBox20.isEnabled = enabled
        binding.checkBox22.isEnabled = enabled
    }
    
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        val nickname = getEnteredNickname()
        if (nickname.isNotEmpty() && !isValidNickname(nickname)) {
            errors.add("Nickname must be between $MIN_NICKNAME_LENGTH and $MAX_NICKNAME_LENGTH characters")
        }
        
        val stepTarget = getEnteredStepTarget()
        if (stepTarget == null) {
            errors.add("Please enter a valid step target")
        } else if (!isValidStepTarget(stepTarget)) {
            errors.add("Step target must be between $MIN_STEP_TARGET and $MAX_STEP_TARGET")
        }
        
        return errors
    }
} 