package com.levibostian.wendy.service

/**
 * Wendy requires that you create a subclass of [PendingTasksFactory] and provide it to Wendy in the [PendingTasks.init] call. Do this in your Application's onCreate() call if you can.
 *
 * This class exists as a map for Wendy to map the tag's of each of your [PendingTask] subclasses with each of your [PendingTask] subclasses. It's annoying to have to provide this, yes, but it works for now.
 *
 * I would like to remove the need of this class in the future. [Check out the issue progress here](https://github.com/levibostian/Wendy-Android/issues/17).
 */
interface PendingTasksFactory {

    /**
     * Wendy will provide a [tag], you provide a blank [PendingTask] for that specific [tag].
     *
     * @sample
     * class WendyExamplePendingTasksFactory : PendingTasksFactory {
     *     override fun getTask(tag: String): PendingTask {
     *         return when (tag) {
     *             FooPendingTask::class.java.simpleName -> FooPendingTask.blank()
     *             else -> throw RuntimeException("No idea what task that is... tag: $tag")
     *         }
     *     }
     *   }
     */
    fun getTask(tag: String): PendingTask

}