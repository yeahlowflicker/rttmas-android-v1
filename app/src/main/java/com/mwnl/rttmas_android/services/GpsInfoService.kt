package com.mwnl.rttmas_android.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.util.Consumer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.mwnl.rttmas_android.models.GpsInfo
import java.lang.String
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class GpsInfoService {


    @SuppressLint("MissingPermission")
    /**
     *  Obtain the last known GPS information from device system service.
     *
     *  @param  [Context] context - The activity context.
     *  @return [GpsInfo] The obtained GPS info record. Null if procedure failed.
     */
    fun getGpsLocationInfo(context: Context): GpsInfo? {

        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        return try {
            // Get the last known location synchronously
            val task = fusedLocationClient.lastLocation
            Tasks.await(task) // This will block until the task is complete

            val gpsInfo = GpsInfo(
                task.result.time,           // UNIX, in milliseconds
                task.result.latitude,
                task.result.longitude,
                task.result.speed,          // in m/s
                task.result.bearing         // in degrees
            )

            return gpsInfo
        } catch (e: Exception) {
            // Handle exceptions (e.g., no location available)
            null
        }
    }
}

