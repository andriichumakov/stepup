package com.example.stepupapp.models

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UserProfile(
    val id: String?,
    val name: String,
    val email: String,
    @SerialName("pfp_url") val pfpUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null
)
