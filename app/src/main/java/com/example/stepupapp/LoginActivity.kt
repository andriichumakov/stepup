package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.databinding.LoginPageBinding
import com.example.stepupapp.models.UserProfile
import com.example.stepupapp.models.AuthResult
import com.example.stepupapp.services.ProfileService
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
                val result = ProfileService.login(this@LoginActivity, email, password)
                handleLoginResult(result)
            }
        }

        // REGISTER LINK
        binding.registerPrompt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleLoginResult(result: AuthResult<UserProfile>) {
        when (result) {
            is AuthResult.Success -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            is AuthResult.Error -> {
                Toast.makeText(this, "Login failed: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
