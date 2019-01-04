package ligtestuff.co.za.kamutils.renderscript

import android.graphics.ImageFormat
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size
import com.dozingcatsoftware.util.ScriptC_flatten_yuv

/*Creates & Handles Allocations*/
object RSAllocator {

    fun createRenderscriptAllocation(rs: RenderScript, size: Size): Allocation {
        val yuvTypeBuilder = Type.Builder(rs, Element.YUV(rs))
        yuvTypeBuilder.setX(size.width)
        yuvTypeBuilder.setY(size.height)
        yuvTypeBuilder.setYuvFormat(ImageFormat.YUV_420_888)
        return Allocation.createTyped(rs, yuvTypeBuilder.create(), Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT)
    }

    fun reuseOrCreate2dAllocation(
        alloc: Allocation,
        rs: RenderScript,
        elementFn: (RenderScript) -> Element,
        x: Int,
        y: Int,
        usage: Int = Allocation.USAGE_SCRIPT
    ): Allocation {
        if (alloc != null && (alloc.type.x) == x && alloc.type.y == y) {
            return alloc
        }
        val typeBuilder = Type.Builder(rs, elementFn(rs)).setX(x).setY(y)
        return Allocation.createTyped(rs, typeBuilder.create(), usage)
    }

    fun create2dAllocation(
        rs: RenderScript,
        elementFn: (RenderScript) -> Element,
        x: Int,
        y: Int,
        usage: Int = Allocation.USAGE_SCRIPT
    ): Allocation {
        val typeBuilder = Type.Builder(rs, elementFn(rs)).setX(x).setY(y)
        return Allocation.createTyped(rs, typeBuilder.create(), usage)
    }

    fun flattenedYuvImageBytes(rs: RenderScript, yuvAlloc: Allocation): ByteArray {
        // There's no way to directly read the U and V bytes from a YUV allocation(?), but we can
        // use a .rs script to extract the three planes into output allocations and combine them.
        val width = yuvAlloc.type.x
        val height = yuvAlloc.type.y
        val yAlloc = create2dAllocation(rs, Element::U8, width, height)
        val uvWidth = Math.ceil(width / 2.0).toInt()
        val uvHeight = Math.ceil(height / 2.0).toInt()
        val uAlloc = create2dAllocation(rs, Element::U8, uvWidth, uvHeight)
        val vAlloc = create2dAllocation(rs, Element::U8, uvWidth, uvHeight)

        val script = ScriptC_flatten_yuv(rs)
        script._yuvInputAlloc = yuvAlloc
        script._uOutputAlloc = uAlloc
        script._vOutputAlloc = vAlloc
        script.forEach_flattenYuv(yAlloc)

        val ySize = width * height
        val uvSize = uvWidth * uvHeight
        val outputBytes = ByteArray(ySize + 2 * uvSize)
        val outBuffer = ByteArray(ySize)
        yAlloc.copyTo(outBuffer)
        System.arraycopy(outBuffer, 0, outputBytes, 0, ySize)
        uAlloc.copyTo(outBuffer)
        System.arraycopy(outBuffer, 0, outputBytes, ySize, uvSize)
        vAlloc.copyTo(outBuffer)
        System.arraycopy(outBuffer, 0, outputBytes, ySize + uvSize, uvSize)
        return outputBytes
    }

}