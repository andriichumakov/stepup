package com.example.stepupapp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import com.example.stepupapp.databinding.LoadingScreenBinding

class LoadingScreenActivity : BaseActivity() {
    private lateinit var binding: LoadingScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoadingScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startProgressBar()
    }

    private fun startProgressBar() {
        val totalTime = 5000L  // 5 seconds
        val interval = 50L     // Update every 50ms for smoother animation

        object : CountDownTimer(totalTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = ((totalTime - millisUntilFinished).toFloat() / totalTime * 100).toInt()
                binding.progressBar.progress = progress
            }

            override fun onFinish() {
                binding.progressBar.progress = 100
                goToMainActivity()
            }
        }.start()
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close loading screen so it doesn't stay in back stack
    }
}
