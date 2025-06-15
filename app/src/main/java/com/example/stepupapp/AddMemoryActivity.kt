package com.example.stepupapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.stepupapp.databinding.ActivityAddMemoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AddMemoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMemoryBinding
    private var selectedImageUri: Uri? = null
    private var imageUriFromCamera: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val CAMERA_REQUEST_CODE = 1002
        private const val CAMERA_PERMISSION_CODE = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.btnTakePicture.setOnClickListener {
            checkCameraPermissionAndOpenCamera()
        }

        binding.btnSubmitMemory.setOnClickListener {
            val date = binding.editTextDate.text.toString()
            val location = binding.editTextLocation.text.toString()
            val steps = binding.editTextSteps.text.toString()

            if (selectedImageUri == null || date.isBlank() || location.isBlank() || steps.isBlank()) {
                Toast.makeText(this, "Please fill all fields and choose an image.", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val place = Place(
                            name = location,
                            date_saved = date,
                            steps_taken = steps,
                            imageUri = selectedImageUri.toString()
                        )

                        val db = PlaceDatabase.getDatabase(applicationContext)
                        db.placeDao().insert(place)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddMemoryActivity, "Memory saved!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddMemoryActivity, "Error saving memory.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val imageFile = File.createTempFile("memory_", ".jpg", cacheDir)
        imageUriFromCamera = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            imageFile
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUriFromCamera)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    binding.imagePreview.setImageURI(uri)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to access image", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = imageUriFromCamera
            binding.imagePreview.setImageURI(selectedImageUri)
        }
    }
}
