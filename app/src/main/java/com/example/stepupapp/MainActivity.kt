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
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest

class MainActivity : BaseActivity() {
    private lateinit var binding: SetupPageBinding
    private var hasUserSetTarget = false
    
    // Profile picture handling
    private var currentPhotoPath: String? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize activity result launchers
        initializeActivityLaunchers()
        CoroutineScope(Dispatchers.IO).launch {
            if (!ProfileService.isSignedIn()) {
                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@MainActivity, AuthOptionsActivity::class.java))
                    finish()
                    return@withContext
                }
            }
            
            if (ProfileService.hasSetStepGoal()) {
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

                // Set up profile picture button
                binding.buttonSelectImage.setOnClickListener {
                    showImagePickerDialog()
                }
                
                // Load existing profile image if available
                loadExistingProfileImage()

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

                        // Save user's nickname (display name)
                        UserPreferences.saveUserNickname(this@MainActivity, userName)

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

                        // Save user interests locally FIRST (always succeeds)
                        val selectedInterests = getUserSelectedInterests()
                        saveUserInterestsLocally(selectedInterests)

                        // Then sync to server in background
                        CoroutineScope(Dispatchers.IO).launch {
                            val stepGoal = UserPreferences.getStepTarget(this@MainActivity)
                            val stepGoalSuccess = ProfileService.updateStepGoal(stepGoal)
                            
                            // Always try to save interests to server, independent of step goal
                            val interestsCode = InterestCodeManager.interestsToCode(selectedInterests)
                            val interestsSuccess = try {
                                ProfileService.updateInterestsCode(interestsCode)
                            } catch (e: Exception) {
                                android.util.Log.w("MainActivity", "Failed to save interests to server: ${e.message}")
                                // Mark interests as needing sync for later retry
                                UserPreferences.markInterestsNeedingSync(this@MainActivity, true)
                                false
                            }
                            
                            // Try to save nickname to server
                            val userNickname = UserPreferences.getUserNickname(this@MainActivity)
                            val nicknameSuccess = if (userNickname.isNotEmpty()) {
                                try {
                                    ProfileService.updateNickname(userNickname)
                                } catch (e: Exception) {
                                    android.util.Log.w("MainActivity", "Failed to save nickname to server: ${e.message}")
                                    // Mark nickname as needing sync for later retry
                                    UserPreferences.markNicknameNeedingSync(this@MainActivity, true)
                                    false
                                }
                            } else {
                                true // No nickname to sync
                            }

                            withContext(Dispatchers.Main) {
                                if (!stepGoalSuccess || !interestsSuccess || !nicknameSuccess) {
                                    Toast.makeText(this@MainActivity, "Setup saved locally. Will sync when online.", Toast.LENGTH_LONG).show()
                                }
                                
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

    private fun getUserSelectedInterests(): Set<String> {
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
        
        return selectedInterests
    }

    private fun saveUserInterestsLocally(selectedInterests: Set<String>) {
        // Save to local preferences (always succeeds)
        UserPreferences.saveUserInterests(this, selectedInterests)
        
        // Also save the interests code locally for faster access
        val interestsCode = InterestCodeManager.interestsToCode(selectedInterests)
        UserPreferences.saveInterestsCodeLocally(this, interestsCode)
        
        Toast.makeText(this, "Interests saved: ${selectedInterests.size} selected", Toast.LENGTH_SHORT).show()
    }
    
    private fun initializeActivityLaunchers() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoPath?.let { path ->
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(File(path)))
                        binding.profileImageView.setImageBitmap(bitmap)
                        saveProfileImageToPreferences(bitmap)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error processing camera image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Clean up the file if camera operation failed
                currentPhotoPath?.let { path ->
                    File(path).delete()
                }
                currentPhotoPath = null
            }
        }
        
        // Gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                try {
                    val bitmap = ImageBase64Utils.uriToBitmap(this, it)
                    if (bitmap != null) {
                        binding.profileImageView.setImageBitmap(bitmap)
                        saveProfileImageToPreferences(bitmap)
                    } else {
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraPermissionGranted = permissions[Manifest.permission.CAMERA] ?: false
            
            if (cameraPermissionGranted) {
                takePhoto()
            } else {
                Toast.makeText(this, "Camera permission denied. You can still choose from gallery.", Toast.LENGTH_LONG).show()
                openGallery()
            }
        }
    }
    
    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        
        AlertDialog.Builder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkPermissionsAndTakePhoto()
                    1 -> checkPermissionsAndChooseFromGallery()
                }
            }
            .show()
    }
    
    private fun checkPermissionsAndTakePhoto() {
        if (hasCameraPermission()) {
            takePhoto()
        } else {
            requestCameraPermission()
        }
    }
    
    private fun checkPermissionsAndChooseFromGallery() {
        // Gallery doesn't need special permissions on Android 13+
        openGallery()
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }
    
    private fun takePhoto() {
        val photoFile = createImageFile()
        photoFile?.let { file ->
            currentPhotoPath = file.absolutePath
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )
            cameraLauncher.launch(photoURI)
        }
    }
    
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
    
    private fun createImageFile(): File? {
        return try {
            // Use internal files directory for better reliability
            File(filesDir, "profile_camera_${System.currentTimeMillis()}.jpg")
        } catch (e: Exception) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    private fun saveProfileImageToPreferences(bitmap: Bitmap) {
        try {
            // Convert bitmap to base64 for database storage
            val base64Image = ImageBase64Utils.bitmapToBase64(bitmap)
            if (base64Image != null) {
                // Save base64 locally first (always succeeds)
                UserPreferences.saveProfileImageBase64(this, base64Image)
                
                // Also save as file for backward compatibility
                val imageFile = File(filesDir, "profile_image.jpg")
                val outputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                outputStream.close()
                UserPreferences.saveProfileImagePath(this, imageFile.absolutePath)
                
                // Try to sync to database in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = ProfileService.updateProfilePictureBase64(base64Image)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                UserPreferences.markProfileImageNeedingSync(this@MainActivity, false)
                                Toast.makeText(this@MainActivity, "Profile picture saved", Toast.LENGTH_SHORT).show()
                            } else {
                                UserPreferences.markProfileImageNeedingSync(this@MainActivity, true)
                                Toast.makeText(this@MainActivity, "Profile picture saved locally. Will sync when online.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("MainActivity", "Failed to sync profile picture: ${e.message}")
                        UserPreferences.markProfileImageNeedingSync(this@MainActivity, true)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Profile picture saved locally. Will sync when online.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Error converting image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving profile picture", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadExistingProfileImage() {
        // Use the centralized ProfilePictureLoader for consistency
        ProfilePictureLoader.loadProfilePicture(this, binding.profileImageView, true)
    }
}
