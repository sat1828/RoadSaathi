package com.roadsaathi.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DriverSessionRequest(
    @SerializedName("fcm_token")
    val fcmToken: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("heading")
    val heading: Float? = null,
    @SerializedName("speed_kmh")
    val speedKmh: Float? = null
)
