package com.roadsaathi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.roadsaathi.data.local.dao.DriverSessionDao
import com.roadsaathi.data.local.dao.HazardReportDao
import com.roadsaathi.data.local.entity.DriverSessionEntity
import com.roadsaathi.data.local.entity.HazardReportEntity

@Database(
    entities = [HazardReportEntity::class, DriverSessionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RoadSaathiDatabase : RoomDatabase() {

    abstract fun hazardReportDao(): HazardReportDao
    abstract fun driverSessionDao(): DriverSessionDao

    companion object {
        @Volatile
        private var INSTANCE: RoadSaathiDatabase? = null

        fun getInstance(context: Context): RoadSaathiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoadSaathiDatabase::class.java,
                    "roadsaathi.db"
                )
                    .enableWAL(true)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
