package com.mwnl.rttmas_android.core

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mwnl.rttmas_android.models.OcrItem
import com.mwnl.rttmas_android.services.OcrCallback
import com.mwnl.rttmas_android.services.OcrService
import com.mwnl.rttmas_android.services.YoloService
import java.util.LinkedList
import java.util.Locale
import java.util.Queue


private val MAX_OCR_QUEUE_CAPACITY = 5
private val MIN_VALID_LICENSE_PLATE_WIDTH = 200


class LicensePlateDetector(
    var ocrQueue : Queue<OcrItem>,
    var yoloService: YoloService,
    var ocrService: OcrService
) {


    /**
     * [This is called after a raw image has been captured]
     * Perform YOLO detection and then OCR to capture all license plate
     * numbers appearing in the captured image.
     *
     * @param [Bitmap] rawImageBitmap - The raw captured image
     */
    fun detectAndRecognizeLicensePlates(rawImageBitmap: Bitmap) : Array<YoloService.Obj?> {
        val objects: Array<YoloService.Obj?> = this.yoloService.detect(rawImageBitmap) ?: return arrayOf()

        // Repeat for each detected object (i.e. bounding box)
        for (i in objects.indices) {

            // Get the YOLO-formatted detected object
            val obj = objects[i]!!

            // Crop the license plate from the original image
            // to form a license plate bitmap
            val bmp = Bitmap.createBitmap(
                rawImageBitmap,
                obj.x.toInt(), obj.y.toInt(),
                obj.w.toInt(), obj.h.toInt()
            )

            // Create a new OCR item and add it to the queue for OCR processing
            // The item will be dropped if the OCR queue is full
            if (isValidLicensePlateBitmap(bmp) && ocrQueue.size <= MAX_OCR_QUEUE_CAPACITY) {
                val newOcrItem = createOcrItem(bmp)
                ocrQueue.add(newOcrItem)
            }
        }

        return objects
    }


    /**
     * [This will be called upon a new image capture]
     * Create a OcrItem for a cropped license plate bitmap.
     * The returned object will then be queued for OCR processing.
     *
     * @param [Bitmap] croppedBmp - The cropped license plate bitmap
     * @return [OcrItem] The constructed OCR item
     */
    private fun createOcrItem(croppedBmp: Bitmap) : OcrItem {

        // Normalize the aspect ratio of the cropped license plate bitmap
        // to improve the OCR accuracy
        val bmp = Bitmap.createScaledBitmap(
            croppedBmp,
            croppedBmp.height/14*26,
            croppedBmp.height,
            true
        )

        // Create and return the OcrItem
        val ocrItem = OcrItem(bmp)
        return ocrItem
    }


    /**
     * [This will be called when working on the OCR queue, not upon image capture]
     * Process a single OCR item.
     *
     * @param [OcrItem] ocrItem - The targeted item to process
     */
    fun processOcrItem(ocrItem: OcrItem) {
        ocrItem.ocrStartTime = System.currentTimeMillis()

        val callback = object:OcrCallback {
            override fun onSuccess(recognizedText: String) {

                var ocrResult = recognizedText

                ocrResult = LicensePlatePostprocessor.postprocessLicensePlateText(ocrResult)

                if (ocrResult.isBlank())
                    return

                ocrItem.ocrResult = ocrResult
                ocrItem.ocrEndTime = System.currentTimeMillis()

                ocrItem.isProcessed = true

                Log.d("OCR", ocrResult)
            }

            override fun onFailure(exception: Exception) {
                TODO("Not yet implemented")
            }
        }

        this.ocrService.performMLKitOCR(ocrItem.bitmap, callback)
    }


    /**
     * A utility function which checks if a cropped license plate bitmap is valid.
     *
     * Rules:
     * - Width must be greater than height
     * - Must be wide enough so the content is clear enough for OCR
     *
     * @param [Bitmap] bmp - The cropped license plate bitmap
     * @return [Boolean] Whether the bitmap is valid
     */
    private fun isValidLicensePlateBitmap(bmp: Bitmap) : Boolean {
        if (bmp.width < bmp.height)
            return false

        if (bmp.width < MIN_VALID_LICENSE_PLATE_WIDTH)
            return false

        return true
    }
}