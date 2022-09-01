package com.levibostian.wendyexample


import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.curiosityio.wendyexample.R
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.extension.addTaskStatusListenerForTask
import com.levibostian.wendy.extension.doesErrorExist
import com.levibostian.wendy.extension.isAbleToManuallyRun
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.Wendy
import java.text.SimpleDateFormat
import java.util.*

class PendingTasksRecyclerViewAdapter(val data: List<PendingTask>) : androidx.recyclerview.widget.RecyclerView.Adapter<PendingTasksRecyclerViewAdapter.ViewHolder>() {

    interface Listener {
        fun manuallyRunPressed(task: PendingTask)
        fun resolveErrorPressed(task: PendingTask)
    }

    var listener: Listener? = null

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        var idTextView: TextView = view.findViewById(R.id.id_textview)
        var dataIdTextView: TextView = view.findViewById(R.id.data_id_textview)
        var groupIdTextView: TextView = view.findViewById(R.id.group_id_textview)
        var manuallyRunTextView: TextView = view.findViewById(R.id.manually_run_textview)
        var tagTextView: TextView = view.findViewById(R.id.tag_textview)
        var createdAtTextView: TextView = view.findViewById(R.id.created_at_textview)
        var statusTextView: PendingStatusTextView = view.findViewById(R.id.status_textview)
        var runTaskButton: Button = view.findViewById(R.id.run_ask_button)
        var resolveErrorButton: Button = view.findViewById(R.id.resolve_error_button)
    }

    override fun getItemCount(): Int = data.count()

    override fun onBindViewHolder(holder: PendingTasksRecyclerViewAdapter.ViewHolder, position: Int) {
        val adapterItem: PendingTask = data[position]

        holder.idTextView.text = String.format("task id: %d", adapterItem.taskId)
        holder.dataIdTextView.text = String.format("data id: %s", adapterItem.dataId)
        holder.groupIdTextView.text = String.format("group id: %s", adapterItem.groupId)
        holder.manuallyRunTextView.text = String.format("manually run: %s", adapterItem.manuallyRun.toString())
        holder.tagTextView.text = String.format("tag: %s", adapterItem.tag)
        holder.createdAtTextView.text = String.format("Created: %s", SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH).format(adapterItem.createdAt))

        holder.statusTextView.text = "not running"
        adapterItem.addTaskStatusListenerForTask(holder.statusTextView)

        holder.runTaskButton.visibility = if (adapterItem.isAbleToManuallyRun()) View.VISIBLE else View.GONE
        holder.runTaskButton.setOnClickListener {
            listener?.manuallyRunPressed(adapterItem)
        }
        holder.resolveErrorButton.visibility = if (adapterItem.doesErrorExist()) View.VISIBLE else View.GONE
        holder.resolveErrorButton.setOnClickListener {
            listener?.resolveErrorPressed(adapterItem)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingTasksRecyclerViewAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_pending_task_recyclerview, parent, false))
    }

}