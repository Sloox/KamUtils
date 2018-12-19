package ligtestuff.co.za.kamutils.utils

import android.app.Activity
import android.util.Log
import android.widget.Toast
import ligtestuff.co.za.kamutils.BuildConfig

fun Any.logd(txt: String, t: String = this::class.java.simpleName) { //internal debug logging
    if (BuildConfig.DEBUG) {
        Log.d(t, txt)
    }
}

fun Any.loge(txt: String, t: String = this::class.java.simpleName) { //internal debug logging
    if (BuildConfig.DEBUG) {
        Log.e(t, txt)
    }
}

fun Activity.toast(txt: String) {
    Toast.makeText(this, txt, Toast.LENGTH_SHORT).show()
}