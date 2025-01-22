package com.mwnl.rttmas_android.core

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.mwnl.rttmas_android.R
import com.mwnl.rttmas_android.models.OcrItem
import com.mwnl.rttmas_android.models.ReportPayload
import com.mwnl.rttmas_android.services.CameraService
import com.mwnl.rttmas_android.services.GpsInfoService
import com.mwnl.rttmas_android.services.ImageCaptureCallback
import com.mwnl.rttmas_android.services.MqttService
import com.mwnl.rttmas_android.services.YoloService.Obj
import java.util.Queue

const val MQTT_TOPIC_PERIODIC_REPORT = "report"

class PeriodicReportManager(
    var activity:               Activity,
    var gui:                    GUI,
    var ocrQueue:               Queue<OcrItem>,
    var cameraService:          CameraService,
    var mqttService:            MqttService,
    var gpsInfoService:         GpsInfoService,
    var licensePlateDetector:   LicensePlateDetector,
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
                val detectedObjects = licensePlateDetector.detectAndRecognizeLicensePlates(bitmap)

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


                // Render the captured and annotated bitmap
                activity.runOnUiThread {
                    val annotatedBitmap = annotateBoundingBoxes(context, bitmap, detectedObjects)
                    gui.imageViewDetection.setImageBitmap(annotatedBitmap)

                    val speedKmh = gpsInfo.speedMs * 3.6f

                    gui.textGpsLatitude.text = gpsInfo.latitude.toString()
                    gui.textGpsLongitude.text = gpsInfo.longitude.toString()
                    gui.textSpeed.text = speedKmh.toString() + " km/h"
                }
            }

            override fun onFailure(exception: Exception) {
                TODO("Not yet implemented")
            }

        }

        // Image capture trigger
        cameraService.captureImage(imageCaptureCallback)
    }



    private fun annotateBoundingBoxes(
        context: Context, bitmap: Bitmap, objects: Array<Obj?>
    ) : Bitmap {
        // draw objects on bitmap
        val rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(rgba)

        val colors = intArrayOf(
            Color.GREEN,        // available
            Color.BLUE,         // bus
            Color.DKGRAY,       // negative
            Color.CYAN,         // occupied
            Color.RED,          // red-line
            Color.YELLOW,       // yellow-line
        )


        val textpaint = Paint()
        textpaint.color = Color.WHITE
        textpaint.textSize = 100f
        textpaint.textAlign = Paint.Align.LEFT


        for (i in objects.indices) {

            val obj = objects[i]!!

            val paint = Paint()
            paint.color = colors[objects[i]!!.label]
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f

            val textbgpaint = Paint()
            textbgpaint.color = colors[objects[i]!!.label]
            textbgpaint.style = Paint.Style.FILL

            canvas.drawRect(
                obj.x,
                obj.y,
                obj.x + obj.w,
                obj.y + obj.h,
                paint
            )
            // draw filled text inside image
            val labels = context.resources.getStringArray(R.array.license_plate_labels)


            val text = "${labels[objects[i]!!.label]} = " + String.format(
                "%.1f",
                objects[i]!!.prob * 100
            ) + "%"
            val text_width = textpaint.measureText(text)
            val text_height = -textpaint.ascent() + textpaint.descent()

            var x = objects[i]!!.x
            var y = objects[i]!!.y - text_height
            if (y < 0) y = 0f
            if (x + text_width > rgba.width) x = rgba.width - text_width
            if (x < 0) x = 0f
            canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint)
            canvas.drawText(text, x, y - textpaint.ascent(), textpaint)
        }

        return rgba
    }
}