package com.curiosityio.wendy.service

import okhttp3.Headers
import okhttp3.ResponseBody

interface WendyProcessApiResponse {
    fun success(response: Any, headers: Headers)
    fun error(statusCode: Int, response: ResponseBody, headers: Headers): Throwable?
}