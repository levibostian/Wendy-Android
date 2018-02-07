package com.curiosityio.wendy.model

import com.curiosityio.wendy.vo.ErrorResponseVo
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmQuery
import retrofit2.Response
import rx.Completable
import rx.Observable
import java.util.*

interface PendingApiTask<RESPONSE: Any> {

    fun buildQueryForExistingTask(realmQuery: RealmQuery<RealmObject>): RealmQuery<RealmObject>

    fun canRunTask(realm: Realm): Boolean

    var created_at: Date
    var manually_run_task: Boolean

    fun getOfflineModelTaskRepresents(realm: Realm): OfflineCapableModel

    fun getApiCall(realm: Realm): Observable<Response<RESPONSE>>

    fun getApiErrorVo(): Class<out ErrorResponseVo>

    fun processApiResponse(realm: Realm, response: RESPONSE)

}