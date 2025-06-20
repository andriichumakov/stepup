package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.databinding.SettingsPageBinding
import com.example.stepupapp.storage.LocalProfileStore
import com.example.stepupapp.managers.settings.*
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity(), 
    SettingsDataCallback, 
    SettingsImageCallback, 
    SettingsAccountCallback {
    
    private lateinit var binding: SettingsPageBinding
    private lateinit var actionBarLocationManager: ActionBarLocationManager
    private lateinit var actionBarGreetingManager: ActionBarGreetingManager
    private lateinit var actionBarProfileManager: ActionBarProfileManager
    
    // Specialized managers following SOLID principles
    private lateinit var dataManager: SettingsDataManager
    private lateinit var uiManager: SettingsUIManager
    private lateinit var imageManager: SettingsImageManager
    private lateinit var accountManager: SettingsAccountManager
    private lateinit var serviceManager: SettingsServiceManager
    
    private var currentStepTarget: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeManagers()
        setupActionBar()
        setupUI()
        loadInitialData()
        setupEventListeners()
    }
    
    private fun initializeManagers() {
        // Initialize managers with dependency injection
        dataManager = SettingsDataManager(this, this)
        uiManager = SettingsUIManager(this, binding)
        imageManager = SettingsImageManager(this, binding, this)
        accountManager = SettingsAccountManager(this, this)
        serviceManager = SettingsServiceManager(this)
    }
    
    private fun setupActionBar() {
        actionBarLocationManager = ActionBarLocationManager(this)
        actionBarLocationManager.setupActionBarLocation()

        actionBarGreetingManager = ActionBarGreetingManager(this)
        actionBarGreetingManager.updateGreeting()
        
        actionBarProfileManager = ActionBarProfileManager(this)
        actionBarProfileManager.updateProfilePicture()
    }
    
    private fun setupUI() {
        imageManager.initializeImageLaunchers()
        setupDebugListener()
    }
    
    private fun setupDebugListener() {
        binding.newNameEditText.setOnClickListener {
            android.util.Log.d("SettingsActivity", "Name field clicked - reloading profile data")
            loadInitialData()
        }
    }
    
    private fun loadInitialData() {
        lifecycleScope.launch {
            try {
                // Load all data in parallel for better performance
                val profileData = dataManager.loadProfileData()
                val stepTarget = dataManager.loadStepTarget()
                val notificationPref = dataManager.loadNotificationPreference()
                val interests = dataManager.loadInterests()
                
                // Update UI on main thread
                runOnUiThread {
                    uiManager.loadProfileImage()
                    uiManager.setNickname(profileData.nickname)
                    uiManager.setStepTarget(stepTarget)
                    uiManager.setNotificationPreference(notificationPref)
                    uiManager.setInterests(interests)
                    
                    currentStepTarget = stepTarget
                }
                
                onDataLoaded()
                
            } catch (e: Exception) {
                onDataError("Failed to load settings: ${e.message}")
            }
        }
    }
    
    private fun setupEventListeners() {
        setupSaveSettingsListener()
        setupAccountUpdateListener()
        setupLogoutListener()
        setupBackButtonListener()
        setupNicknameChangeListener()
    }
    
    private fun setupSaveSettingsListener() {
        binding.button2.setOnClickListener {
            val errors = uiManager.getValidationErrors()
            if (errors.isNotEmpty()) {
                showValidationErrors(errors)
                return@setOnClickListener
            }
            
            val newTarget = uiManager.getEnteredStepTarget() ?: 0
            val newNickname = uiManager.getEnteredNickname()
            val notificationPref = uiManager.getNotificationPreference()
            val interests = uiManager.getSelectedInterests()
            
            saveSettings(newTarget, newNickname, notificationPref, interests)
        }
    }
    
    private fun setupAccountUpdateListener() {
        binding.saveAccountButton.setOnClickListener {
            val newUsername = binding.editUsername.text.toString().trim()
            val newPassword = binding.editPassword.text.toString().trim()

            if (newUsername.isEmpty() && newPassword.isEmpty()) {
                Toast.makeText(this, "Please enter a username or password to update", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = accountManager.updateUserAccount(newUsername, newPassword)
                runOnUiThread {
                    onAccountUpdated(result)
                }
            }
        }
    }
    
    private fun setupLogoutListener() {
        binding.logoutButton.setOnClickListener {
            lifecycleScope.launch {
                val success = accountManager.signOut()
                
                if (success) {
                    // Clear local data
                    LocalProfileStore.clearProfiles(applicationContext)
                    UserPreferences.clear(this@SettingsActivity)
                    
                    // Navigate to auth screen
                    val intent = Intent(this@SettingsActivity, AuthOptionsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                
                runOnUiThread {
                    onSignOutCompleted(success)
                }
            }
        }
    }
    
    private fun setupBackButtonListener() {
        binding.backButton.setOnClickListener {
            navigateToHome()
        }
    }
    
    private fun setupNicknameChangeListener() {
        binding.newNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newName = uiManager.getEnteredNickname()
                if (newName.isNotEmpty() && uiManager.isValidNickname(newName)) {
                    saveNicknameChange(newName)
                }
            }
        }
    }
    
    private fun saveSettings(newTarget: Int, newNickname: String, notificationPref: Boolean, interests: Set<String>) {
        lifecycleScope.launch {
            var allSuccess = true
            val messages = mutableListOf<String>()
            
            // Save nickname if provided and valid
            if (newNickname.isNotEmpty() && uiManager.isValidNickname(newNickname)) {
                val nicknameSuccess = dataManager.saveNickname(newNickname)
                if (!nicknameSuccess) {
                    allSuccess = false
                    messages.add("Failed to save nickname")
                }
            }
            
            // Handle step target changes
            if (newTarget != currentStepTarget && uiManager.isValidStepTarget(newTarget)) {
                val stepTargetSuccess = dataManager.saveStepTarget(newTarget)
                if (stepTargetSuccess) {
                    // Reset notifications and streaks for new target
                    serviceManager.resetNotificationStates()
                    serviceManager.resetStreakNotificationTracking()
                } else {
                    allSuccess = false
                    messages.add("Failed to update step goal remotely")
                }
            } else if (uiManager.isValidStepTarget(newTarget)) {
                // Save target even if unchanged for consistency
                dataManager.saveStepTarget(newTarget)
            }
            
            // Save notification preference
            dataManager.saveNotificationPreference(notificationPref)
            
            // Save interests
            val interestsSuccess = dataManager.saveInterests(interests)
            if (!interestsSuccess) {
                // Note: Local save always succeeds, this is just for sync status
                messages.add("Interests saved locally, will sync when online")
            }
            
            runOnUiThread {
                if (allSuccess) {
                    Toast.makeText(this@SettingsActivity, "Settings updated successfully", Toast.LENGTH_SHORT).show()
                    
                    // Restart service and navigate home
                    serviceManager.restartStepCounterService()
                    navigateToHome()
                } else {
                    val errorMessage = if (messages.isNotEmpty()) {
                        messages.joinToString("; ")
                    } else {
                        "Some settings failed to update"
                    }
                    Toast.makeText(this@SettingsActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun saveNicknameChange(newName: String) {
        lifecycleScope.launch {
            val success = dataManager.saveNickname(newName)
            
            runOnUiThread {
                if (success) {
                    // Update ActionBar greeting with new nickname
                    actionBarGreetingManager.updateGreeting()
                    Toast.makeText(this@SettingsActivity, "Nickname updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Failed to save nickname", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showValidationErrors(errors: List<String>) {
        val errorMessage = errors.joinToString("\n")
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }
    
    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
    
    // SettingsDataCallback implementation
    override fun onDataLoaded() {
        // Data loading completed successfully
    }
    
    override fun onDataSaved(success: Boolean, message: String) {
        if (!success) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDataError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }
    
    // SettingsImageCallback implementation
    override fun onImageSelected(success: Boolean, message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        if (success) {
            // Update the header profile picture
            actionBarProfileManager.updateProfilePicture()
        }
    }
    
    override fun onImageProcessing(isProcessing: Boolean) {
        // Could show/hide loading indicator here if needed
        uiManager.enableUI(!isProcessing)
    }
    
    // SettingsAccountCallback implementation
    override fun onAccountUpdated(result: AccountUpdateResult) {
        val message = if (result.success) {
            result.message
        } else {
            if (result.errors.isNotEmpty()) {
                result.errors.joinToString("\n")
            } else {
                result.message
            }
        }
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onSignOutCompleted(success: Boolean) {
        if (!success) {
            Toast.makeText(this, "Failed to sign out", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        actionBarLocationManager.onDestroy()
    }
}