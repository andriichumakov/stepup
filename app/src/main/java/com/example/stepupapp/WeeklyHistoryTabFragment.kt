package com.example.stepupapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.stepupapp.databinding.FragmentWeeklyHistoryTabBinding
import java.text.SimpleDateFormat
import java.util.*

class WeeklyHistoryTabFragment : Fragment() {
    private var _binding: FragmentWeeklyHistoryTabBinding? = null
    private val binding get() = _binding!!
    
    private var tabType: TabType = TabType.STEPS
    
    enum class TabType {
        STEPS, CALORIES, DISTANCE
    }
    
    companion object {
        private const val ARG_TAB_TYPE = "tab_type"
        
        fun newInstance(type: TabType): WeeklyHistoryTabFragment {
            return WeeklyHistoryTabFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TAB_TYPE, type.name)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tabType = TabType.valueOf(it.getString(ARG_TAB_TYPE, TabType.STEPS.name))
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeeklyHistoryTabBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateTabContent()
    }
    
    private fun updateTabContent() {
        val weeklyData = when (tabType) {
            TabType.STEPS -> UserPreferences.getWeeklySteps(requireContext())
            TabType.CALORIES -> UserPreferences.getWeeklyCalories(requireContext())
            TabType.DISTANCE -> UserPreferences.getWeeklyDistance(requireContext())
        }
        
        if (weeklyData.isEmpty()) {
            // Handle empty data
            return
        }
        
        // Update each day's display
        val days = listOf(
            Triple(binding.day1Label, binding.day1Progress, binding.day1Value),
            Triple(binding.day2Label, binding.day2Progress, binding.day2Value),
            Triple(binding.day3Label, binding.day3Progress, binding.day3Value),
            Triple(binding.day4Label, binding.day4Progress, binding.day4Value),
            Triple(binding.day5Label, binding.day5Progress, binding.day5Value),
            Triple(binding.day6Label, binding.day6Progress, binding.day6Value),
            Triple(binding.day7Label, binding.day7Progress, binding.day7Value)
        )
        
        val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        
        weeklyData.take(7).forEachIndexed { index, data ->
            if (index < days.size) {
                val (label, progress, value) = days[index]
                label.text = if (data.day == today) "Today" else data.day
                progress.max = data.target
                progress.progress = data.steps
                
                // Format the value based on tab type
                value.text = when (tabType) {
                    TabType.STEPS -> "${data.steps} steps"
                    TabType.CALORIES -> "${data.steps} Cal"
                    TabType.DISTANCE -> String.format("%.2f km", data.steps / 1000.0)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 