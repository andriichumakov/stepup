package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.databinding.LoginPageBinding
import com.example.stepupapp.services.ProfileService
import com.example.stepupapp.storage.LocalProfileStore
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    private lateinit var binding: LoginPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // LOGIN BUTTON
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            lifecycleScope.launch {
                val profile = ProfileService.login(this@LoginActivity, email, password)
                if (profile != null) {
                    LocalProfileStore.addOrUpdateProfile(applicationContext, profile)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Login failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // REGISTER LINK
        binding.registerPrompt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
