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
import ligtestuff.co.za.kamutils.utils.logd
import wrightstuff.co.za.cameramanager.renderscripttesting.ScriptC_generalRS
import java.lang.Boolean.TRUE


class CameraHelper(
    private val ctx: Context,
    private val fixRotation: Boolean = false,
    private val bitmapCallback: (pb: Bitmap) -> Unit
) {
    private val LOG_E = TRUE //logging can slow down the image processing so lets enable/disable it on demand
    private var camID: String = ""//camera Identifier string
    private var numCams: Int = 0 //0 until it can be determined
    private var camSelection: Int = 0 //0 for now as can be more than 1
    private var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB

    /*speed up variables*/
    //this vars may need to be updated on size changes or configuration changes
    private var pb: Bitmap? = null //bitmap is replaced by the allocation call therefore we can store it neatly
    private var rgbaType: Type? = null//again just a type
    private var out: Allocation? = null

    /*Camera Helper Specifics*/
    private var manager: CameraManager
    private var phoneCam: CameraInstance? = null


    /*Test Scripts*/
    private val general: ScriptC_generalRS
    private val blur: ScriptIntrinsicBlur
    private val convolve3x3: ScriptIntrinsicConvolve3x3


    /*RenderScript*/
    private var rs: RenderScript

    init {
        logd("initializing CameraHelper", LOG_E)
        manager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        numCams = manager.cameraIdList.size
        camID = manager.cameraIdList[camSelection]
        rs = RenderScript.create(ctx, RenderScript.ContextType.NORMAL)
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(rs))
        general = ScriptC_generalRS(rs)
        blur = ScriptIntrinsicBlur.create(rs, Element.RGBA_8888(rs))
        blur.setRadius(1F)
        convolve3x3 = ScriptIntrinsicConvolve3x3.create(rs, Element.RGBA_8888(rs))
        convolve3x3.setCoefficients(floatArrayOf(1F, 0F, -1F, 2F, 0F, -2F, 1F, 0F, -1F))
    }

    fun onResume() {
        logd("onResume called for CameraHelper", LOG_E)
        startCamera()
    }

    fun onPause() {
        logd("onPause called for CameraHelper", LOG_E)
        closeCamera()
        clearVariables()
    }

    private fun clearVariables() {
        pb?.recycle()
        pb = null
        out = null
        rgbaType = null
    }

    private fun startCamera() {
        initializeVariables() //incase things have changed
        logd("Start Camera", LOG_E)
        if (!AndroidUtils.checkCameraPermissions(ctx)) return //lets just make sure camera permissions are granted
        //chain calls to create camera, initialize Capture session and start preview capture
        phoneCam = CameraInstance(manager, camID, getTargetCameraSize(ctx)) {
            val surface = getSurfaceSequential(phoneCam!!.captureSize) //definitely not null here
            phoneCam?.createCaptureSession(surface) { success ->
                if (success) {
                    phoneCam?.startPreviewCapture(surface)
                }
            }
        }
    }

    private fun initializeVariables() {
        pb = null
        rgbaType = null
        out = null
    }

    /**
     * This is the sequential implementation of retrieving a surface before threads are introduced.
     * Threads could potentially lead to higher framerates if the amount of image previews coming from the camera is higher than what can
     * be displayed at present
     * */
    private fun getSurfaceSequential(size: Size): Surface {
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


    /**
     * need to take into account fixRotation here possibly
     * */
    private fun allocationToBitmap(alloc: Allocation): Bitmap? {
        val pb: Bitmap? = createOrReuseBitmap(alloc)
        val rgbaType = createOrReuseAllocToBitmapType(alloc)
        val out = createOrReuseAllocBitmapOut(rgbaType)
        yuvToRgbIntrinsic.setInput(alloc)
        yuvToRgbIntrinsic.forEach(out)
        // out.copyTo(pb)
        //test scripts here!
        //general.forEach_mono(out, createOrReuseMonoAlloc(out))
        blur.setInput(out)
        blur.forEach(createOrReuseBlurAlloc(out))
        convolve3x3.setInput(blurAlloc)
        convolve3x3.forEach(createOrReuse3x3Alloc(blurAlloc!!))
        threebythree?.copyTo(pb)
        return pb
    }

    private var threebythree: Allocation? = null
    private fun createOrReuse3x3Alloc(inAlloc: Allocation): Allocation? {
        if (threebythree == null) {
            threebythree = Allocation.createTyped(rs, inAlloc.type)
        }
        return threebythree!!
    }

    private var blurAlloc: Allocation? = null
    private fun createOrReuseBlurAlloc(inAlloc: Allocation): Allocation {
        if (blurAlloc == null) {
            blurAlloc = Allocation.createTyped(rs, inAlloc.type)
        }
        return blurAlloc!!
    }

    private var monoAlloc: Allocation? = null
    private fun createOrReuseMonoAlloc(inAlloc: Allocation): Allocation {
        if (monoAlloc == null) {
            monoAlloc = Allocation.createTyped(rs, inAlloc.type)
        }
        return monoAlloc!!
    }

    private fun createOrReuseAllocBitmapOut(rgbaType: Type): Allocation {
        if (out == null) {
            out = Allocation.createTyped(rs, rgbaType, Allocation.USAGE_SCRIPT)
        }
        return out!!
    }

    private fun createOrReuseAllocToBitmapType(alloc: Allocation): Type {
        if (rgbaType == null) {
            rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(alloc.type.x).setY(alloc.type.y).create()
        }
        return rgbaType!!
    }

    private fun createOrReuseBitmap(alloc: Allocation): Bitmap {
        if (pb == null) {
            pb = Bitmap.createBitmap(alloc.type.x, alloc.type.y, Bitmap.Config.ARGB_8888)
        }
        return pb!!
    }

    private fun closeCamera() {
        phoneCam?.cleanupCam()
    }

    companion object {
        fun getTargetCameraSize(context: Context): Size {
            val ds = getLandscapeDisplaySize(context)
            return Size(ds.width, ds.height)
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