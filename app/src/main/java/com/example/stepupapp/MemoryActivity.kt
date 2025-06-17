package com.example.stepupapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import com.example.stepupapp.databinding.ActivityMemoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryBinding
    private val memoryViews = mutableListOf<View>()
    private val ADD_MEMORY_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Receive currentSteps from HomeActivity (default to 0 if not passed)
        val currentSteps = intent.getIntExtra("currentSteps", 0)

        binding.btnAddPlace.setOnClickListener {
            val intent = Intent(this, AddMemoryActivity::class.java)
            intent.putExtra("currentSteps", currentSteps)
            startActivityForResult(intent, ADD_MEMORY_REQUEST_CODE)        }

        binding.backButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        loadPlacesFromRoom()
    }

    private fun loadPlacesFromRoom() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = PlaceDatabase.getDatabase(applicationContext)
            val places = db.placeDao().getAll()

            withContext(Dispatchers.Main) {
                binding.memoryContainer.removeAllViews()
                binding.placeListContainer.removeAllViews()
                memoryViews.clear()

                if (places.isNotEmpty()) {
                    // Show static section for first memory
                    showFirstPlaceStatic(places.first())

                    // Add dynamic places (2, 3, ...) and buttons
                    places.drop(1).forEachIndexed { index, place ->
                        addMemory(place, index)
                    }
                }
                binding.contentContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun showFirstPlaceStatic(place: Place) {
        try {
            binding.imgMemory.setImageURI(Uri.parse(place.imageUri))
        } catch (e: Exception) {
            binding.imgMemory.setImageResource(R.drawable.ic_launcher_background)
        }

        binding.txtPlaceName.text = place.name

        val infoLayout = (binding.txtPlaceName.parent as ViewGroup).getChildAt(2) as LinearLayout
        val row1 = infoLayout.getChildAt(0) as LinearLayout
        val row2 = infoLayout.getChildAt(1) as LinearLayout
        val row3 = infoLayout.getChildAt(2) as LinearLayout
        val row4 = infoLayout.getChildAt(3) as LinearLayout

        (row1.getChildAt(1) as TextView).text = "👣 Steps: ${place.steps_taken}"
        (row2.getChildAt(1) as TextView).text = "📅 Date: ${place.date_saved}"
        (row3.getChildAt(1) as TextView).text = "📝 ${place.description}"
        (row4.getChildAt(1) as TextView).text = "⭐️ Rating: ${place.rating}/5"    }

    private fun addMemory(place: Place, index: Int) {
        val context = this

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // start hidden
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                val margin = resources.getDimensionPixelSize(R.dimen.default_margin)
                (it as ViewGroup.MarginLayoutParams).setMargins(margin)
            }
        }

        val imgView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
            scaleType = ImageView.ScaleType.CENTER_CROP

            try {
                val uri = Uri.parse(place.imageUri)
                setImageURI(uri)
            } catch (e: Exception) {
                Log.e("MemoryActivity", "Image failed to load: ${place.imageUri}")
                setImageResource(R.drawable.ic_launcher_background)
            }
        }

        val placeName = TextView(context).apply {
            text = "📍 Location: ${place.name}"
            textSize = 18f
            setTextColor(getColor(R.color.primary_green))
        }

        val steps = TextView(context).apply {
            text = "👣 Steps: ${place.steps_taken}"
            textSize = 16f
        }

        val date = TextView(context).apply {
            text = "📅 Date: ${place.date_saved}"
            textSize = 16f
        }
        val description = TextView(context).apply {
            text = "📝 ${place.description}"
            textSize = 16f
        }

        val ratingBar = RatingBar(context).apply {
            numStars = 5
            stepSize = 0.5f
            rating = place.rating
            setIsIndicator(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(imgView)
        layout.addView(placeName)
        layout.addView(steps)
        layout.addView(date)
        layout.addView(description)
        layout.addView(ratingBar)


        memoryViews.add(layout)
        binding.memoryContainer.addView(layout)

        // Button (2, 3, ...)
        val button = Button(context).apply {
            text = (index + 2).toString()
            layoutParams = LinearLayout.LayoutParams(150, 150).apply {
                setMargins(8)
            }
            setOnClickListener {
                memoryViews.forEachIndexed { i, view ->
                    view.visibility = if (i == index) View.VISIBLE else View.GONE
                }
            }
        }

        binding.placeListContainer.addView(button)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_MEMORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadPlacesFromRoom() // Refresh the list
        }
    }

}
