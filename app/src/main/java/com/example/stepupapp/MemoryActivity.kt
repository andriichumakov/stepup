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
    private lateinit var actionBarGreetingManager: ActionBarGreetingManager
    private lateinit var actionBarProfileManager: ActionBarProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize and setup ActionBar greeting
        actionBarGreetingManager = ActionBarGreetingManager(this)
        actionBarGreetingManager.updateGreeting()
        
        actionBarProfileManager = ActionBarProfileManager(this)
        actionBarProfileManager.updateProfilePicture()



        binding.btnAddPlace.setOnClickListener {
            val intent = Intent(this, AddMemoryActivity::class.java)
            val currentSteps = UserPreferences.getDailySteps(this, java.util.Date())
            intent.putExtra("currentSteps", currentSteps)
            startActivityForResult(intent, ADD_MEMORY_REQUEST_CODE)
        }

        binding.btnMemoryDashboard.setOnClickListener {
            val intent = Intent(this, MemoryDashboardActivity::class.java)
            startActivity(intent)
        }

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
        binding.txtSteps.text = "👣 Steps: ${place.steps_taken}"
        binding.txtDate.text = "📅 Date: ${place.date_saved}"
        binding.txtDescription.text = "📝 ${place.description}"
        binding.txtRating.text = "⭐️ Rating: ${place.rating}/5"
    }

    private fun addMemory(place: Place, index: Int) {
        val context = this

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // start hidden
            background = resources.getDrawable(R.drawable.memory_container_background, null)
            elevation = 8f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                val smallMargin = 8 // Reduced margins for wider container
                (it as ViewGroup.MarginLayoutParams).setMargins(smallMargin, 16, smallMargin, 24)
            }
        }

        val imgView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                650
            ).also {
                it.setMargins(0, 0, 0, 24)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = resources.getDrawable(R.drawable.memory_image_background, null)
            clipToOutline = true

            try {
                val uri = Uri.parse(place.imageUri)
                setImageURI(uri)
            } catch (e: Exception) {
                Log.e("MemoryActivity", "Image failed to load: ${place.imageUri}")
                setImageResource(R.drawable.ic_launcher_background)
            }
        }

        val placeName = TextView(context).apply {
            text = place.name
            textSize = 20f
            setTextColor(getColor(R.color.dark_green))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 0, 0, 16)
            }
        }

        val steps = TextView(context).apply {
            text = "👣 Steps: ${place.steps_taken}"
            textSize = 16f
            setTextColor(getColor(R.color.dark_blue))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 0, 0, 8)
            }
        }

        val date = TextView(context).apply {
            text = "📅 Date: ${place.date_saved}"
            textSize = 16f
            setTextColor(getColor(R.color.dark_blue))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 0, 0, 8)
            }
        }
        
        val description = TextView(context).apply {
            text = "📝 ${place.description}"
            textSize = 16f
            setTextColor(getColor(R.color.dark_blue))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 0, 0, 16)
            }
        }

        val ratingBar = RatingBar(context).apply {
            numStars = 5
            stepSize = 0.5f
            rating = place.rating
            setIsIndicator(true)
            progressTintList = resources.getColorStateList(R.color.gold_accent, null)
            secondaryProgressTintList = resources.getColorStateList(R.color.light_green, null)
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
