package ligtestuff.co.za.kamutils.utils

import android.os.SystemClock
import android.util.Log
import java.util.*

/**
 * A utility class to help log timings splits throughout a method call.
 * A clone of TimingLogger but with custom disabled flag settable
 *
 */
internal class WorkLogger(tag: String, label: String, disabled: Boolean) {

    /**
     * Stores the time of each split.
     */
    private var mSplits: ArrayList<Long>? = null
    /**
     * Stores the labels for each split.
     */
    private lateinit var mSplitLabels: ArrayList<String>
    /**
     * The Log tag to use for checking Log.isLoggable and for
     * logging the timings.
     */
    private var mTag: String? = null
    /**
     * A label to be included in every log.
     */
    private var mLabel: String? = null
    /**
     * Used to track whether Log.isLoggable was enabled at reset time.
     */
    private var mDisabled: Boolean = false

    init {
        reset(tag, label, disabled)
    }


    private fun reset(tag: String, label: String, disabled: Boolean) {
        mTag = tag
        mLabel = label
        mDisabled = disabled
        reset()
    }


    private fun reset() {
        if (mDisabled) return
        if (mSplits == null) {
            mSplits = ArrayList()
            mSplitLabels = ArrayList()
        } else {
            mSplits!!.clear()
            mSplitLabels.clear()
        }
        addSplit(null)
    }


    private fun addSplit(splitLabel: String?) {
        if (mDisabled) return
        val now = SystemClock.elapsedRealtime()
        mSplits!!.add(now)
        mSplitLabels.add(splitLabel ?: "Null String")
    }


    fun dumpToLog() {
        if (mDisabled) return
        Log.d(mTag, "$mLabel: begin")
        val first = mSplits!![0]
        var now = first
        for (i in 1 until mSplits!!.size) {
            now = mSplits!![i]
            val splitLabel = mSplitLabels[i]
            val prev = mSplits!![i - 1]

            Log.d(mTag, "$mLabel:  ${now - prev} ms, $splitLabel")
        }
        Log.d(mTag, "$mLabel: end, ${now - first} ms")
    }
}