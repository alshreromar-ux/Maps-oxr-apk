package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.model.LatLngPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object LocationHelper {
    // Riyadh default fallback center when location permission or GPS fix is not yet available
    val DEFAULT_CITY_CENTER = LatLngPoint(24.7136, 46.6753)

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): LatLngPoint? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
            val cts = CancellationTokenSource()
            val fresh: Location? = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).await()
            val resolved = fresh ?: client.lastLocation.await()
            resolved?.let { LatLngPoint(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }
}
