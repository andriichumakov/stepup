package com.example.stepupapp

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREFS_NAME = "StepUpPrefs"
    private const val KEY_STEP_TARGET = "step_target"
    private const val DEFAULT_STEP_TARGET = 6000

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getStepTarget(context: Context): Int {
        return getPrefs(context).getInt(KEY_STEP_TARGET, DEFAULT_STEP_TARGET)
    }

    fun setStepTarget(context: Context, target: Int) {
        getPrefs(context).edit().putInt(KEY_STEP_TARGET, target).apply()
    }
}