package com.example.realtimeweather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

fun getCurrentLocation(
    context: Context,
    onLocationReady: (String) -> Unit,
    onError: (String) -> Unit
) {
    // Kiểm tra quyền
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        onError("Location permission required")
        return
    }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) {
                val latLon = "${location.latitude},${location.longitude}"
                onLocationReady(latLon)
            } else {
                onError("Unable to get location")
            }
        }
        .addOnFailureListener { exception ->
            onError("Location error: ${exception.message}")
        }
}