package com.curiosityio.wendy.vo

abstract class ErrorResponseVo {

    abstract val errorArray: Array<String>?

    val errorMessageToDisplayToUser: String
        get() {
            val errors = errorArray

            if (errors == null || errors.isEmpty()) {
                return "Unknown error. Please try again."
            } else {
                return errors[0]
            }
        }

}
