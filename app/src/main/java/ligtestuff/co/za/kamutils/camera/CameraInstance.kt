package ligtestuff.co.za.kamutils.camera

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.util.Size
import android.view.Surface
import ligtestuff.co.za.kamutils.utils.logd

/*Holds a Camera instance & associated logic*/
internal class CameraInstance(
    cameraManager: CameraManager,
    camID: String,
    camPictureSize: Size,
    private val surface: Surface,
    cameraDisconnected: () -> Unit = {},
    cameraError: (Int) -> Unit = {},
    cameraOpened: (CameraDevice?) -> Unit = {}
) {
    /*Camera necessities*/
    private var camera: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val cameraCharacteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(camID)

    /*setup capture size best effort*/
    var captureSize: Size = pickBestSize(
        cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888),
        camPictureSize
    )

    init {
        try {//open the camera on init
            cameraManager.openCamera(camID, object : CameraDevice.StateCallback() {
                override fun onDisconnected(cam: CameraDevice?) {
                    logd("camera disconnected")
                    cameraDisconnected()
                }

                override fun onError(cam: CameraDevice?, error: Int) {
                    logd("camera error")
                    cameraError(error)
                }

                override fun onOpened(cam: CameraDevice?) {
                    logd("camera opened")
                    camera = cam!!
                    cameraOpened(cam)
                }

            }, null)
        } catch (ex: SecurityException) {
            throw ex
        }
    }

    private fun isCameraOpened() = camera != null

    fun createCaptureSession(configured: (Boolean) -> Unit) {
        if (isCameraOpened()) configured(false)

        camera!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession?) {
                logd("Capture Session failed")
                configured(false)
            }

            override fun onConfigured(session: CameraCaptureSession?) {
                logd("Capture Session configured")
                captureSession = session
                configured(true)
            }

        }, null)
    }

    fun startPreviewCapture(): Boolean {
        if (camera == null || captureSession == null) return false

        val repeatingRequest = camera!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        repeatingRequest.addTarget(surface)

        captureSession!!.setRepeatingRequest(repeatingRequest.build(), null, null)
        return true
    }

    fun cleanupCam() {
        captureSession?.close()
        captureSession = null
        camera?.close()
        camera = null
    }


    companion object {
        fun pickBestSize(sizes: Array<Size>, target: Size): Size {
            fun differenceFromRequested(s: Size) = Math.abs(s.width - target.width) + Math.abs(s.height - target.height)
            var bestSize = sizes[0]
            var bestDiff = differenceFromRequested(sizes[0])
            for (i in 1 until sizes.size) {
                val diff = differenceFromRequested(sizes[i])
                if (diff < bestDiff) {
                    bestSize = sizes[i]
                    bestDiff = diff
                }
            }
            return bestSize
        }
    }

}