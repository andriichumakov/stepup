package com.example.stepupapp

import android.app.Activity
import android.view.View
import android.widget.TextView

class ActionBarGreetingManager(private val activity: Activity) {
    
    fun updateGreeting() {
        val userName = UserPreferences.getUserName(activity)
        val greetingTextView = activity.findViewById<TextView>(R.id.actionbar_greeting)
        
        if (greetingTextView != null) {
            if (userName.isNotEmpty()) {
                greetingTextView.text = activity.getString(R.string.greeting_hello, userName)
            } else {
                greetingTextView.text = activity.getString(R.string.greeting_default)
            }
        }
    }
    
    fun updateGreetingWithCustomName(name: String) {
        val greetingTextView = activity.findViewById<TextView>(R.id.actionbar_greeting)
        
        if (greetingTextView != null) {
            if (name.isNotEmpty()) {
                greetingTextView.text = activity.getString(R.string.greeting_hello, name)
            } else {
                greetingTextView.text = activity.getString(R.string.greeting_default)
            }
        }
    }
} 