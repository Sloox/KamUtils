package ligtestuff.co.za.kamutils.views

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.TextureView

class AutoFitTextureView : TextureView {

    private var mRatioWidth = 0
    private var mRatioHeight = 0


    /*default constructors*/
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context, attr: AttributeSet?, defStyle: Int) : super(context, attr, defStyle)


    /*Aspect ratio*/
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative")
        }
        mRatioWidth = width
        mRatioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth)
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height)
            }
        }
    }


    /*Surface Texture Listner */
    fun setTextureCallbacks(
        textureAvailable: (SurfaceTexture?, Int, Int) -> Unit,
        textureSizeChanged: (SurfaceTexture?, Int, Int) -> Unit = { text, w, h -> /*Do nothing*/ },
        textureUpdated: (SurfaceTexture?) -> Unit = { text -> /*Do nothing*/ }
    ) {
        this.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
                textureSizeChanged(surface, width, height)
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                textureUpdated(surface)
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                textureAvailable(surface, width, height)
            }
        }

    }

}