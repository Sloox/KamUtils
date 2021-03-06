package ligtestuff.co.za.kamutils.mainview

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_kam_screen.*
import ligtestuff.co.za.kamutils.R
import ligtestuff.co.za.kamutils.camera.CameraHelper
import ligtestuff.co.za.kamutils.utils.AndroidUtils

class KamScreen : AppCompatActivity() {
    private lateinit var cameraManager: CameraHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kam_screen)
        cameraManager = CameraHelper(this, bitmapCallback = this::handleBitmap)
    }

    override fun onResume() {
        super.onResume()
        AndroidUtils.onResumeChecks(cameraView, this) {
            //checks permissions & will open camera
            cameraManager.onResume()
        }
    }

    private fun handleBitmap(pb: Bitmap) {
        cameraView.processedCameraBitmap = pb
        cameraView.invalidate()
    }

    override fun onPause() {
        super.onPause()
        cameraManager.onPause()
    }
}
