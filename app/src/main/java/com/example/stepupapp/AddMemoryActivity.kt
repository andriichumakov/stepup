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
import androidx.exifinterface.media.ExifInterface
import android.location.Geocoder
import android.util.Log
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class AddMemoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMemoryBinding
    private var selectedImageUri: Uri? = null
    private var imageUriFromCamera: Uri? = null
    private lateinit var locationManager: com.example.stepupapp.LocationManager

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val CAMERA_REQUEST_CODE = 1002
        private const val CAMERA_PERMISSION_CODE = 2001
        private const val LOCATION_PERMISSION_CODE = 2002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentSteps = intent.getIntExtra("currentSteps", 0)
        binding.textViewSteps.text = currentSteps.toString()

        locationManager = com.example.stepupapp.LocationManager(this) { location ->
            val locationName = locationManager.getLocationName(location.latitude, location.longitude)
            binding.editTextLocation.setText(locationName)
            Toast.makeText(this, "Current location detected: $locationName", Toast.LENGTH_SHORT).show()
            locationManager.stopLocationUpdates()
        }

        binding.btnChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.btnTakePicture.setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        binding.btnGetCurrentLocation.setOnClickListener {
            getCurrentLocation()
        }

        binding.btnSubmitMemory.setOnClickListener {
            Log.d("MemoryDebug", "selectedImageUri=$selectedImageUri, imageUriFromCamera=$imageUriFromCamera")

            val date = binding.textViewDate.text.toString()
            val location = binding.editTextLocation.text.toString()
            val steps = binding.textViewSteps.text.toString()
            val description = binding.editTextDescription.text.toString()
            val rating = binding.ratingBar.rating

            Log.d("MemoryDebug", "Submit pressed with: selectedImageUri=$selectedImageUri, imageUriFromCamera=$imageUriFromCamera")
            Log.d("MemoryDebug", "Fields: date='$date', location='$location', steps='$steps'")

            val imageUriToSave = selectedImageUri ?: imageUriFromCamera
            if (imageUriToSave == null || date.isBlank() || location.isBlank() || steps.isBlank()) {
                Toast.makeText(this, "Please fill all fields and choose or take an image.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val place = Place(
                            name = location,
                            date_saved = date,
                            steps_taken = steps,
                            imageUri = imageUriToSave.toString(),
                            description = description,
                            rating = rating
                        )

                        val db = PlaceDatabase.getDatabase(applicationContext)
                        db.placeDao().insert(place)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddMemoryActivity, "Memory saved!", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
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

    private fun checkPermissionsAndOpenCamera() {
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
            "$packageName.provider",
            imageFile
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUriFromCamera)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            imageUriFromCamera = null
            selectedImageUri?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    binding.imagePreview.setImageURI(uri)

                    val dateTaken = getDateFromImage(uri) ?: SimpleDateFormat("yy-MM-dd", Locale.getDefault()).format(Date())
                    Log.d("MemoryDebug", "Setting date to: $dateTaken")
                    binding.textViewDate.setText(dateTaken)

                    CoroutineScope(Dispatchers.IO).launch {
                        val address = getLocationFromImage(uri)
                        withContext(Dispatchers.Main) {
                            if (address != null) {
                                binding.editTextLocation.setText(address)
                                Toast.makeText(this@AddMemoryActivity, "Location detected: $address", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@AddMemoryActivity, "No GPS data in image. Use 📍 Current button or enter manually.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to access image", Toast.LENGTH_SHORT).show()
                }
            }

        }else if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = null

            imageUriFromCamera?.let { uri ->
                binding.imagePreview.setImageURI(uri)

                val dateTaken = getDateFromImage(uri)
                binding.textViewDate.text = dateTaken ?: ""

                CoroutineScope(Dispatchers.IO).launch {
                    val address = getLocationFromImage(uri)
                    withContext(Dispatchers.Main) {
                        if (address != null) {
                            binding.editTextLocation.setText(address)
                            Toast.makeText(this@AddMemoryActivity, "Location detected: $address", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AddMemoryActivity, "No GPS data in image. Use 📍 Current button or enter manually.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } ?: run {
                Toast.makeText(this, "Failed to capture image.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun getLocationFromImage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                val latLong = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLong)
                Log.d("MemoryDebug", "EXIF has GPS? $hasLatLong, lat=${latLong.getOrNull(0)}, lon=${latLong.getOrNull(1)}")
                if (hasLatLong) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latLong[0].toDouble(), latLong[1].toDouble(), 1)
                    Log.d("MemoryDebug", "Geocoder returned addresses: $addresses")
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        inputStream.close()
                        return "${address.locality ?: "Unknown"}, ${address.countryName ?: ""}"
                    } else {
                        Log.d("MemoryDebug", "No addresses found from geocoder")
                    }
                } else {
                    Log.d("MemoryDebug", "No GPS data in image - could enable camera location or use current location")
                }
                inputStream.close()
            }
            null
        } catch (e: Exception) {
            Log.e("MemoryDebug", "Error extracting location: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun getDateFromImage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.use {
                val exif = ExifInterface(it)
                val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                if (dateTimeStr != null) {
                    val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    val formatter = SimpleDateFormat("yy-MM-dd", Locale.getDefault())

                    val date = parser.parse(dateTimeStr)
                    val today = Date()
                    if (date != null && !date.after(today)) {
                        return formatter.format(date)
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getCurrentLocation() {
        if (locationManager.checkLocationPermission()) {
            binding.editTextLocation.hint = "Getting current location..."
            locationManager.startLocationUpdates()
        } else {
            locationManager.requestLocationPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show()
                }
            }
            com.example.stepupapp.LocationManager.REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    Toast.makeText(this, "Location permission denied. Please enter location manually.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
