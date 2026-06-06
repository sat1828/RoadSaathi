package com.roadsaathi.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.roadsaathi.R
import com.roadsaathi.data.local.dao.HazardReportDao
import com.roadsaathi.data.local.entity.HazardReportEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HazardProximityService : Service() {

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient

    @Inject
    lateinit var hazardReportDao: HazardReportDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                serviceScope.launch {
                    checkProximity(location)
                }
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "roadsaathi_location"
        const val NOTIFICATION_ID = 1001
        private const val PROXIMITY_RADIUS_METERS = 500f
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RoadSaathi Location",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Location tracking for hazard proximity alerts"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RoadSaathi")
            .setContentText("Monitoring nearby hazards...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L
        ).setMinUpdateDistanceMeters(10f).build()

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private suspend fun checkProximity(currentLocation: Location) {
        val allActive = hazardReportDao.getAllReports().first()
        val nearby = allActive.filter { entity ->
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                entity.latitude, entity.longitude,
                results
            )
            results[0] <= PROXIMITY_RADIUS_METERS
        }
        if (nearby.isNotEmpty()) {
            showProximityAlert(nearby)
        }
    }

    private fun showProximityAlert(reports: List<HazardReportEntity>) {
        val types = reports.map { it.hazardType }.distinct().joinToString(", ")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hazard Nearby")
            .setContentText("$types detected within $PROXIMITY_RADIUS_METERS meters")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(1002, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
