package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.stepupapp.databinding.LoadingScreenBinding
import com.example.stepupapp.managers.SessionManager
import com.example.stepupapp.services.ProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoadingScreenActivity : BaseActivity() {
    private lateinit var binding: LoadingScreenBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoadingScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize session manager
        sessionManager = SessionManager(this)
        sessionManager.initialize(createSessionCallback())

        // Start session check immediately
        startSessionCheck()
    }

    private fun setProgressBar(progress: Int) {
        binding.progressBar.progress = progress
    }

    private fun startSessionCheck() {
        lifecycleScope.launch {
            setProgressBar(20)

            // Try to restore session using the session manager
            val result = withContext(Dispatchers.IO) {
                sessionManager.restoreSession()
            }

            setProgressBar(80)

            delay(300)  // Small artificial delay to smooth visual experience
            setProgressBar(100)

            when (result) {
                SessionManager.SessionRestoreResult.SUCCESS -> {
                    if (ProfileService.hasSetStepGoal()) {
                        goToHomeActivity()
                    } else {
                        goToMainActivity()
                    }
                }
                SessionManager.SessionRestoreResult.NO_SESSION,
                SessionManager.SessionRestoreResult.INVALID_TOKEN,
                SessionManager.SessionRestoreResult.STORAGE_CORRUPTED,
                SessionManager.SessionRestoreResult.ERROR -> {
                    goToAuthOptionsActivity()
                }
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

    private fun createSessionCallback() = object : SessionManager.SessionCallback {
        override fun onSessionRestored() {
            // Session restored successfully - handled in startSessionCheck
        }

        override fun onSessionFailed() {
            // Session failed - handled in startSessionCheck
        }

        override fun onStorageCorrupted() {
            // Storage was corrupted but has been cleaned up
            // User will need to sign in again
        }

        override fun onNoSession() {
            // No stored session found - user needs to sign in
        }
    }
}
