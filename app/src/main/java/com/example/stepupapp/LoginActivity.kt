package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import com.example.stepupapp.databinding.LoginPageBinding
import com.example.stepupapp.databinding.RegisterPageBinding

class LoginActivity : BaseActivity() {
    private lateinit var binding: LoginPageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.loginButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}