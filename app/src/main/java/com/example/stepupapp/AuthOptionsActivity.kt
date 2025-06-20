package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import com.example.stepupapp.databinding.LoginMethodBinding

class AuthOptionsActivity : BaseActivity() {
    private lateinit var binding: LoginMethodBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginMethodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        binding.registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}