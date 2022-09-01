package com.levibostian.wendyexample

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION_CODES.LOLLIPOP
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.widget.TextView
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.types.ReasonPendingTaskSkipped

class PendingStatusTextView : TextView, PendingTaskStatusListener {

    private lateinit var mContext: Context

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        initialize(context)
    }
    @TargetApi(LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes) {
        initialize(context)
    }

    private fun initialize(context: Context) {
        mContext = context
    }

    override fun skipped(taskId: Long, reason: ReasonPendingTaskSkipped) {
        text = reason.accept(object : ReasonPendingTaskSkipped.Visitor<String> {
            override fun visitUnresolvedRecordedError(): String {
                return "Skipped: previously recorded error exists."
            }
            override fun visitPartOfFailedGroup(): String {
                return "Skipped: part of failing group"
            }
            override fun visitNotReadyToRun(): String {
                return "Skipped: not ready to run"
            }
        })
    }
    override fun running(taskId: Long) {
        text = "Running"
        setTextColor(ContextCompat.getColor(mContext, android.R.color.holo_blue_dark))
    }

    override fun complete(taskId: Long, successful: Boolean) {
        text = if (successful) "Success!" else "Failed!"
        setTextColor(ContextCompat.getColor(mContext, if (successful) android.R.color.holo_green_dark else android.R.color.holo_orange_dark))
    }

    override fun errorRecorded(taskId: Long, errorMessage: String?, errorId: String?) {
        text = errorMessage
        setTextColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark))
    }

    override fun errorResolved(taskId: Long) {
        text = "Error gone!"
        setTextColor(ContextCompat.getColor(mContext, android.R.color.holo_red_light))
    }

}