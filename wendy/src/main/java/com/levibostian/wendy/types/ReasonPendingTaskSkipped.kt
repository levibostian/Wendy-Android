package com.levibostian.wendy.types

import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks

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
        /**
         * @see [Visitor]
         */
        override fun <E> accept(visitor: Visitor<E>): E = visitor.visitNotReadyToRun()
    },
    /**
     * If a [PendingTask] runs that has a non-null [PendingTask.groupId] and it fails running, then *all* of the other [PendingTask]s that belongs to the group that have yet to run will all be skipped and rescheduled to run again.
     */
    PART_OF_FAILED_GROUP {
        /**
         * @see [Visitor]
         */
        override fun <E> accept(visitor: Visitor<E>): E = visitor.visitPartOfFailedGroup()
    },
    /**
     * If there exists a recorded error for [PendingTask]. The [PendingTask] will execute again when [PendingTasks.resolveError] is called on the [PendingTask].
     */
    UNRESOLVED_RECORDED_ERROR {
        /**
         * @see [Visitor]
         */
        override fun <E> accept(visitor: Visitor<E>): E = visitor.visitUnresolvedRecordedError()
    };

    /**
     * If you have ever used the Visitor Pattern before in Java/Kotlin, this is for you.
     *
     * @see [Visitor]
     */
    abstract fun <E> accept(visitor: Visitor<E>): E

    /**
     * If you have ever used the Visitor Pattern before in Java/Kotlin, this is for you.
     *
     * @see [accept]
     */
    interface Visitor<out E> {
        /**
         * @see [NOT_READY_TO_RUN]
         */
        fun visitNotReadyToRun(): E
        /**
         * @see [PART_OF_FAILED_GROUP]
         */
        fun visitPartOfFailedGroup(): E
        /**
         * @see [UNRESOLVED_RECORDED_ERROR]
         */
        fun visitUnresolvedRecordedError(): E
    }

}