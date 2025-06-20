package com.example.stepupapp.managers

import com.example.stepupapp.databinding.SetupPageBinding

class InterestSelectionManager {
    
    fun getSelectedInterests(binding: SetupPageBinding): Set<String> {
        val selectedInterests = mutableSetOf<String>()
        
        val interestMappings = mapOf(
            binding.checkAmusements to "Amusements",
            binding.checkArchitecture to "Architecture",
            binding.checkCultural to "Cultural",
            binding.checkShops to "Shops",
            binding.checkFoods to "Foods",
            binding.checkSport to "Sport",
            binding.checkHistorical to "Historical",
            binding.checkNatural to "Natural",
            binding.checkOther to "Other"
        )
        
        interestMappings.forEach { (checkbox, interest) ->
            if (checkbox.isChecked) {
                selectedInterests.add(interest)
            }
        }
        
        // If no interests selected, default to showing all
        if (selectedInterests.isEmpty()) {
            selectedInterests.add("All")
        }
        
        return selectedInterests
    }
} 