package com.curiosityio.wendy.error

// Thrown when API response code >= 500
class APIDownException(message: String) : Exception(message)