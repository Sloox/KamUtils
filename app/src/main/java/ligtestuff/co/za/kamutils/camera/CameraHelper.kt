package ligtestuff.co.za.kamutils.camera

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.renderscript.*
import android.util.DisplayMetrics
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import ligtestuff.co.za.kamutils.renderscript.RSAllocator
import ligtestuff.co.za.kamutils.utils.AndroidUtils
import ligtestuff.co.za.kamutils.utils.WorkLogger
import ligtestuff.co.za.kamutils.utils.logd
import java.lang.Boolean.TRUE


class CameraHelper(private val ctx: Context, private val bitmapCallback: (pb: Bitmap) -> Unit) {
    private val LOG_E = TRUE //logging can slow down the image processing so lets enable/disable it on demand
    private var camID: String = ""//camera Identifier string
    private var numCams: Int = 0 //0 until it can be determined
    private var camSelection: Int = 0 //0 for now as can be more than 1
    private lateinit var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB

    /*Camera Helper Specifics*/
    private var manager: CameraManager
    private var phoneCam: CameraInstance? = null

    private var profileLogger = WorkLogger(CameraHelper::javaClass.name, "Test", false)

    /*RenderScript*/
    private lateinit var rs: RenderScript

    init {
        logd("initializing CameraHelper", LOG_E)
        manager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        numCams = manager.cameraIdList.size
        camID = manager.cameraIdList[camSelection]
        rs = RenderScript.create(ctx, RenderScript.ContextType.NORMAL)
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(rs))
    }

    fun onResume() {
        logd("onResume called for CameraHelper", LOG_E)
        startCamera()
    }

    fun onPause() {
        logd("onPause called for CameraHelper", LOG_E)
        closeCamera()
    }


    private fun startCamera() {
        initializeVariables() //incase things have changed
        logd("Start Camera", LOG_E)
        if (!AndroidUtils.checkCameraPermissions(ctx)) return //lets just make sure camera permissions are granted
        //chain calls to create camera, initialize Capture session and start preview capture
        phoneCam = CameraInstance(manager, camID, getTargetCameraSize(ctx)) {
            val surface = getSurface(phoneCam!!.captureSize) //definitely not null here
            phoneCam?.createCaptureSession(surface) { success ->
                if (success) {
                    phoneCam?.startPreviewCapture(surface)
                }
            }
        }
    }

    private fun initializeVariables() {
        pb = null
    }

    /*Getting it to work before i refactor*/
    private fun getSurface(size: Size): Surface {
        //return Surface(textView.surfaceTexture) // draw directly to the textureView surface
        val allocation: Allocation = RSAllocator.createRenderscriptAllocation(rs, size)
        allocation.setOnBufferAvailableListener {
            // logd("Received Camera Image", LOG_E) //slows down the FPS
            if (it != null && (it.usage and Allocation.USAGE_IO_INPUT != 0)) {
                it.ioReceive()
                bitmapCallback(allocationToBitmap(it)!!)
            }
        }
        return allocation.surface
    }

    private fun allocationToBitmap(alloc: Allocation): Bitmap? {
        profileLogger.addSplit("allocationToBitmap - start")
        var pb: Bitmap? = createOrReuseBitmap(alloc)
        profileLogger.addSplit("bitmap create")
        val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(alloc.type.x).setY(alloc.type.y)
        val out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        profileLogger.addSplit("createTyped + rgbaType")
        yuvToRgbIntrinsic.setInput(alloc)
        yuvToRgbIntrinsic.forEach(out)
        profileLogger.addSplit("forEach")
        out.copyTo(pb)
        profileLogger.addSplit("copyTo")
        profileLogger.dumpToLog()
        profileLogger.reset()
        return pb
    }

    private var pb: Bitmap? = null
    private fun createOrReuseBitmap(alloc: Allocation): Bitmap? {
        if (pb == null) {
            pb = Bitmap.createBitmap(alloc.type.x, alloc.type.y, Bitmap.Config.ARGB_8888)
        }
        return pb
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