package com.levibostian.wendy.db


interface PendingTaskFields {
    var id: Long
    var created_at: Long
    var manually_run: Boolean
    var group_id: String?
    var data_id: String?
    var tag: String
}