package com.example.stepupapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream


class AddMemoryActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    private lateinit var imagePreview: ImageView
    private lateinit var chooseImageBtn: Button
    private lateinit var uploadBtn: Button
    private lateinit var dateInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var stepsInput: EditText  // Added steps input
    private lateinit var backBtn: Button  // Added back button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_memory)

        imagePreview = findViewById(R.id.imagePreview)
        chooseImageBtn = findViewById(R.id.btnChooseImage)
        uploadBtn = findViewById(R.id.btnSubmitMemory)
        dateInput = findViewById(R.id.editTextDate)
        locationInput = findViewById(R.id.editTextLocation)
        stepsInput = findViewById(R.id.editTextSteps)  // You must add this EditText in your layout!
        backBtn = findViewById(R.id.btnBack)  // Initialize back button

        chooseImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        uploadBtn.setOnClickListener {
            val date = dateInput.text.toString()
            val location = locationInput.text.toString()
            val steps = stepsInput.text.toString()

            if (selectedImageUri == null || date.isEmpty() || location.isEmpty() || steps.isEmpty()) {
                Toast.makeText(this, "Please fill all fields and choose an image.", Toast.LENGTH_SHORT).show()
            } else {
                val imageFile = uriToFile(selectedImageUri!!)
                uploadPlace(imageFile, location, date, steps)
            }
        }

        // Set up the back button click listener
        backBtn.setOnClickListener {
            val intent = Intent(this, MemoryActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            imagePreview.setImageURI(selectedImageUri)
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream from URI")
        val fileName = getFileName(uri)
        val file = File(cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return file
    }

    private fun getFileName(uri: Uri): String {
        var name = "image.jpg"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }

    private fun uploadPlace(imageFile: File, name: String, date: String, steps: String) {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

        val namePart = RequestBody.create("text/plain".toMediaTypeOrNull(), name)
        val datePart = RequestBody.create("text/plain".toMediaTypeOrNull(), date)
        val stepsPart = RequestBody.create("text/plain".toMediaTypeOrNull(), steps)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://172.20.10.2:5000/")  // Use localhost IP for Android emulator; or your server URL
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }).build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        api.uploadPlace(namePart, datePart, stepsPart, imagePart).enqueue(object : Callback<PlaceResponse> {
            override fun onResponse(call: Call<PlaceResponse>, response: Response<PlaceResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AddMemoryActivity, "Place uploaded successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddMemoryActivity, "Upload failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PlaceResponse>, t: Throwable) {
                Toast.makeText(this@AddMemoryActivity, "Upload error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    interface ApiService {
        @Multipart
        @POST("places")
        fun uploadPlace(
            @Part("name") name: RequestBody,
            @Part("date_saved") date: RequestBody,
            @Part("steps_taken") steps: RequestBody,
            @Part image: MultipartBody.Part
        ): Call<PlaceResponse>
    }

    data class PlaceResponse(
        val id: Int,
        val name: String,
        val date_saved: String,
        val steps_taken: String,
        val image_url: String?
    )
}
