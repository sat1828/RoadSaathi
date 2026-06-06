package com.roadsaathi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_sessions")
data class DriverSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fcmToken: String,
    val lastLat: Double,
    val lastLng: Double,
    val lastSeen: Long
)
