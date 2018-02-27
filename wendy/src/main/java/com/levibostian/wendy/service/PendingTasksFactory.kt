package com.levibostian.wendy.service

interface PendingTasksFactory {

    /**
     * This exists because I could not figure out how to save a generic class that overrides [PendingTask] in the sqlite database and instantiate it after querying it. So, instead I get the instance here via the tag after I query it.
     */
    fun getTask(tag: String): PendingTask

}