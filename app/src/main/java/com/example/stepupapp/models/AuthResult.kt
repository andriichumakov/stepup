package com.example.stepupapp.models

sealed class AuthResult<out T> {
    data class Success<T>(val data: T): AuthResult<T>()
    data class Error(val message: String): AuthResult<Nothing>()
}
