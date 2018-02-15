package com.levibostian.wendy.types

enum class ReasonPendingTaskSkipped {

    NOT_READY_TO_RUN {
        override fun <E> accept(visitor: Visitor<E>): E = visitor.visitNotReadyToRun()
    };

    abstract fun <E> accept(visitor: Visitor<E>): E

    interface Visitor<out E> {
        fun visitNotReadyToRun(): E
    }

}