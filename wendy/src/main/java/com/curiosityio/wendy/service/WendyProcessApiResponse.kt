package com.curiosityio.wendy.service

import com.curiosityio.wendy.error.NetworkException
import okhttp3.Headers
import okhttp3.ResponseBody

interface WendyProcessApiResponse {
    fun success(response: Any, headers: Headers)
    fun networkFail(exception: Throwable): Throwable?
    fun apiResponseError(statusCode: Int, response: ResponseBody, headers: Headers): Throwable?
}