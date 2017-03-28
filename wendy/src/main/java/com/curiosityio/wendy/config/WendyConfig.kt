package com.curiosityio.wendy.config

import com.curiosityio.wendy.error.WendyErrorNotifier
import com.curiosityio.wendy.runner.WendyTasksRunnerManager
import com.curiosityio.wendy.service.WendyProcessApiResponse

class WendyConfig {

    companion object {
        var wendyProcessApiResponse: WendyProcessApiResponse? = null
        var wendyErrorNotifier: WendyErrorNotifier? = null
        var wendyTasksRunnerManager: WendyTasksRunnerManager? = null

        fun overrideProcessApiResponse(wendyProcessApiResponse: WendyProcessApiResponse) {
            this.wendyProcessApiResponse = wendyProcessApiResponse
        }

        fun setErrorNotifier(wendyErrorNotifier: WendyErrorNotifier) {
            this.wendyErrorNotifier = wendyErrorNotifier
        }

        fun setTasksRunnerManager(wendyTasksRunnerManager: WendyTasksRunnerManager) {
            this.wendyTasksRunnerManager = wendyTasksRunnerManager
        }
    }

}
