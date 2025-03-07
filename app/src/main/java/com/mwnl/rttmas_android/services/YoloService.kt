package com.mwnl.rttmas_android.services

import android.content.res.AssetManager
import android.graphics.Bitmap


/**
 * This class is directly related to "/app/cpp/native-lib.cpp".
 * The function and parameter names must match those defined in that file.
 */
class YoloService {

    external fun loadLicensePlateModel(mgr: AssetManager?, modelid: Int, cpugpu: Int): Boolean
    external fun loadParkingSlotModel(mgr: AssetManager?, modelid: Int, cpugpu: Int): Boolean

    class Obj {
        var x: Float = 0f
        var y: Float = 0f
        var w: Float = 0f
        var h: Float = 0f
        var label: Int = 0
        var prob: Float = 0f
        var modelIndex: Int = 0     // Which model was used to detect this object
    }

    external fun detect(bitmap: Bitmap?, detectParking: Boolean): Array<Obj?>?

    companion object {
        init {
            System.loadLibrary("android_ncnn_yolo11")
        }
    }
}