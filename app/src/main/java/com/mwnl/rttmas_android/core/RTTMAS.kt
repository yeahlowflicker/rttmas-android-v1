package com.mwnl.rttmas_android.core

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.mwnl.rttmas_android.models.ReportFrame
import com.mwnl.rttmas_android.services.CameraService
import com.mwnl.rttmas_android.services.FcmService
import com.mwnl.rttmas_android.services.GpsInfoService
import com.mwnl.rttmas_android.services.MqttService
import com.mwnl.rttmas_android.services.OcrService
import com.mwnl.rttmas_android.services.PermissionService
import com.mwnl.rttmas_android.services.YoloService

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

    // A temporary report frame
    var currentReportFrame = ReportFrame()

    // Traffic alert manager
    var trafficAlertManager = TrafficAlertManager(this)

    // Recycler view adapter
    var adapterTrafficAlerts = TrafficAlertAdapter(activity, trafficAlertManager.alerts)

    // Core services
    var cameraService       = CameraService()
    var fcmService          = FcmService(activity, trafficAlertManager)
    var gpsInfoService      = GpsInfoService()
    var mqttService         = MqttService()
    var ocrService          = OcrService()
    var permissionService   = PermissionService()
    var yoloService         = YoloService()


    // License plate detector
    var licensePlateDetector = LicensePlateDetector(
        yoloService, ocrService
    )

    // Periodic report manager
    var periodicReportManager = PeriodicReportManager(
        this, activity, gui,
        cameraService, mqttService,
        gpsInfoService, licensePlateDetector
    )


    /**
     *  [This will be called by MainActivity]
     *  The entrypoint of the entire RTTMAS logic.
     */
    fun initialize() {
        // Setup GUI elements
        setupGUI()

        // Obtain Android device ID
        deviceID = permissionService.getAndroidDeviceID(context)

        // Setup MQTT
        mqttService.connectToMqttServer(deviceID)

        // Setup FCM
        fcmService.setupFcm()

        // Load YOLO11 model (license plate)
        val retInitPlateModel: Boolean = yoloService.loadLicensePlateModel(context.assets, 0, 1)
        if (!retInitPlateModel)
            Log.e("MainActivity", "mobilenetssdncnn Init failed for license plate model")

        // Load YOLO11 model (parking slot)
        val retInitParkingModel: Boolean = yoloService.loadParkingSlotModel(context.assets, 0, 1)
        if (!retInitParkingModel)
            Log.e("MainActivity", "mobilenetssdncnn Init failed for parking slot model")

        // Launch device camera
        cameraService.startCamera(context, activity)

        // Start the never-ending OCR processing thread
        startOcrThread()

        // Start the periodic MQTT report sequence
        startPeriodicReportSequence()

        // Enable detection on initialization complete
        isDetectionEnabled = true
        gui.switchDetectionStatus.isChecked = isDetectionEnabled
        gui.textDetectionStatus.text = "Detection ON"
    }


    /**
     * Setup and initialize the GUI views.
     */
    private fun setupGUI() {
        gui.switchDetectionStatus.setOnCheckedChangeListener { _, isChecked ->
            isDetectionEnabled = isChecked
            gui.textDetectionStatus.text = if (isDetectionEnabled) "Detection ON" else "Detection OFF"

            if (!isDetectionEnabled) {
                gui.imageViewDetection.setImageBitmap(null)
            }
        }

        // Setup recycler view
        adapterTrafficAlerts = TrafficAlertAdapter(activity, trafficAlertManager.alerts)
        gui.recyclerViewTrafficAlerts.layoutManager = LinearLayoutManager(context)
        gui.recyclerViewTrafficAlerts.adapter = adapterTrafficAlerts
    }


    /**
     * Create a never-ending thread to continuously perform OCR on queued items.
     * If the queue is empty, this thread is simply idle.
     */
    private fun startOcrThread() {
        Thread {
            while (true) {
                if (!isDetectionEnabled || currentReportFrame.ocrQueue.size == 0)
                    continue

                val ocrItem = currentReportFrame.ocrQueue.remove()
                licensePlateDetector.processOcrItem(currentReportFrame, ocrItem)
            }
        }.start()
    }


    /**
     *  Entrypoint for periodic report sequence.
     */
    private fun startPeriodicReportSequence() {
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {

                if (isDetectionEnabled) {
                    // Upload the previous report frame
                    periodicReportManager.uploadAndClearPreviousFrame(currentReportFrame)

                    // Then, initialize a new report frame if detection is enabled
                    periodicReportManager.initializeNewReportFrame(context)
                }

                handler.postDelayed(this, PERIODIC_REPORT_INTERVAL_MILLISECS)

            }
        }, PERIODIC_REPORT_INTERVAL_MILLISECS)
    }
}