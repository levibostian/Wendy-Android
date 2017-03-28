package com.curiosityio.wendy.error

// This should not happen. This happens when the API returns back JSON with an error inside of it that I did not know about and put into the app.
class ParseErrorFromAPIException(message: String) : Exception(message)