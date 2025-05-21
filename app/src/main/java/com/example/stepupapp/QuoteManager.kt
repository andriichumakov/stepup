package com.example.stepupapp

data class Quote(
    val text: String,
    val author: String
)

class QuoteManager {
    companion object {
        private val quotes = listOf(
            Quote("Every step is progress, no matter how small.", "Anonymous"),
            Quote("Go walk! Or dont. I can't force you to do anything.", "The app"),
            Quote("Walking is a man's best medicine.", "Hippocrates"),
            Quote("The journey of a thousand miles begins with a single step.", "Lao Tzu"),
            Quote("If you need an app to remind you to walk, you're already cooked.", "Ancient Wisdom"),
            Quote("An early-morning walk is a blessing for the whole day.", "Henry David Thoreau"),
            Quote("Walking is the great adventure, the first meditation, a practice of heartiness and soul primary to humankind.", "Gary Snyder"),
            Quote("You should walk, no, run. I'm approaching.", "Andrii"),
            Quote("Walking is the best way to get closer to nature and yourself.", "Anonymous"),
            Quote("A walk in nature walks the soul back home.", "Mary Davis"),
            Quote("Walking is a simple way to take care of your body and mind.", "Anonymous"),
            Quote("The best time to walk is now.", "Anonymous"),
            Quote("Walking is the most ancient exercise and still the best modern exercise.", "Carrie Latet"),
            Quote("Go touch some grass.", "Anastasia"),
            Quote("Take a walk. It will clear your mind and lift your spirits.", "Anonymous")
        )

        fun getRandomQuote(): Quote {
            return quotes.random()
        }
    }
} 