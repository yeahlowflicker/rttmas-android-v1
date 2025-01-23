package com.mwnl.rttmas_android.services

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mwnl.rttmas_android.core.TrafficAlertManager
import org.json.JSONObject


const val FCM_TOPIC_TRAFFIC_ALERTS = "alerts"


class FcmService(
    var activity: Activity,
    var trafficAlertManager: TrafficAlertManager
) : FirebaseMessagingService() {

    private lateinit var receiver: BroadcastReceiver


    /**
     * Setup the entire FCM service and configure broadcast receivers.
     * This listens to Android's system events to capture FCM messages.
     */
    fun setupFcm() {
        // Setup broadcast receiver
        // This allows the MainActivity to capture the received alert data
        // from FCMService
        // Define the BroadcastReceiver to handle the data from Service
        receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    // Get the message from the intent
                    val message = intent.getStringExtra("message")

                    Log.d(TAG, "Broadcast receiver received.")

                    try {
                        checkNotNull(message)

                        val payload = JSONObject(message)
                        trafficAlertManager.onTrafficAlertReceived(payload)

                    } catch (e: Exception) {
                        e.message?.let { Log.d(TAG, it) }
                    }
                }
            }


        // Register the receiver with the custom action
        val filter = IntentFilter("com.rttmas.FCM_DATA")
        activity.registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)


        // Subscribe to FCM topic(s)
        subscribeToFcm()
    }


    /**
     * Subscribe to FCM topic.
     */
    private fun subscribeToFcm() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result

                Log.d(TAG, token!!)
            })

        FirebaseMessaging.getInstance().subscribeToTopic(FCM_TOPIC_TRAFFIC_ALERTS)
            .addOnCompleteListener { task ->
                var msg = "FCM subscribed"
                if (!task.isSuccessful)
                    msg = "FCM subscribe failed"
                Log.d(TAG, msg)
            }
    }



    override fun onNewToken(token: String) {
        Log.d(TAG, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.from)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

            val broadcastIntent = Intent()
            broadcastIntent.setAction("com.rttmas.FCM_DATA") // Custom action name
            broadcastIntent.putExtra("message", remoteMessage.data["message"])
            sendBroadcast(broadcastIntent) // Sends the broadcast with data
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Log.d(
                TAG, "Message Notification Body: " + remoteMessage.notification!!
                    .body
            )
        }
    }
}