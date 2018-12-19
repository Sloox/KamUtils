package ligtestuff.co.za.kamutils.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.view.View
import ligtestuff.co.za.kamutils.R

object AndroidUtils {
    private const val REQUEST_CAMERA = 0

    fun onResumeChecks(view: View, activity: Activity, cameraCallback: () -> Unit) {
        setSystemUILowProfile(view)
        checkRequestCameraPermissions(activity, view, cameraCallback)
    }

    fun setSystemUILowProfile(view: View) {
        view.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
    }

    fun checkRequestCameraPermissions(activity: Activity, view: View, cameraCallback: () -> Unit) {
        if (checkCameraPermissions(activity)) {
            cameraCallback()
            return
        }
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

        if (shouldProvideRationale) {
            Snackbar.make(view, R.string.permissions_rationale_camera, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.action_grant) { requestCameraPermission(activity) }
                .show()
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        }
    }

    fun makeSnack(text: String, view: View) {
        Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.action_ok) { }
            .show()
    }

    fun checkCameraPermissions(activity: Activity): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
    }


}