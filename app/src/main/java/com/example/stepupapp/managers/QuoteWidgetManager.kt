package com.example.stepupapp.managers

import com.example.stepupapp.QuoteManager
import com.example.stepupapp.databinding.ActivityHomeBinding

class QuoteWidgetManager(private val binding: ActivityHomeBinding) {
    
    fun initialize() {
        updateQuote()
        setupQuoteRefresh()
    }
    
    fun updateQuote() {
        val quote = QuoteManager.getRandomQuote()
        binding.quoteText.text = quote.text
        binding.quoteAuthor.text = "— ${quote.author}"
    }
    
    private fun setupQuoteRefresh() {
        binding.refreshQuoteButton.setOnClickListener {
            updateQuote()
        }
    }
} 