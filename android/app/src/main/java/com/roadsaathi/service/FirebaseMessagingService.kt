package com.roadsaathi.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.roadsaathi.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "hazard_alerts"
        const val NOTIFICATION_ID_BASE = 2000

        private val _alertFlow = MutableSharedFlow<HazardAlert>(extraBufferCapacity = 10)
        val alertFlow: SharedFlow<HazardAlert> = _alertFlow
    }

    data class HazardAlert(
        val title: String,
        val message: String,
        val hazardType: String,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val remoteId: String? = null
    )

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token is sent to server via DriverSessionRepository when session is created
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"]

        if (type == "HAZARD_ALERT" || type == "hazard_alert") {
            val aiBrief = data["aiBrief"] ?: ""
            val alert = HazardAlert(
                title = "Road Hazard Alert",
                message = aiBrief.ifBlank { "Hazard reported ahead" },
                hazardType = data["hazardType"] ?: "unknown",
                latitude = data["lat"]?.toDoubleOrNull() ?: 0.0,
                longitude = data["lng"]?.toDoubleOrNull() ?: 0.0,
                remoteId = data["clusterId"]
            )

            scope.launch {
                _alertFlow.emit(alert)
            }

            showNotification(alert)
        }
    }

    private fun showNotification(alert: HazardAlert) {
        createNotificationChannel()

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + (alert.remoteId?.hashCode() ?: 0), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hazard Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for nearby road hazards"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
