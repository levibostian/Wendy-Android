package com.levibostian.wendyexample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

import com.curiosityio.wendyexample.R
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

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
                PendingTasks.sharedInstance().addTask(pendingTask)
            }
        }

        activity_main_reset_tasks_button.setOnClickListener {
            PendingTasks.sharedInstance().resetTasks()
        }
    }

}
