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
    };

    abstract fun <E> accept(visitor: Visitor<E>): E

    interface Visitor<out E> {
        fun visitNotReadyToRun(): E
    }

}