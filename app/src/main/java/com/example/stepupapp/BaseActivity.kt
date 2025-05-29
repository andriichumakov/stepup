package com.example.stepupapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Base activity class that all activities should extend
 */
open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
} 