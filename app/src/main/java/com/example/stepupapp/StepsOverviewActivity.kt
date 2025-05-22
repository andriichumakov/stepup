package com.example.stepupapp

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepupapp.databinding.ActivityStepsOverviewBinding

class StepsOverviewActivity : BaseActivity() {
    private lateinit var binding: ActivityStepsOverviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepsOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up back button
        binding.backButton.setOnClickListener {
            finish() // This will close the current activity and return to the previous one (Home)
        }

        // Set up RecyclerView
//        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        // TODO: Set up adapter with sample data
    }
}