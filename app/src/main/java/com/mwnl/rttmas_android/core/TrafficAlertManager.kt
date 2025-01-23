package com.mwnl.rttmas_android.core

import android.util.Log
import com.mwnl.rttmas_android.models.TrafficAlert
import com.mwnl.rttmas_android.services.TAG
import org.json.JSONObject

class TrafficAlertManager(
    var rttmas: RTTMAS
) {
    var alerts : ArrayList<TrafficAlert> = arrayListOf()

    /**
     * [This will be called by FcmService upon message received]
     *
     * @param [JSONObject] payload - The received JSON alert payload
     */
    fun onTrafficAlertReceived(payload: JSONObject) {

        try {

            // Extract JSON fields
            val timestamp   = payload.getInt("timestamp")
            val title       = payload.getString("title")
            val description = payload.getString("description")
            val alertType   = payload.getInt("type")

            // Construct traffic alert object
            val trafficAlert = TrafficAlert(
                alertType, timestamp, title, description,
            )

            // Remove old alerts from list
            if (alerts.size >= 3)
                alerts.removeAt(alerts.size - 1)


            // Add new alert to list
            alerts.add(0, trafficAlert)


            // Update GUI view
            rttmas.adapterTrafficAlerts.notifyItemInserted(0)
            rttmas.adapterTrafficAlerts.notifyItemRemoved(alerts.size - 1)

        } catch (e: Exception) {
            e.message?.let { Log.e(TAG, it) }
            return
        }
    }
}