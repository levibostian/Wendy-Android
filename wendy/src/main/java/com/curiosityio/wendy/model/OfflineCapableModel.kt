package com.curiosityio.wendy.model

interface OfflineCapableModel {

    var realm_id: Int
    var api_id: Int

    fun setRealmIdToApiId() // GSON does not allow you to have 2+ fields of same name. I must manually set the realm_id from api_id here so they both match. This function is meant to set realm_id = api_id for parent and sift down children.

}
