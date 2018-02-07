package com.levibostian.wendyexample

import com.levibostian.wendy.service.PendingTask

class FooPendingTask : PendingTask() {

    companion object {
        fun blank(): FooPendingTask { return FooPendingTask() }
    }

    override fun runTask(complete: (successful: Boolean) -> Unit) {
        // Here, instantiate your dependencies, talk to your DB, your API, etc. Run the task.
        complete(true)
    }

}