package com.mwnl.rttmas_android.models

import android.util.Log
import org.json.JSONObject
import java.util.LinkedList
import java.util.Queue

class ReportFrame {

    // Time
    var reportTimestamp: Int = -1

    // GPS info
    var latitude:   Float   = -1.0f
    var longitude:  Float   = -1.0f
    var speedMs:    Float   = 0f
    var heading:    Float   = 0f

    // License plates
    var detectedLicensePlates: ArrayList<String> = arrayListOf()

    // Parking slot detection status
    var availableParkingSlotFound: Boolean = false

    // The OCR queue
    val ocrQueue : Queue<OcrItem> = LinkedList()


    /**
     * Apply the data of a GpsInfo object directly into this payload.
     *
     * @param [GpsInfo] gpsInfo - The source object
     */
    fun applyGpsInfo(gpsInfo: GpsInfo) {
        this.latitude   = gpsInfo.latitude.toFloat()
        this.longitude  = gpsInfo.longitude.toFloat()
        this.speedMs    = gpsInfo.speedMs
        this.heading    = gpsInfo.heading
    }


    /**
     * Convert this object to JSON object.
     *
     * @return [JSONObject] The jsonified result
     */
    fun jsonify(deviceID: String) : JSONObject {
        val json = JSONObject()

        json.put("report_time",         this.reportTimestamp)
        json.put("reporter_uid",        deviceID)
        json.put("latitude",            this.latitude)
        json.put("longitude",           this.longitude)
        json.put("speed_ms",            this.speedMs)
        json.put("heading",             this.heading)

        json.put("plates", this.detectedLicensePlates.joinToString(","))

        json.put("parking_available",   this.availableParkingSlotFound)

        return json
    }
}