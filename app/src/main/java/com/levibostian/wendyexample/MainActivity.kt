package com.levibostian.wendyexample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast

import com.curiosityio.wendyexample.R
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.listeners.TaskRunnerListener
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.Wendy
import com.levibostian.wendy.types.ReasonPendingTaskSkipped
import com.levibostian.wendyexample.extension.closeKeyboard
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), TaskRunnerListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        activity_main_create_pending_task_button.setOnClickListener {
            if (activity_main_custom_data_edittext.text.isBlank()) {
                activity_main_custom_data_edittext.error = "You must enter data here."
            } else {
                val pendingTask = FooPendingTask(
                        activity_main_manually_run_checkbox.isChecked,
                        if (activity_main_group_id_edittext.text.isNullOrBlank()) null else activity_main_group_id_edittext.text.toString(),
                        activity_main_custom_data_edittext.text.toString()
                )
                Wendy.sharedInstance().addTask(pendingTask)
                closeKeyboard()
            }
        }
        
        activity_main_automatically_run_tasks_checkbox.setOnCheckedChangeListener { _, isChecked ->
            WendyConfig.automaticallyRunTasks = isChecked
        }
        WendyConfig.automaticallyRunTasks = activity_main_automatically_run_tasks_checkbox.isChecked

        activity_main_run_all_tasks_button.setOnClickListener {
            Wendy.sharedInstance().runTasks(null)
        }
        activity_main_clear_all_data_button.setOnClickListener {
            Toast.makeText(this, "Clearing data...", Toast.LENGTH_SHORT).show()
            Wendy.shared.clear {
                refreshListOfTasks()
                Toast.makeText(this, "Data cleared!", Toast.LENGTH_SHORT).show()
            }
        }

        activity_main_tasks_recyclerview.layoutManager = LinearLayoutManager(this)
        refreshListOfTasks()

        WendyConfig.addTaskRunnerListener(this)
    }

    private fun refreshListOfTasks() {
        val recyclerViewAdapter = PendingTasksRecyclerViewAdapter(Wendy.sharedInstance().getAllTasks())
        recyclerViewAdapter.listener = object : PendingTasksRecyclerViewAdapter.Listener {
            override fun manuallyRunPressed(task: PendingTask) {
                Wendy.sharedInstance().runTask(task.taskId!!)
            }
            override fun resolveErrorPressed(task: PendingTask) {
                Wendy.shared.resolveError(task.taskId!!)
            }
        }
        activity_main_tasks_recyclerview.adapter = recyclerViewAdapter
    }

    override fun newTaskAdded(task: PendingTask) {
        refreshListOfTasks()
    }
    override fun runningTask(task: PendingTask) {
    }
    override fun taskSkipped(reason: ReasonPendingTaskSkipped, task: PendingTask) {
    }
    override fun taskComplete(success: Boolean, task: PendingTask) {
        Handler().postDelayed({
            refreshListOfTasks()
        }, 1000)
    }
    override fun allTasksComplete() {
    }
    override fun errorRecorded(task: PendingTask, errorMessage: String?, errorId: String?) {
        Handler().postDelayed({
            refreshListOfTasks()
        }, 1000)

        val notificationManager = NotificationManagerCompat.from(this)
        val notification = NotificationCompat.Builder(this, NotificationChannelUtil.ERROR_OCCURRED_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_notification_clear_all)
                .setContentTitle("Error occurred!")
                .setContentText(errorMessage)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        notificationManager.notify(0, notification)
    }
    override fun errorResolved(task: PendingTask) {
        refreshListOfTasks()
    }

}
