package com.example.stepupapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.example.stepupapp.databinding.SettingsPageBinding
import com.example.stepupapp.storage.LocalProfileStore
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.launch
import java.io.File

class SettingsActivity : BaseActivity() {
    private lateinit var binding: SettingsPageBinding
    private lateinit var actionBarLocationManager: ActionBarLocationManager
    private lateinit var actionBarGreetingManager: ActionBarGreetingManager
    private lateinit var actionBarProfileManager: ActionBarProfileManager
    
    // Profile picture management
    private var currentImageFile: File? = null
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize activity result launchers
        initializeActivityResultLaunchers()

        // Initialize and setup ActionBar location
        actionBarLocationManager = ActionBarLocationManager(this)
        actionBarLocationManager.setupActionBarLocation()

        // Initialize and setup ActionBar managers
        actionBarGreetingManager = ActionBarGreetingManager(this)
        actionBarGreetingManager.updateGreeting()
        
        actionBarProfileManager = ActionBarProfileManager(this)
        actionBarProfileManager.updateProfilePicture()

        // Load current profile data
        loadCurrentProfileData()

        // Load current step target
        val currentTarget = UserPreferences.getStepTarget(this)
        binding.editTextNumber2.setText(currentTarget.toString())

        // Load current notification preference
        binding.stepCounterNotificationSwitch.isChecked = UserPreferences.shouldShowStepCounterNotification(this)

        // Load current interests from database
        loadCurrentInterestsFromDatabase()

        // Setup profile change listeners
        setupProfileChangeListeners()

        // SAVE STEP GOAL & INTERESTS
        binding.button2.setOnClickListener {
            try {
                val newTarget = binding.editTextNumber2.text.toString().toInt()
                if (newTarget > 0) {
                    // Only reset notifications if the target is actually changing
                    if (newTarget != currentTarget) {
                        UserPreferences.setStepTarget(this, newTarget)
                        lifecycleScope.launch {
                            val success = ProfileService.updateStepGoal(newTarget)
                            if (!success) {
                                Toast.makeText(this@SettingsActivity, "Failed to update step goal remotely", Toast.LENGTH_SHORT).show()
                            }
                        }
                        // Reset notification states in the service
                        StepCounterService.getInstance()?.resetNotificationStates()
                        // Reset streak notification tracking so new streaks with new target are notified
                        UserPreferences.resetStreakNotificationTracking(this)
                        android.util.Log.d("SettingsActivity", "Step target changed from $currentTarget to $newTarget, notification states and streak tracking reset")
                    } else {
                        // If target didn't change, still save it to ensure consistency
                        UserPreferences.setStepTarget(this, newTarget)
                    }
                    
                    // Save notification preference
                    UserPreferences.setStepCounterNotificationVisibility(this, binding.stepCounterNotificationSwitch.isChecked)
                    
                    // Save interests to database
                    saveUserInterestsToDatabase()
                    
                    Toast.makeText(this, "Settings updated successfully", Toast.LENGTH_SHORT).show()

                    // Get current step count from the service before restarting
                    val currentService = StepCounterService.getInstance()
                    val stepData = currentService?.getCurrentStepCountData() ?: StepCounterService.StepCountData(0, 0)

                    // Restart the step counter service with preserved steps
                    val serviceIntent = StepCounterService.createStartIntent(
                        this,
                        stepData.totalSteps,
                        stepData.initialSteps
                    )
                    stopService(Intent(this, StepCounterService::class.java))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }

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
                // Use ProfileService signOut which clears user-specific data including profile images
                ProfileService.signOut(this@SettingsActivity)
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

    private fun loadCurrentInterestsFromDatabase() {
        lifecycleScope.launch {
            try {
                // Try to load from database first
                val serverInterestsCode = ProfileService.getUserInterestsCode()
                
                if (serverInterestsCode != null) {
                    // Server data available - use it and update local cache
                    val currentInterests = InterestCodeManager.codeToInterests(serverInterestsCode)
                    
                    runOnUiThread {
                        setInterestCheckboxes(currentInterests)
                    }
                    
                    // Update local preferences to match server
                    UserPreferences.saveUserInterests(this@SettingsActivity, currentInterests)
                    UserPreferences.saveInterestsCodeLocally(this@SettingsActivity, serverInterestsCode)
                    UserPreferences.markInterestsNeedingSync(this@SettingsActivity, false) // Clear sync flag
                    
                } else {
                    // No server data - use local fallback
                    throw Exception("No server data available")
                }
                
            } catch (e: Exception) {
                // Fallback to local preferences if database fails
                android.util.Log.w("SettingsActivity", "Loading from server failed: ${e.message}, using local data")
                
                runOnUiThread {
                    // Try local interests code first, then fall back to interest names
                    val localCode = UserPreferences.getInterestsCodeLocally(this@SettingsActivity)
                    val localInterests = if (localCode != null) {
                        InterestCodeManager.codeToInterests(localCode)
                    } else {
                        UserPreferences.getUserInterests(this@SettingsActivity)
                    }
                    
                    setInterestCheckboxes(localInterests)
                    
                    // Show user that they're working offline
                    Toast.makeText(this@SettingsActivity, "Working offline - changes will sync when online", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun setInterestCheckboxes(interests: Set<String>) {
        binding.checkBox10.isChecked = interests.contains("Amusements")
        binding.checkBox11.isChecked = interests.contains("Architecture")
        binding.checkBox12.isChecked = interests.contains("Cultural")
        binding.checkBox14.isChecked = interests.contains("Shops")
        binding.checkBox16.isChecked = interests.contains("Foods")
        binding.checkBox18.isChecked = interests.contains("Sport")
        binding.checkBox21.isChecked = interests.contains("Historical")
        binding.checkBox20.isChecked = interests.contains("Natural")
        binding.checkBox22.isChecked = interests.contains("Other")
    }
    
    private fun getUserSelectedInterests(): Set<String> {
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
        return selectedInterests
    }

    private fun saveUserInterestsToDatabase() {
        val selectedInterests = getUserSelectedInterests()
        val interestsCode = InterestCodeManager.interestsToCode(selectedInterests)
        
        // ALWAYS save locally first (guaranteed to succeed)
        UserPreferences.saveUserInterests(this, selectedInterests)
        UserPreferences.saveInterestsCodeLocally(this, interestsCode)
        
        // Then try to sync to server in background
        lifecycleScope.launch {
            try {
                val success = ProfileService.updateInterestsCode(interestsCode)
                
                runOnUiThread {
                    if (success) {
                        // Clear the sync flag since we successfully synced
                        UserPreferences.markInterestsNeedingSync(this@SettingsActivity, false)
                        // Don't show success message - it's expected behavior
                    } else {
                        // Mark for later sync
                        UserPreferences.markInterestsNeedingSync(this@SettingsActivity, true)
                        Toast.makeText(this@SettingsActivity, "Interests saved locally. Will sync when online.", Toast.LENGTH_LONG).show()
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "Failed to sync interests to server: ${e.message}", e)
                
                runOnUiThread {
                    // Mark for later sync
                    UserPreferences.markInterestsNeedingSync(this@SettingsActivity, true)
                    Toast.makeText(this@SettingsActivity, "Interests saved locally. Will sync when online.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initializeActivityResultLaunchers() {
        // Gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleImageSelected(uri)
                }
            }
        }

        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentImageFile?.let { file ->
                    handleImageSelected(Uri.fromFile(file))
                }
            } else {
                // Clean up the file if camera operation failed
                currentImageFile?.delete()
                currentImageFile = null
            }
        }

        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied. You can still choose from gallery.", Toast.LENGTH_LONG).show()
                // Fallback to gallery if camera permission denied
                openGallery()
            }
        }
    }

    private fun loadCurrentProfileData() {
        lifecycleScope.launch {
            try {
                // Load profile from database first
                val profile = ProfileService.getCurrentProfile()
                
                runOnUiThread {
                    // Load profile picture using the ProfilePictureLoader
                    ProfilePictureLoader.loadProfilePicture(
                        context = this@SettingsActivity,
                        imageView = binding.currentProfileImageView,
                        showDefault = true
                    )
                    
                    // Load nickname from database or local storage
                    val currentNickname = profile?.nickname ?: UserPreferences.getUserNickname(this@SettingsActivity)
                    if (currentNickname.isNotEmpty()) {
                        binding.newNameEditText.setText(currentNickname)
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "Failed to load profile data: ${e.message}")
                
                runOnUiThread {
                    // Load local profile picture
                    ProfilePictureLoader.loadProfilePicture(
                        context = this@SettingsActivity,
                        imageView = binding.currentProfileImageView,
                        showDefault = true
                    )
                    
                    // Load nickname from local storage as fallback
                    val currentNickname = UserPreferences.getUserNickname(this@SettingsActivity)
                    if (currentNickname.isNotEmpty()) {
                        binding.newNameEditText.setText(currentNickname)
                    }
                }
            }
        }
    }

    private fun setupProfileChangeListeners() {
        // Profile picture change button
        binding.changeProfilePictureButton.setOnClickListener {
            showImageSelectionDialog()
        }

        // Name field with live updates (no separate save button needed)
        binding.newNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newName = binding.newNameEditText.text.toString().trim()
                if (newName.isNotEmpty() && newName.length >= 2) {
                    saveNameChange(newName)
                }
            }
        }
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        
        AlertDialog.Builder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestCameraPermissionAndOpen()
                    1 -> openGallery()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestCameraPermissionAndOpen() {
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        try {
            currentImageFile = File(filesDir, "profile_camera_${System.currentTimeMillis()}.jpg")
            val imageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                currentImageFile!!
            )
            cameraLauncher.launch(imageUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        galleryLauncher.launch(intent)
    }

    private fun handleImageSelected(uri: Uri) {
        lifecycleScope.launch {
            try {
                // Convert to base64 using ImageBase64Utils
                val bitmap = ImageBase64Utils.uriToBitmap(this@SettingsActivity, uri)
                if (bitmap != null) {
                    val base64String = ImageBase64Utils.bitmapToBase64(bitmap)
                    
                    if (base64String != null) {
                        // Save locally first (guaranteed to succeed)
                        UserPreferences.saveProfileImageBase64(this@SettingsActivity, base64String)
                    
                    runOnUiThread {
                        // Update the UI immediately
                        binding.currentProfileImageView.setImageBitmap(bitmap)
                        // Also update the header profile picture
                        actionBarProfileManager.updateProfilePicture()
                        Toast.makeText(this@SettingsActivity, "Profile picture updated", Toast.LENGTH_SHORT).show()
                    }
                    
                    // Try to sync to database in background
                    try {
                        val success = ProfileService.updateProfilePictureBase64(base64String)
                        
                        runOnUiThread {
                                                         if (!success) {
                                 // Mark for later sync but don't show error since local save succeeded
                                 UserPreferences.markProfileImageNeedingSync(this@SettingsActivity, true)
                                 android.util.Log.w("SettingsActivity", "Profile picture saved locally, will sync when online")
                             } else {
                                 // Clear sync flag on successful upload
                                 UserPreferences.markProfileImageNeedingSync(this@SettingsActivity, false)
                             }
                        }
                        
                    } catch (e: Exception) {
                                                 android.util.Log.w("SettingsActivity", "Failed to sync profile picture: ${e.message}")
                         runOnUiThread {
                             UserPreferences.markProfileImageNeedingSync(this@SettingsActivity, true)
                         }
                    }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "Failed to convert image to base64", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Failed to process image", Toast.LENGTH_SHORT).show()
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Error handling image: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveNameChange(newName: String) {
        lifecycleScope.launch {
            try {
                // Save locally first (guaranteed to succeed)
                UserPreferences.saveUserNickname(this@SettingsActivity, newName)
                
                runOnUiThread {
                    // Update ActionBar greeting with new nickname
                    actionBarGreetingManager.updateGreeting()
                    Toast.makeText(this@SettingsActivity, "Nickname updated", Toast.LENGTH_SHORT).show()
                }
                
                // Try to sync to database in background
                try {
                    val success = ProfileService.updateNickname(newName)
                    
                    runOnUiThread {
                        if (!success) {
                            // Mark for later sync but don't show error since local save succeeded
                            UserPreferences.markNicknameNeedingSync(this@SettingsActivity, true)
                            android.util.Log.w("SettingsActivity", "Nickname saved locally, will sync when online")
                        } else {
                            // Clear sync flag on successful upload
                            UserPreferences.markNicknameNeedingSync(this@SettingsActivity, false)
                        }
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.w("SettingsActivity", "Failed to sync nickname: ${e.message}")
                    runOnUiThread {
                        UserPreferences.markNicknameNeedingSync(this@SettingsActivity, true)
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Error saving nickname: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Error saving nickname: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        actionBarLocationManager.onDestroy()
    }
}