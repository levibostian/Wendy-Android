package com.curiosityio.wendy.config

import android.content.Context
import com.curiosityio.wendy.error.WendyErrorNotifier
import com.curiosityio.wendy.runner.PendingApiTasksRunner
import com.curiosityio.wendy.runner.WendyTasksRunnerManager
import com.curiosityio.wendy.service.WendyProcessApiResponse

class WendyConfig {

    companion object {
        var wendyProcessApiResponse: WendyProcessApiResponse? = null
        var wendyErrorNotifier: WendyErrorNotifier? = null
        var wendyTasksRunnerManager: WendyTasksRunnerManager? = null
    }

    class Builder {

        fun setProcessApiResponse(wendyProcessApiResponse: WendyProcessApiResponse): Builder {
            WendyConfig.wendyProcessApiResponse = wendyProcessApiResponse
            return this
        }

        fun setErrorNotifier(wendyErrorNotifier: WendyErrorNotifier): Builder {
            WendyConfig.wendyErrorNotifier = wendyErrorNotifier
            return this
        }

        fun setTasksRunnerManager(wendyTasksRunnerManager: WendyTasksRunnerManager): Builder {
            WendyConfig.wendyTasksRunnerManager = wendyTasksRunnerManager
            return this
        }

        fun build(context: Context) {
            if (WendyConfig.wendyProcessApiResponse == null ||
                    WendyConfig.wendyErrorNotifier == null ||
                    WendyConfig.wendyTasksRunnerManager == null) {
                throw RuntimeException("You did not fully initialize Wendy. In the WendyConfig.Builder(), make sure to set all of the config objects.")
            } else {
                PendingApiTasksRunner.init(context)
            }
        }

    }

}
