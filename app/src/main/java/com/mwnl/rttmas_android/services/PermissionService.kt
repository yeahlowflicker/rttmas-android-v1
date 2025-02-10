package com.mwnl.rttmas_android.services

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    fun requestAppPermissions(activity: AppCompatActivity) {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )

        val requestPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->

            if (!isGranted) {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.

                Toast.makeText(activity, "Please ensure Camera, Location and Notifications permissions are allowed.", Toast.LENGTH_SHORT).show()
            }
        }

        for (permission in REQUIRED_PERMISSIONS)
            requestPermissionLauncher.launch( permission )
    }


    /**
     *  Check whether all required app permissions have been granted.
     *
     *  @param  [Context] context - Pass the activity context here
     *  @return [Boolean] True if permissions are granted, false otherwise
     */
    fun checkIfAppPermissionsGranted(context: Context): Boolean {
        for (permission in REQUIRED_PERMISSIONS)
            if (ContextCompat.checkSelfPermission( context, permission ) != PackageManager.PERMISSION_GRANTED)
                return false

        return true
    }

}