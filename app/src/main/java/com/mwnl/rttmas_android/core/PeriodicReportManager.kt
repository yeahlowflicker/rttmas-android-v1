package com.mwnl.rttmas_android.core

import android.content.Context
import android.graphics.Bitmap
import com.mwnl.rttmas_android.models.OcrItem
import com.mwnl.rttmas_android.models.ReportPayload
import com.mwnl.rttmas_android.services.CameraService
import com.mwnl.rttmas_android.services.GpsInfoService
import com.mwnl.rttmas_android.services.ImageCaptureCallback
import com.mwnl.rttmas_android.services.MqttService
import java.util.LinkedList
import java.util.Queue

const val MQTT_TOPIC_PERIODIC_REPORT = "report"

class PeriodicReportManager(
    var ocrQueue: Queue<OcrItem>,
    var cameraService: CameraService,
    var mqttService: MqttService,
    var gpsInfoService: GpsInfoService,
    var licensePlateDetector: LicensePlateDetector,
) {


    /**
     * Make a single report and upload to server via MQTT.
     *
     * @param [Context] context - The activity context
     */
    fun makeReport(context: Context) {

        // Construct the callback for image capture
        // The onSuccess method contains the logic after the image
        // has been successfully captured
        val imageCaptureCallback = object: ImageCaptureCallback {
            override fun onSuccess(bitmap: Bitmap) {

                // Perform YOLO detection
                licensePlateDetector.detectAndRecognizeLicensePlates(bitmap)

                // Obtain GPS info
                val gpsInfo = gpsInfoService.getGpsLocationInfo(context) ?: return


                // Construct payload for MQTT upload
                val reportPayload = ReportPayload()
                reportPayload.applyGpsInfo(gpsInfo)


                // Upload the payload via MQTT
                mqttService.publishMessage(
                    MQTT_TOPIC_PERIODIC_REPORT,
                    reportPayload.jsonify().toString()
                )
            }

            override fun onFailure(exception: Exception) {
                TODO("Not yet implemented")
            }

        }

        // Image capture trigger
        cameraService.captureImage(imageCaptureCallback)
    }
}