package com.mwnl.rttmas_android.services

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("RTTMAS", token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("RTTMAS", "From: " + remoteMessage.from)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("RTTMAS", "Message data payload: " + remoteMessage.data)

            val broadcastIntent = Intent()
            broadcastIntent.setAction("com.rttmas.FCM_DATA") // Custom action name
            broadcastIntent.putExtra("message", remoteMessage.data["message"])
            sendBroadcast(broadcastIntent) // Sends the broadcast with data
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Log.d(
                "RTTMAS", "Message Notification Body: " + remoteMessage.notification!!
                    .body
            )
        }
    }
}