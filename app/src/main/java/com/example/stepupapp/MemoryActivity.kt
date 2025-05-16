package com.example.stepupapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepupapp.databinding.ActivityMemoryBinding
import com.example.stepup.Memory
import com.example.stepup.MemoryAdapter
import com.example.stepupapp.R

class MemoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryBinding
    private lateinit var memoryAdapter: MemoryAdapter
    private val memoryList = mutableListOf<Memory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memoryAdapter = MemoryAdapter(memoryList)
        binding.recyclerViewMemories.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMemories.adapter = memoryAdapter

        // Example data
        memoryList.add(
            Memory(
                imageRes = R.drawable.ic_launcher_background,
                date = "2025-05-09",
                location = "Hanoi, Vietnam"
            )
        )
        memoryAdapter.notifyDataSetChanged()
    }
}
