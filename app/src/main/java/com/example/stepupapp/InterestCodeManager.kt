package com.example.stepupapp

import android.util.Log

/**
 * Utility class to manage interest code mapping between string names and numeric codes
 * Interest codes:
 * 1 = Amusements
 * 2 = Architecture  
 * 3 = Cultural
 * 4 = Shops
 * 5 = Foods
 * 6 = Sport
 * 7 = Historical
 * 8 = Natural
 * 9 = Other
 */
object InterestCodeManager {
    private const val TAG = "InterestCodeManager"
    
    // Map interest names to numeric codes
    private val interestToCodeMap = mapOf(
        "Amusements" to 1,
        "Architecture" to 2,
        "Cultural" to 3,
        "Shops" to 4,
        "Foods" to 5,
        "Sport" to 6,
        "Historical" to 7,
        "Natural" to 8,
        "Other" to 9
    )
    
    // Reverse map for code to interest name conversion
    private val codeToInterestMap = interestToCodeMap.entries.associate { (k, v) -> v to k }
    
    /**
     * Convert a set of interest names to a numeric code string
     * Example: ["Amusements", "Cultural", "Foods", "Sport"] -> "1356"
     */
    fun interestsToCode(interests: Set<String>): String {
        if (interests.isEmpty() || interests.contains("All")) {
            return "" // Empty code means all interests
        }
        
        val codes = interests.mapNotNull { interest ->
            interestToCodeMap[interest]?.also {
                Log.d(TAG, "Mapped interest '$interest' to code $it")
            } ?: run {
                Log.w(TAG, "Unknown interest: $interest")
                null
            }
        }.sorted() // Sort to ensure consistent ordering
        
        val result = codes.joinToString("")
        Log.d(TAG, "Converted interests $interests to code: $result")
        return result
    }
    
    /**
     * Convert a numeric code string to a set of interest names
     * Example: "1356" -> ["Amusements", "Cultural", "Foods", "Sport"]
     */
    fun codeToInterests(code: String?): Set<String> {
        if (code.isNullOrEmpty()) {
            Log.d(TAG, "Empty or null code, returning 'All' interests")
            return setOf("All")
        }
        
        val interests = mutableSetOf<String>()
        
        // Parse each digit in the code string
        for (char in code) {
            val digit = char.digitToIntOrNull()
            if (digit != null) {
                codeToInterestMap[digit]?.let { interest ->
                    interests.add(interest)
                    Log.d(TAG, "Mapped code $digit to interest '$interest'")
                } ?: Log.w(TAG, "Unknown interest code: $digit")
            } else {
                Log.w(TAG, "Invalid character in interest code: $char")
            }
        }
        
        val result = if (interests.isEmpty()) setOf("All") else interests
        Log.d(TAG, "Converted code '$code' to interests: $result")
        return result
    }
    
    /**
     * Get all available interest names
     */
    fun getAllInterestNames(): Set<String> {
        return interestToCodeMap.keys
    }
    
    /**
     * Check if an interest name is valid
     */
    fun isValidInterest(interest: String): Boolean {
        return interestToCodeMap.containsKey(interest)
    }
    
    /**
     * Get the code for a specific interest
     */
    fun getCodeForInterest(interest: String): Int? {
        return interestToCodeMap[interest]
    }
    
    /**
     * Get the interest name for a specific code
     */
    fun getInterestForCode(code: Int): String? {
        return codeToInterestMap[code]
    }
    
    /**
     * Validate that a code string only contains valid interest codes
     */
    fun isValidCode(code: String): Boolean {
        if (code.isEmpty()) return true // Empty is valid (means all)
        
        return code.all { char ->
            val digit = char.digitToIntOrNull()
            digit != null && codeToInterestMap.containsKey(digit)
        }
    }
    
    /**
     * Get a human-readable description of interests from code
     */
    fun getInterestsDescription(code: String?): String {
        val interests = codeToInterests(code)
        return when {
            interests.contains("All") -> "All interests"
            interests.size == 1 -> interests.first()
            interests.size <= 3 -> interests.joinToString(", ")
            else -> "${interests.take(2).joinToString(", ")} and ${interests.size - 2} more"
        }
    }
} 