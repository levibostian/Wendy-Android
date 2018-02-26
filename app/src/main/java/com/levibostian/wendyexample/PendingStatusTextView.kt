package com.levibostian.wendyexample

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.curiosityio.wendyexample.R
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.types.ReasonPendingTaskSkipped

class PendingStatusTextView : TextView, PendingTaskStatusListener {

    private lateinit var mContext: Context

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        initialize(context, attrs, defStyleAttr)
    }
    @TargetApi(LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes) {
        initialize(context, attrs, defStyleAttr)
    }

    private fun initialize(context: Context, attrs: AttributeSet, defStyleAttr: Int) {
        mContext = context
    }

    override fun skipped(taskId: Long, reason: ReasonPendingTaskSkipped) {
        text = reason.accept(object : ReasonPendingTaskSkipped.Visitor<String> {
            override fun visitNotReadyToRun(): String {
                return "Skipped: not ready to run"
            }
        })
    }
    override fun running(taskId: Long) {
        text = "Running"
    }
    override fun complete(taskId: Long, successful: Boolean) {
        text = if (successful) "Success!" else "Failed!"
    }

}