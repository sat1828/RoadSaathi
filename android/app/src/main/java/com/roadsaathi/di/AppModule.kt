package com.roadsaathi.di

import android.content.Context
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.roadsaathi.data.local.RoadSaathiDatabase
import com.roadsaathi.data.local.dao.DriverSessionDao
import com.roadsaathi.data.local.dao.HazardReportDao
import com.roadsaathi.data.remote.ApiClient
import com.roadsaathi.data.repository.ImageRepository
import com.roadsaathi.ml.TFLiteClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoadSaathiDatabase {
        return RoadSaathiDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideHazardReportDao(database: RoadSaathiDatabase): HazardReportDao {
        return database.hazardReportDao()
    }

    @Provides
    @Singleton
    fun provideDriverSessionDao(database: RoadSaathiDatabase): DriverSessionDao {
        return database.driverSessionDao()
    }

    @Provides
    @Singleton
    fun provideTFLiteClassifier(@ApplicationContext context: Context): TFLiteClassifier {
        return TFLiteClassifier(context)
    }

    @Provides
    @Singleton
    fun provideApiClient(@ApplicationContext context: Context): ApiClient {
        return ApiClient(context)
    }

    @Provides
    @Singleton
    fun provideImageRepository(@ApplicationContext context: Context): ImageRepository {
        return ImageRepository(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}
