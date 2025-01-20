package com.mwnl.rttmas_android.services

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

class PermissionService {

    @SuppressLint("HardwareIds")
    /**
     *  Obtain the Android device ID.
     *  This ID is unique for each device.
     *
     *  @param  [Context] context    - pass the activity context here
     *  @return [string] the obtained device ID
     */
    fun getAndroidDeviceID(context: Context) : String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

}