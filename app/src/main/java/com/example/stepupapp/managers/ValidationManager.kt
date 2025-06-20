package com.example.stepupapp.managers

import android.content.Context
import com.example.stepupapp.R

class ValidationManager(private val context: Context) {
    
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )
    
    fun validateUserName(name: String): ValidationResult {
        return when {
            name.trim().isEmpty() -> ValidationResult(
                false,
                context.getString(R.string.error_name_required)
            )
            name.trim().length < 2 -> ValidationResult(
                false,
                context.getString(R.string.error_name_too_short)
            )
            else -> ValidationResult(true)
        }
    }
    
    fun validateStepTarget(stepTarget: String, customStepsPlaceholder: String): ValidationResult {
        return try {
            if (stepTarget == customStepsPlaceholder) {
                ValidationResult(false, "Please set a step target")
            } else {
                val target = stepTarget.toInt()
                if (target <= 0) {
                    ValidationResult(false, "Please enter a valid step target")
                } else {
                    ValidationResult(true)
                }
            }
        } catch (e: NumberFormatException) {
            ValidationResult(false, "Please enter a valid number")
        }
    }
    
    companion object {
        const val MIN_NAME_LENGTH = 2
        const val MIN_STEP_TARGET = 1
    }
} 