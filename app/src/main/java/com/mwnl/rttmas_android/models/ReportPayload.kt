package com.mwnl.rttmas_android.models

import org.json.JSONObject

class ReportPayload {

    // Time
    var reportTimestamp: Int = -1

    // GPS info
    var latitude:   Float   = -1.0f
    var longitude:  Float   = -1.0f
    var speedMs:    Float   = 0f
    var heading:    Float   = 0f

    // License plates
    var detectedLicensePlates: ArrayList<String> = arrayListOf()


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
    fun jsonify() : JSONObject {
        val json = JSONObject()

        json.put("report_time",     this.reportTimestamp)
        json.put("latitude",        this.latitude)
        json.put("longitude",       this.longitude)
        json.put("speed_ms",        this.speedMs)
        json.put("heading",         this.heading)

        json.put("plates", this.detectedLicensePlates.joinToString(","))

        return json
    }
}