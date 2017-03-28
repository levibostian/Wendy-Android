package com.curiosityio.wendy.error

// 403 API call. This should not be seen by an end user. The app should not give them the ability to even try doing this.
class UserNotEnoughPrivilegesException(message: String) : Exception(message)