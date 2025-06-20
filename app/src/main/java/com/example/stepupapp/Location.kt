package com.example.stepupapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Location(
    val name: String,
    val type: String,
    val rating: Double,
    val openingHours: String = "",
    val description: String = "",
    val address: String = "",
    val imageUrl: String = ""
) : Parcelable
