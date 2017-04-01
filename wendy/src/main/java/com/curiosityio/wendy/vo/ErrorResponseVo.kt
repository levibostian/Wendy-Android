package com.curiosityio.wendy.vo

abstract class ErrorResponseVo {

    abstract fun getErrorArray(): Array<String?>?

    fun getErrorMessageToDisplayToUser(): String {
        val errors = getErrorArray()
        if (errors == null || errors.isEmpty()) {
            return "Unknown error. Please try again."
        } else {
            errors.forEach {
                if (it != null && it.isNotEmpty()) return it
            }

            return "Unknown error"
        }
    }

}
