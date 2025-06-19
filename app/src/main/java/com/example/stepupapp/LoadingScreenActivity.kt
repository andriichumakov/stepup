package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.databinding.LoadingScreenBinding
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoadingScreenActivity : BaseActivity() {
    private lateinit var binding: LoadingScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoadingScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start session check immediately
        startSessionCheck()
    }

    private fun setProgressBar(progress: Int) {
        binding.progressBar.progress = progress
    }

    private fun startSessionCheck() {
        lifecycleScope.launch {
            setProgressBar(20)

            // Try to restore session from refresh token
            val restored = withContext(Dispatchers.IO) {
                ProfileService.restoreSessionFromToken(applicationContext)
            }

            setProgressBar(80)

            delay(300)  // Small artificial delay to smooth visual experience
            setProgressBar(100)

            if (restored) {
                if (ProfileService.hasSetStepGoal())
                {
                    goToHomeActivity()
                    return@launch
                }
                goToMainActivity()
            } else {
                goToAuthOptionsActivity()
            }
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToAuthOptionsActivity() {
        val intent = Intent(this, AuthOptionsActivity::class.java)
        startActivity(intent)
        finish()
    }
}
