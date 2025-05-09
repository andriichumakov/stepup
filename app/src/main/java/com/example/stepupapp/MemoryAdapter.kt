package com.example.stepup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stepupapp.databinding.ItemMemoryBinding

class MemoryAdapter(private val memories: List<Memory>) :
    RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder>() {

    inner class MemoryViewHolder(val binding: ItemMemoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val binding = ItemMemoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val memory = memories[position]
        holder.binding.imgMemory.setImageResource(memory.imageRes)
        holder.binding.txtDate.text = "Date: ${memory.date}"
        holder.binding.txtLocation.text = "Location: ${memory.location}"
    }

    override fun getItemCount(): Int = memories.size
}
