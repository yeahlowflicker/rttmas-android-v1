package com.mwnl.rttmas_android.services

import android.util.Log
import com.mwnl.rttmas_android.BuildConfig
import com.mwnl.rttmas_android.core.MQTT_TOPIC_PERIODIC_REPORT
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLSocketFactory

const val TAG = "RTTMAS"
const val MQTT_SERVER_URI = BuildConfig.MQTT_SERVER_URI
const val MQTT_USERNAME = BuildConfig.MQTT_USERNAME
const val MQTT_PASSWORD = BuildConfig.MQTT_PASSWORD
const val MQTT_QOS = BuildConfig.MQTT_QOS

class MqttService {

    private lateinit var mqttClient: MqttAsyncClient

    var isMqttConnected = false


    fun connectToMqttServer(clientID: String) {
        try {
            mqttClient = MqttAsyncClient(MQTT_SERVER_URI, clientID, null)
            val options = MqttConnectOptions()

//            options.isAutomaticReconnect = true

            // Set options for MQTTS
            options.userName = MQTT_USERNAME
            options.password = MQTT_PASSWORD.toCharArray()
            options.isCleanSession = true
//            options.connectionTimeout = 10
//            options.keepAliveInterval = 20

            // Enable SSL/TLS (Optional)
            options.socketFactory = SSLSocketFactory.getDefault()

            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable) {
                    isMqttConnected = false
                    Log.d(TAG, "Connection lost: " + cause.message)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    if (topic == MQTT_TOPIC_PERIODIC_REPORT)
                        isMqttConnected = true

                    Log.d(
                        TAG,
                        "Message arrived: Topic: " + topic + ", Message: " + String(message.payload)
                    )
                }

                override fun deliveryComplete(token: IMqttDeliveryToken) {
                }
            })

            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "Connected to MQTTS broker")
                    subscribeToTopic("test")

                    isMqttConnected = true
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d(TAG, "Failed to connect to MQTTS broker: " + exception.message)
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribeToTopic(topic: String) {
        try {
            mqttClient.subscribe(topic, MQTT_QOS, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(
                        TAG,
                        "Subscribed to topic: $topic"
                    )
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d(TAG, "Failed to subscribe to topic: " + exception.message)
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publishMessage(topic: String?, payload: String) {
        try {
            val message = MqttMessage()
            message.payload = payload.toByteArray(StandardCharsets.UTF_8)
            mqttClient.publish(topic, message)
            Log.d(TAG, "Message published: $payload")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            if (mqttClient.isConnected) {
                mqttClient.disconnect()
                Log.d(TAG, "MQTT Disconnected")

                isMqttConnected = false
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}