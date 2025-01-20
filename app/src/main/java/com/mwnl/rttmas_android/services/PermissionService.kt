package com.mwnl.rttmas_android.services

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionService {

    // Permissions
    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        "android.permission.CAMERA",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_FINE_LOCATION"
    )

    @SuppressLint("HardwareIds")
    /**
     *  Obtain the Android device ID.
     *  This ID is unique for each device.
     *
     *  @param  [Context] context - Pass the activity context here
     *  @return [string] the obtained device ID
     */
    fun getAndroidDeviceID(context: Context) : String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }


    /**
     *  Request app permissions from user.
     *
     *  @param [Activity] activity - The caller activity
     */
    fun requestAppPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }


    /**
     *  Check whether all required app permissions have been granted.
     *
     *  @param  [Context] context - Pass the activity context here
     *  @return [Boolean] True if permissions are granted, false otherwise
     */
    private fun checkIfAppPermissionsGranted(context: Context): Boolean {
        for (permission in REQUIRED_PERMISSIONS)
            if (ContextCompat.checkSelfPermission( context, permission ) != PackageManager.PERMISSION_GRANTED)
                return false

        return true
    }

}