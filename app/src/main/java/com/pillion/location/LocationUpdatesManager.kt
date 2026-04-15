package com.pillion.location

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class LocationSample(
    val lat: Double,
    val lng: Double,
    val speedMps: Float,
)

class LocationUpdatesManager(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
) {
    @SuppressLint("MissingPermission")
    fun locationFlow(intervalMillis: Long = 5000L): Flow<LocationSample> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(
                        LocationSample(
                            lat = location.latitude,
                            lng = location.longitude,
                            speedMps = location.speed,
                        )
                    )
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose {
            fusedLocationProviderClient.removeLocationUpdates(callback)
        }
    }
}
