package com.example.stepupapp

data class Quote(
    val text: String,
    val author: String
)

class QuoteManager {
    companion object {
        private val quotes = listOf(
            Quote("Every step is progress, no matter how small.", "Anonymous"),
            Quote("Walking is the best possible exercise. Habituate yourself to walk very fast.", "Thomas Jefferson"),
            Quote("Walking is a man's best medicine.", "Hippocrates"),
            Quote("The journey of a thousand miles begins with a single step.", "Lao Tzu"),
            Quote("Walking is the perfect way of moving if you want to see into the life of things.", "Elizabeth von Arnim"),
            Quote("An early-morning walk is a blessing for the whole day.", "Henry David Thoreau"),
            Quote("Walking is the great adventure, the first meditation, a practice of heartiness and soul primary to humankind.", "Gary Snyder"),
            Quote("Walking is the natural recreation for a man who desires not absolutely to suppress his intellect but to turn it out to play for a season.", "Leslie Stephen"),
            Quote("Walking is the best way to get closer to nature and yourself.", "Anonymous"),
            Quote("A walk in nature walks the soul back home.", "Mary Davis"),
            Quote("Walking is a simple way to take care of your body and mind.", "Anonymous"),
            Quote("The best time to walk is now.", "Anonymous"),
            Quote("Walking is the most ancient exercise and still the best modern exercise.", "Carrie Latet"),
            Quote("Walking is a man's best friend.", "Anonymous"),
            Quote("Take a walk. It will clear your mind and lift your spirits.", "Anonymous")
        )

        fun getRandomQuote(): Quote {
            return quotes.random()
        }
    }
} 