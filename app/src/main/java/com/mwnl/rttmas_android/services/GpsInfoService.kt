package com.mwnl.rttmas_android.services

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.mwnl.rttmas_android.models.GpsInfo

class GpsInfoService {

    var isGpsEnabled = false


    @RequiresApi(Build.VERSION_CODES.P)
    fun setupGpsStatusReceiver(activity: Activity) {

        val locationManager = activity.getSystemService(LOCATION_SERVICE) as LocationManager
        isGpsEnabled = locationManager.isLocationEnabled

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isGpsEnabled = locationManager.isLocationEnabled
            }
        }

        activity.registerReceiver(receiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
    }


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

