package com.mwnl.rttmas_android.services

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class OcrService {
    private val mlkitTextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


    /**
     * A utility function to perform ML Kit OCR on a bitmap.
     *
     * @param [Bitmap] bmp - The input bitmap
     * @param [OcrCallback] callback - The callback function after OCR completes
     */
    fun performMLKitOCR(bmp: Bitmap, callback: OcrCallback) {

        // Convert the input bitmap to ML Kit's InputImage format
        val image = bmp.let {
            InputImage.fromBitmap(it, 0)
        }

        // The actual OCR process
        image.let {
            mlkitTextRecognizer.process(it).addOnSuccessListener { scannedText ->
                callback.onSuccess(scannedText.text)
            }.addOnFailureListener { exception ->
                callback.onFailure(exception)
            }
        }
    }
}


/**
 * A callback interface for handling the async OCR events.
 */
interface OcrCallback {
    fun onSuccess(recognizedText: String)
    fun onFailure(exception: Exception)
}
