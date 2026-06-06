package com.roadsaathi.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_at")
    val expiresAt: Long,
    @SerializedName("user")
    val user: UserDto
)

data class UserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("role")
    val role: String
)
