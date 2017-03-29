package com.curiosityio.wendy

import com.curiosityio.wendy.manager.PendingApiTasksManager
import com.curiosityio.wendy.model.PendingApiTask
import org.junit.Test

import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito

/**
 * Example local unit test, which will execute on the development machine (host).

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class PendingApiTasksManagerTest {

    @Test
    @Throws(Exception::class)
    fun registeringPendingApiTasks_isCorrect() {
        val apiTask = Mockito.mock(PendingApiTask::class.java)
        PendingApiTasksManager.registerPendingApiTasks(apiTask::class.java)

        assertEquals(1, PendingApiTasksManager.registeredPendingApiTasks.size)
        assertEquals(apiTask::class.java, PendingApiTasksManager.registeredPendingApiTasks[0])
    }

}