package com.mwnl.rttmas_android.models

import android.graphics.Bitmap

class OcrItem(
    var bitmap: Bitmap,
    var ocrResult: String = ""
) {
    var ocrStartTime: Long = -1
    var ocrEndTime: Long = -1

    fun getOcrElapsedTime() : Long {
        return ocrEndTime - ocrStartTime
    }
}