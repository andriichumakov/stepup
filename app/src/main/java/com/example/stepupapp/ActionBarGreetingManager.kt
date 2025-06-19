package com.example.stepupapp

import android.app.Activity
import android.view.View
import android.widget.TextView

class ActionBarGreetingManager(private val activity: Activity) {
    
    fun updateGreeting() {
        val userNickname = UserPreferences.getUserNickname(activity)
        val greetingTextView = activity.findViewById<TextView>(R.id.actionbar_greeting)
        
        if (greetingTextView != null) {
            if (userNickname.isNotEmpty()) {
                greetingTextView.text = activity.getString(R.string.greeting_hello, userNickname)
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