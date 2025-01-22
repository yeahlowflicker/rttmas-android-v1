package com.mwnl.rttmas_android.core

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.util.Log
import com.mwnl.rttmas_android.models.OcrItem
import com.mwnl.rttmas_android.services.CameraService
import com.mwnl.rttmas_android.services.FcmService
import com.mwnl.rttmas_android.services.GpsInfoService
import com.mwnl.rttmas_android.services.MqttService
import com.mwnl.rttmas_android.services.OcrService
import com.mwnl.rttmas_android.services.PermissionService
import com.mwnl.rttmas_android.services.YoloService
import java.util.LinkedList
import java.util.Queue

// How often should a report be made, time interval in ms
const val PERIODIC_REPORT_INTERVAL_MILLISECS = 1000L


/**
 *  Main wrapper for RTTMAS.
 *  This contains all the services and variables needed for RTTMAS.
 */
class RTTMAS(
    var context:    Context,
    var activity: Activity,
    var gui:        GUI,
) {

    // The device's unique ID
    lateinit var deviceID : String

    // Flag: Is detection enabled?
    var isDetectionEnabled = false


    // The OCR queue
    private val ocrQueue : Queue<OcrItem> = LinkedList()

    // Core services
    var cameraService       = CameraService()
    var fcmService          = FcmService()
    var gpsInfoService      = GpsInfoService()
    var mqttService         = MqttService()
    var ocrService          = OcrService()
    var permissionService   = PermissionService()
    var yoloService         = YoloService()


    // License plate detector
    var licensePlateDetector = LicensePlateDetector(
        ocrQueue, yoloService, ocrService
    )

    // Periodic report manager
    var periodicReportManager = PeriodicReportManager(
        activity, gui,
        ocrQueue, cameraService, mqttService,
        gpsInfoService, licensePlateDetector
    )


    fun initialize() {
        bindGUI()

        deviceID = permissionService.getAndroidDeviceID(context)

        mqttService.connectToMqttServer(deviceID)

        val ret_init: Boolean = yoloService.loadModel(context.assets, 0, 1)
        if (!ret_init) {
            Log.e("MainActivity", "mobilenetssdncnn Init failed")
        }

        cameraService.startCamera(context, activity)

        startOcrThread()

        startPeriodicReportSequence()

        isDetectionEnabled = true
        gui.switchDetectionStatus.isChecked = isDetectionEnabled
        gui.textDetectionStatus.text = "Detection ON"
    }

    private fun bindGUI() {
        gui.switchDetectionStatus.setOnCheckedChangeListener { _, isChecked ->
            isDetectionEnabled = isChecked
            gui.textDetectionStatus.text = if (isDetectionEnabled) "Detection ON" else "Detection OFF"

            if (!isDetectionEnabled) {
                gui.imageViewDetection.setImageBitmap(null)
            }
        }
    }

    private fun startOcrThread() {
        Thread {
            while (true) {
                if (ocrQueue.size == 0)
                    continue

                val ocrItem = ocrQueue.remove()
                licensePlateDetector.processOcrItem(ocrItem)
            }
        }.start()
    }

    // Entrypoint for periodic report sequence
    private fun startPeriodicReportSequence() {

        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {

                // Don't do anything, if detection is disabled
                if (!isDetectionEnabled)
                    return

                periodicReportManager.makeReport(context)

                handler.postDelayed(this, PERIODIC_REPORT_INTERVAL_MILLISECS)

            }
        }, PERIODIC_REPORT_INTERVAL_MILLISECS)
    }
}