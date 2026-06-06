package com.roadsaathi.presentation.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.roadsaathi.service.FirebaseMessagingService
import com.roadsaathi.presentation.common.components.PendingBadge

@Composable
fun MapScreen(
    onNavigateToCamera: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // permissions granted
        }
    }

    LaunchedEffect(Unit) {
        FirebaseMessagingService.alertFlow.collect { alert ->
            viewModel.showAlert(
                AlertData(
                    title = alert.title,
                    message = alert.message,
                    hazardType = alert.hazardType,
                    distance = 0f
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.autoRefreshHeatmap()
    }

    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (fineLocation != PackageManager.PERMISSION_GRANTED &&
            coarseLocation != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            onResume()
            getMapAsync { googleMap ->
                googleMap.uiSettings.isMyLocationButtonEnabled = false
                googleMap.uiSettings.isZoomControlsEnabled = true

                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    googleMap.isMyLocationEnabled = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000
        ).setMinUpdateDistance(10f).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    viewModel.updateUserLocation(location)
                    mapView.getMapAsync { googleMap ->
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude),
                                15f
                            )
                        )
                        viewModel.loadHeatmap(location.latitude, location.longitude)

                        googleMap.clear()
                        val clusters = uiState.heatmapClusters
                        for (cluster in clusters) {
                            val position = LatLng(cluster.latitude, cluster.longitude)
                            googleMap.addMarker(
                                MarkerOptions()
                                    .position(position)
                                    .title("${cluster.hazardType} (${cluster.count})")
                                    .snippet("Severity: ${cluster.severity}")
                                    .icon(
                                        BitmapDescriptorFactory.defaultMarker(
                                            getHue(cluster.hazardType)
                                        )
                                    )
                            )
                        }
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateUserLocation(location)
                    viewModel.loadHeatmap(location.latitude, location.longitude)
                    mapView.getMapAsync { googleMap ->
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude),
                                15f
                            )
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        uiState.showIncomingAlert?.let { alert ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(),
                exit = slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = alert.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(onClick = { viewModel.onAlertDismiss() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        mapView.getMapAsync { googleMap ->
                            googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    15f
                                )
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(40.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "My Location",
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            FloatingActionButton(
                onClick = onNavigateToCamera,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Report Hazard")
            }

            if (uiState.pendingReportCount > 0) {
                PendingBadge(
                    count = uiState.pendingReportCount,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = (-4).dp, end = (-4).dp)
                )
            }
        }
    }
}

private fun getHue(hazardType: String): Float {
    return when {
        hazardType.contains("POTHOLE") || hazardType.contains("pothole") -> 30f
        hazardType.contains("WATER") || hazardType.contains("water") -> 210f
        hazardType.contains("ACCIDENT") || hazardType.contains("accident") -> 0f
        hazardType.contains("SIGNAGE") || hazardType.contains("signage") -> 60f
        hazardType.contains("COLLAPSE") || hazardType.contains("collapse") -> 15f
        else -> 120f
    }
}
