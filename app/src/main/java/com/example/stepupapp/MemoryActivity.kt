package com.example.stepupapp

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddPlace.setOnClickListener {
            val intent = Intent(this, AddMemoryActivity::class.java)
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
        (row3.getChildAt(1) as TextView).text = "🕒 Recently visited"
        (row4.getChildAt(1) as TextView).text = "🎵 Vibe memory"
    }

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

        layout.addView(imgView)
        layout.addView(placeName)
        layout.addView(steps)
        layout.addView(date)

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
}
