package ligtestuff.co.za.kamutils.camera

import android.content.Context
import android.hardware.camera2.CameraManager
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.util.DisplayMetrics
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import ligtestuff.co.za.kamutils.renderscript.RSAllocator
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
        textView.setTextureCallbacks({ _, _, _ -> startCamera() })
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
        //chain calls to create camera, initialize Capture session and start preview capture
        phoneCam = CameraInstance(manager, camID, getTargetCameraSize(ctx)) {

            val surface = getSurface(phoneCam!!.captureSize) //definitely not null here
            phoneCam?.createCaptureSession (surface) { success ->
                if (success) {
                    phoneCam?.startPreviewCapture(surface)
                }
            }
        }
    }

    /*Getting it to work before i refactor*/
    private fun getSurface(size: Size): Surface {
        val rs: RenderScript = RenderScript.create(ctx, RenderScript.ContextType.NORMAL)
        val allocation: Allocation = RSAllocator.createRenderscriptAllocation(rs, size)
        allocation.setOnBufferAvailableListener { it ->
            logd("Received Camera Image")
           if (it != null && (it.usage and Allocation.USAGE_IO_INPUT != 0)) {
               it.ioReceive()
            }
        }

        return allocation.surface

        //return Surface(textView.surfaceTexture) //temporary surface
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