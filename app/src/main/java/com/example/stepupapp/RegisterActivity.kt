package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import com.example.stepupapp.databinding.ActivityHomeBinding
import com.example.stepupapp.databinding.RegisterPageBinding

class RegisterActivity : BaseActivity() {
    private lateinit var binding: RegisterPageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.registerButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}