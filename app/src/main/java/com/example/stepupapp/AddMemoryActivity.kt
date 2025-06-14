package com.example.stepupapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.stepupapp.databinding.ActivityAddMemoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddMemoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMemoryBinding
    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
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
        }
    }
}
