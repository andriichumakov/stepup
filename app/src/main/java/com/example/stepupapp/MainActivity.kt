package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.stepupapp.databinding.SetupPageBinding
import com.example.stepupapp.managers.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {
    private lateinit var binding: SetupPageBinding
    private var hasUserSetTarget = false
    
    // Manager instances following dependency injection pattern
    private lateinit var profileSetupManager: ProfileSetupManager
    private lateinit var imagePickerManager: ImagePickerManager
    private lateinit var validationManager: ValidationManager
    private lateinit var interestSelectionManager: InterestSelectionManager
    private lateinit var stepGoalManager: StepGoalManager
    private lateinit var setupNavigationManager: SetupNavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize managers and register launchers FIRST (before any coroutines)
        initializeManagers()
        registerActivityLaunchers()
        
        // Then handle navigation and setup
        CoroutineScope(Dispatchers.IO).launch {
            val navigationHandled = setupNavigationManager.checkAuthenticationAndNavigate()
            
            if (!navigationHandled) {
                withContext(Dispatchers.Main) {
                    initializeSetupScreen()
                }
            }
        }
    }
    
    private fun initializeManagers() {
        profileSetupManager = ProfileSetupManager(this)
        imagePickerManager = ImagePickerManager(this)
        validationManager = ValidationManager(this)
        interestSelectionManager = InterestSelectionManager()
        stepGoalManager = StepGoalManager(this)
        setupNavigationManager = SetupNavigationManager(this)
    }
    
    private fun registerActivityLaunchers() {
        // Register activity result launchers early, before activity starts
        imagePickerManager.registerActivityLaunchers()
    }
    
    private fun initializeSetupScreen() {
        startStepCounterService()
        setupBinding()
        setupImagePicker()
        setupStepTargetButtons()
        setupContinueButton()
        loadExistingProfileImage()
    }
    
    private fun startStepCounterService() {
        val serviceIntent = Intent(this, StepCounterService::class.java)
        startService(serviceIntent)
    }
    
    private fun setupBinding() {
        binding = SetupPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
    
    private fun setupImagePicker() {
        // Now set up the UI interactions with the ImageView
        imagePickerManager.setupImageView(binding.profileImageView)
        
        binding.buttonSelectImage.setOnClickListener {
            imagePickerManager.showImagePickerDialog()
        }
    }
    
    private fun setupStepTargetButtons() {
        StepGoalManager.PRESET_GOALS.forEachIndexed { index, goal ->
            val button = when (index) {
                0 -> binding.button
                1 -> binding.button3
                2 -> binding.button4
                else -> return@forEachIndexed
            }
            
            button.setOnClickListener {
                if (stepGoalManager.setStepTarget(goal, binding.editTextNumber)) {
                    hasUserSetTarget = true
                }
            }
        }
    }
    
    private fun setupContinueButton() {
        binding.button10.setOnClickListener {
            handleContinueButtonClick()
        }
    }
    
    private fun handleContinueButtonClick() {
        val userName = binding.editTextText2.text.toString().trim()
        val customSteps = binding.editTextNumber.text.toString()
        
        // Validate user input
        val nameValidation = validationManager.validateUserName(userName)
        if (!nameValidation.isValid) {
            showValidationError(nameValidation.errorMessage!!, binding.editTextText2)
            return
        }
        
        // Handle step target
        val finalStepGoal = determineStepGoal(customSteps)
        if (finalStepGoal == null) {
            Toast.makeText(this, "Please enter a valid step target", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Process setup completion
        processSetupCompletion(userName, finalStepGoal)
    }
    
    private fun determineStepGoal(customSteps: String): Int? {
        val customStepsPlaceholder = getString(R.string.custom_steps)
        
        return if (customSteps != customStepsPlaceholder) {
            val validation = validationManager.validateStepTarget(customSteps, customStepsPlaceholder)
            if (validation.isValid) {
                customSteps.toInt().also { hasUserSetTarget = true }
            } else {
                Toast.makeText(this, validation.errorMessage, Toast.LENGTH_SHORT).show()
                null
            }
        } else if (hasUserSetTarget) {
            UserPreferences.getStepTarget(this)
        } else {
            stepGoalManager.getDefaultStepGoal()
        }
    }
    
    private fun processSetupCompletion(userName: String, stepGoal: Int) {
        val selectedInterests = interestSelectionManager.getSelectedInterests(binding)
        
        CoroutineScope(Dispatchers.IO).launch {
            profileSetupManager.saveProfileSetup(
                nickname = userName,
                stepGoal = stepGoal,
                selectedInterests = selectedInterests,
                callback = object : ProfileSetupManager.SetupCallback {
                    override suspend fun onSetupComplete() {
                        withContext(Dispatchers.Main) {
                            setupNavigationManager.navigateToHome()
                        }
                    }
                    
                    override suspend fun onSetupError(message: String) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            setupNavigationManager.navigateToHome()
                        }
                    }
                }
            )
        }
    }
    
    private fun showValidationError(message: String, focusView: android.view.View) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        focusView.requestFocus()
    }
    
    private fun loadExistingProfileImage() {
        ProfilePictureLoader.loadProfilePicture(this, binding.profileImageView, true)
    }
}
