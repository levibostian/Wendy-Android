package com.curiosityio.wendy.error

interface WendyErrorNotifier {
    fun errorEncountered(error: Throwable)
}
