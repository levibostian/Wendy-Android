package com.curiosityio.wendy.service

import com.curiosityio.wendy.error.NetworkException
import com.curiosityio.wendy.model.PendingApiTask
import com.curiosityio.wendy.vo.ErrorResponseVo
import okhttp3.Headers
import okhttp3.ResponseBody
import retrofit2.Response

interface WendyProcessApiResponse {
    fun success(response: Response<Any>, responseBody: Any, headers: Headers)
    fun networkFail(exception: Throwable): Throwable?
    fun apiResponseError(statusCode: Int, response: Response<Any>, headers: Headers, errorVo: Class<out ErrorResponseVo>): Throwable?
}