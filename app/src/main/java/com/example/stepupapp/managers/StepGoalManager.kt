package com.example.stepupapp.managers

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import com.example.stepupapp.UserPreferences

class StepGoalManager(private val context: Context) {
    
    companion object {
        private const val DEFAULT_STEP_GOAL = 6000
        
        val PRESET_GOALS = listOf(5000, 6500, 8000)
    }
    
    fun setStepTarget(target: Int, stepTargetEditText: EditText? = null): Boolean {
        return if (target > 0) {
            UserPreferences.setStepTarget(context, target)
            stepTargetEditText?.setText(target.toString())
            Toast.makeText(context, "Step target set to $target", Toast.LENGTH_SHORT).show()
            true
        } else {
            false
        }
    }
    
    fun getStepTargetFromInput(stepTargetText: String, customStepsPlaceholder: String): Int? {
        return try {
            if (stepTargetText != customStepsPlaceholder && stepTargetText.isNotEmpty()) {
                val target = stepTargetText.toInt()
                if (target > 0) target else null
            } else {
                null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    fun getDefaultStepGoal(): Int = DEFAULT_STEP_GOAL
} 