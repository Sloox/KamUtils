package ligtestuff.co.za.kamutils.camera

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.DisplayMetrics
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import ligtestuff.co.za.kamutils.utils.AndroidUtils
import ligtestuff.co.za.kamutils.utils.logd
import ligtestuff.co.za.kamutils.views.AutoFitTextureView

class CameraHelper(private val ctx: Context, private val textView: AutoFitTextureView) {
    private var camID: String = ""//camera Identifier string
    private var numCams: Int = 0 //0 until it can be determined
    private var camSelection: Int = 0 //0 for now as can be more than 1

    /*Camera Helper Specifics*/
    private var manager: CameraManager
    private var phoneCam: CameraInstance? = null

    init {
        logd("initializing CameraHelper")
        manager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        numCams = manager.cameraIdList.size
        camID = manager.cameraIdList[camSelection]
        textView.setTextureCallbacks({ tView, w, h -> startCamera() })
    }

    fun onResume() {
        logd("onResume called for CameraHelper")
        if (textView.isAvailable) {
            startCamera()
        }
    }

    fun onPause() {
        logd("onPause called for CameraHelper")
        closeCamera()
    }


    private fun startCamera() {
        logd("Start Camera")
        if (!AndroidUtils.checkCameraPermissions(ctx)) return //lets just make sure camera permissions are granted
        val surface = Surface(textView.surfaceTexture) //temporary surface

        //chain calls to create camera, initialize Capture session and start preview capture
        phoneCam = CameraInstance(manager, camID, getTargetCameraSize(ctx), surface) {
            phoneCam?.createCaptureSession { success ->
                if (success) {
                    phoneCam?.startPreviewCapture()
                }
            }
        }
    }

    private fun closeCamera() {
        phoneCam?.cleanupCam()
    }

    companion object {
        fun getTargetCameraSize(context: Context): Size {
            val ds = getLandscapeDisplaySize(context)
            return Size(ds.width / 2, ds.height / 2)
        }

        private fun getLandscapeDisplaySize(context: Context): Size {
            val metrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(metrics)
            val ds = Size(metrics.widthPixels, metrics.heightPixels)
            return if (ds.width >= ds.height) ds else Size(ds.height, ds.width)
        }
    }


}