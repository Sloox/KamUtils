package ligtestuff.co.za.kamutils.mainview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_kam_screen.*
import ligtestuff.co.za.kamutils.R
import ligtestuff.co.za.kamutils.utils.AndroidUtils

class KamScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kam_screen)
    }

    override fun onResume() {
        super.onResume()
        AndroidUtils.onResumeChecks(cameraView, this) {
            //camera permissions granted & all is ok
        }
    }
}
