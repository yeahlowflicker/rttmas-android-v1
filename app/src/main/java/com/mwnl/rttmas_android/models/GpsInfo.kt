package com.mwnl.rttmas_android.models

class GpsInfo(
    val time        : Long,         // UNIX, in milliseconds
    val latitude    : Double,
    val longitude   : Double,
    val speedMs     : Float,        // in m/s
    val heading     : Float,        // in degrees
)