package com.mwnl.rttmas_android.core

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
const val PERIODIC_REPORT_INTERVAL_MILLISECS = 3000L

// How often should the app check the device's status, in ms
const val PERIODIC_CONNTECTABILITY_CHECK_INTERVAL_MILLISECS = 1000L

// How much available memory is required for parking model to activate
const val ENABLE_PARKING_MODEL_MEMORY_SIZE_THRESHOLD_MB = 1500L

/**
 *  Main wrapper for RTTMAS.
 *  This contains all the services and variables needed for RTTMAS.
 */
class RTTMAS(
    var context:    Context,
    var activity:   Activity,
    var gui:        GUI,
) {

    // The device's unique ID
    lateinit var deviceID : String

    // A status flag which tells the app's status
    // 0: Running normally
    // 1: Permissions not granted
    // 2: GPS disabled
    // 3: Disconnted from backend server
    var appConnectabilityStatus = 0

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

    // Should the app detect parking slots?
    // This is true only if there is enough memory
    var isDetectParking = getDeviceAvailableMemorySizeInMB() > ENABLE_PARKING_MODEL_MEMORY_SIZE_THRESHOLD_MB


    /**
     *  [This will be called by MainActivity]
     *  The entrypoint of the entire RTTMAS logic.
     */
    fun initialize() {
        Toast.makeText(context, "Available Memory: ${getDeviceAvailableMemorySizeInMB()} MB", Toast.LENGTH_SHORT).show()

        // Setup GUI elements
        setupGUI()

        // Ensure app permissions
        permissionService.requestAppPermissions(activity as AppCompatActivity)

        // Obtain Android device ID
        deviceID = permissionService.getAndroidDeviceID(context)

        // Setup MQTT
        mqttService.connectToMqttServer(deviceID)

        // Setup FCM
        fcmService.setupFcm()

        // Setup GPS service
        gpsInfoService.setupGpsStatusReceiver(activity)

        // Load YOLO11 model (license plate)
        val retInitPlateModel: Boolean = yoloService.loadLicensePlateModel(context.assets, 0, 1)
        if (!retInitPlateModel)
            Log.e("MainActivity", "mobilenetssdncnn Init failed for license plate model")

        // Load YOLO11 model (parking slot)
        if (isDetectParking) {
            val retInitParkingModel: Boolean = yoloService.loadParkingSlotModel(context.assets, 0, 1)
            if (!retInitParkingModel)
                Log.e("MainActivity", "mobilenetssdncnn Init failed for parking slot model")
        }

        // Launch device camera
        cameraService.startCamera(context, activity)

        // Start the periodic connectability assertion
        startAssertConnectabilitySequence()

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
     * Obtain the device's total memory, in Megabytes.
     */
    private fun getDeviceAvailableMemorySizeInMB() : Long {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val availableMemory = memInfo.availMem / (1024 * 1024)

        return availableMemory
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

                if (appConnectabilityStatus == 0 && isDetectionEnabled) {
                    cameraService.setCameraZoom(1F)

                    // Upload the previous report frame
                    periodicReportManager.uploadAndClearPreviousFrame(currentReportFrame)

                    // Then, initialize a new report frame if detection is enabled
                    periodicReportManager.initializeNewReportFrame(context, isDetectParking)
                }

                handler.postDelayed(this, PERIODIC_REPORT_INTERVAL_MILLISECS)

            }
        }, PERIODIC_REPORT_INTERVAL_MILLISECS)
    }


    private fun assertConnectability() {
        val isPermissionsGranted    = permissionService.checkIfAppPermissionsGranted(context)
        val isGpsAvailable          = gpsInfoService.isGpsEnabled
        val isMqttConnected         = mqttService.isMqttConnected

        activity.runOnUiThread {

            if (!isPermissionsGranted) {
//                permissionService.requestAppPermissions(activity as AppCompatActivity)
                appConnectabilityStatus = 1
            }
            else if (!isGpsAvailable) {
                appConnectabilityStatus = 2

                gui.textGpsLatitude.text = "0.000000"
                gui.textGpsLatitude.text = "0.000000"
                gui.textSpeed.text = "0.0 km/h"
            }
            else if (!isMqttConnected) {
                mqttService.disconnect()
                mqttService.connectToMqttServer(deviceID)
                appConnectabilityStatus = 3
            }
            else {
                appConnectabilityStatus = 0
            }

            gui.imageViewDetection.visibility           = if (appConnectabilityStatus == 0) View.VISIBLE else View.GONE
            gui.switchDetectionStatus.isEnabled         = appConnectabilityStatus == 0

            gui.warningPermissionNotGranted.visibility  = if (appConnectabilityStatus == 1) View.VISIBLE else View.GONE
            gui.warningGpsUnavailable.visibility        = if (appConnectabilityStatus == 2) View.VISIBLE else View.GONE
            gui.warningServerDisconnected.visibility    = if (appConnectabilityStatus == 3) View.VISIBLE else View.GONE
        }
    }


    private fun startAssertConnectabilitySequence() {
        val handler = Handler()

        handler.postDelayed(object: Runnable {
            override fun run() {
                assertConnectability()
                handler.postDelayed(this, PERIODIC_CONNTECTABILITY_CHECK_INTERVAL_MILLISECS)
            }
        }, PERIODIC_CONNTECTABILITY_CHECK_INTERVAL_MILLISECS)
    }
}