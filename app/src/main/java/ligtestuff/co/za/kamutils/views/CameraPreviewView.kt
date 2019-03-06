package ligtestuff.co.za.kamutils.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View

class CameraPreviewView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var processedCameraBitmap: Bitmap? = null
    private val blackPaint = Paint()
    private var oldTime: Long = 0L

    init {
        blackPaint.setARGB(255, 0, 0, 0)
    }

    override fun onDraw(canvas: Canvas?) {
        if (processedCameraBitmap != null && !processedCameraBitmap!!.isRecycled && canvas != null) {
            canvas.save()
            canvas.rotate(90F)
            canvas.drawBitmap(processedCameraBitmap, null, canvas.clipBounds, blackPaint)
            canvas.restore()
            drawText(canvas, "" + 1000 / (SystemClock.elapsedRealtime() - oldTime))
            oldTime = SystemClock.elapsedRealtime()
        } else {
            canvas?.drawPaint(blackPaint)
        }
    }

    private fun drawText(canvas: Canvas, text: String) {
        val paint = Paint()
        paint.color = Color.RED
        paint.textSize = 26f
        canvas.drawText(text, 10f, 25f, paint)
    }

}