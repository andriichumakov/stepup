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
    val nickname: String? = null,
    val step_goal: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
    @SerialName("interests_code") val interestsCode: String? = null,
    @SerialName("pfp_64base") val pfp64Base: String? = null
)
