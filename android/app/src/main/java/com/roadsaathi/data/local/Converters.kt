package com.roadsaathi.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = try {
        SyncStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        SyncStatus.PENDING
    }
}
