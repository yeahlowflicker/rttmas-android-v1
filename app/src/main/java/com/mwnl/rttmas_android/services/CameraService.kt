package com.mwnl.rttmas_android.services

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min


class CameraService {

    private var imageCapture: ImageCapture? = null

    /**
     * [This will be called upon detection start or app init]
     * Start the device camera stream
     *
     * @param [Context] context - The activity context
     */
    fun startCamera(
        context: Context,
        activity: Activity
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {

                val cameraProvider = cameraProviderFuture.get()

                // Obtain camera info and select the appropriate camera lens
                val availableCameraInfos = cameraProvider.availableCameraInfos
                val cameraSelector = availableCameraInfos[2].cameraSelector


                // Initialize the image capture object
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(activity.windowManager.defaultDisplay.rotation)
                    .build()

                //
                val camera = cameraProvider.bindToLifecycle(
                    (activity as LifecycleOwner),
                    cameraSelector,
                    imageCapture
                )

                // Set the zoom level of the camera
                val cameraControl = camera.cameraControl;
                cameraControl.setZoomRatio(2F);


            } catch (e: ExecutionException) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            } catch (_: InterruptedException) { }
        }, ContextCompat.getMainExecutor(context))
    }


    /**
     * Capture a single image.
     *
     * @param [Callback] callback - The callback logic after capturing an image
     */
    fun captureImage(callback: ImageCaptureCallback) {

        imageCapture!!.takePicture(
            Executors.newSingleThreadExecutor(),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    // Convert from ImageProxy to Bitmap
                    val bmp = image.toBitmap()

                    // A matrix used for rotating the bitmap
                    val matrix = Matrix()
                    matrix.postRotate(90f)

                    // The camera captures a tall, portrait image
                    // so it has to be center-cropped
                    val width = bmp.width
                    val height = max(bmp.height, 300)
                    val size = min(width.toDouble(), height.toDouble()).toInt()
                    val x = (width - size) / 2
                    val y = (height - size) / 2

                    // Crop the bitmap to a square
                    val bitmap = Bitmap.createBitmap(bmp, x, y, size, size, matrix, false)

                    // Bitmap obtained, trigger success callback
                    callback.onSuccess(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    callback.onFailure(exception)
                }
            }
        )
    }
}


/**
 * A callback interface for handling the async image capture events.
 */
interface ImageCaptureCallback {
    fun onSuccess(bitmap: Bitmap)
    fun onFailure(exception: Exception)
}

