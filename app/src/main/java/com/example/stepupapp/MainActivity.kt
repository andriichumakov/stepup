package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.stepupapp.databinding.SetupPageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private lateinit var binding: SetupPageBinding
    private var hasUserSetTarget = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SetupPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up preset step target buttons
        binding.button.setOnClickListener { 
            setStepTarget(5000)
            hasUserSetTarget = true
        }
        binding.button3.setOnClickListener { 
            setStepTarget(6500)
            hasUserSetTarget = true
        }
        binding.button4.setOnClickListener { 
            setStepTarget(8000)
            hasUserSetTarget = true
        }

        // Set up continue button
        binding.button10.setOnClickListener {
            try {
                // Check if custom step target is entered
                val customSteps = binding.editTextNumber.text.toString()
                if (customSteps != getString(R.string.custom_steps)) {
                    val target = customSteps.toInt()
                    if (target > 0) {
                        setStepTarget(target)
                        hasUserSetTarget = true
                    } else {
                        Toast.makeText(this, "Please enter a valid step target", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                // If user hasn't set a target (either through buttons or custom input), use default
                if (!hasUserSetTarget) {
                    setStepTarget(6000) // Default target
                }

                // Proceed to home activity
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setStepTarget(target: Int) {
        UserPreferences.setStepTarget(this, target)
        Toast.makeText(this, "Step target set to $target", Toast.LENGTH_SHORT).show()
    }
}
