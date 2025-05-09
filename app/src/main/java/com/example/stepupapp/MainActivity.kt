package com.example.stepupapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepup.Memory
import com.example.stepup.MemoryAdapter
import com.example.stepupapp.R
import com.example.stepupapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var memoryAdapter: MemoryAdapter
    private val memoryList = mutableListOf<Memory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memoryAdapter = MemoryAdapter(memoryList)
        binding.recyclerViewMemories.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMemories.adapter = memoryAdapter

        binding.btnAddMemory.setOnClickListener {
            val memory = Memory(
                imageRes = R.drawable.ic_launcher_background, // Or any image in drawable
                date = "2025-05-09",
                location = "Hanoi, Vietnam"
            )
            memoryList.add(0, memory)
            memoryAdapter.notifyItemInserted(0)
            binding.recyclerViewMemories.scrollToPosition(0)
        }
    }
}
