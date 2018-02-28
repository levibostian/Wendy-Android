package com.levibostian.wendy.types

import com.levibostian.wendy.service.PendingTask

/**
 * Reasons why a [PendingTask] was skipped by the task runner.
 */
enum class ReasonPendingTaskSkipped {

    /**
     * The [PendingTask] of the app said that it was not ready to run.
     *
     * @see PendingTask.canRunTask to learn how your app can indicate if it's able to run or not at runtime.
     */
    NOT_READY_TO_RUN {
        override fun <E> accept(visitor: Visitor<E>): E = visitor.visitNotReadyToRun()
    },
    /**
     * If a [PendingTask] runs that has a non-null [PendingTask.group_id] and it fails running, then *all* of the other [PendingTask]s that belongs to the group that have yet to run will all be skipped and rescheduled to run again.
     */
    PART_OF_FAILED_GROUP {
        override fun <E> accept(visitor: Visitor<E>): E = visitor.visitPartOfFailedGroup()
    };

    abstract fun <E> accept(visitor: Visitor<E>): E

    interface Visitor<out E> {
        fun visitNotReadyToRun(): E
        fun visitPartOfFailedGroup(): E
    }

}