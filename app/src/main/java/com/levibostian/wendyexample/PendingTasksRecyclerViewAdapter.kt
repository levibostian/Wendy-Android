package com.levibostian.wendyexample


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.curiosityio.wendyexample.R
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks
import com.levibostian.wendy.types.ReasonPendingTaskSkipped
import java.text.SimpleDateFormat
import java.util.*

class PendingTasksRecyclerViewAdapter(val data: List<PendingTask>) : RecyclerView.Adapter<PendingTasksRecyclerViewAdapter.ViewHolder>() {

    interface Listener {
        fun manuallyRunPressed(task: PendingTask)
        fun resolveErrorPressed(task: PendingTask)
    }

    var listener: Listener? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        holder.idTextView.text = String.format("task id: %d", adapterItem.task_id)
        holder.dataIdTextView.text = String.format("data id: %s", adapterItem.data_id)
        holder.groupIdTextView.text = String.format("group id: %s", adapterItem.group_id)
        holder.manuallyRunTextView.text = String.format("manually run: %s", adapterItem.manually_run.toString())
        holder.tagTextView.text = String.format("tag: %s", adapterItem.tag)
        holder.createdAtTextView.text = String.format("Created: %s", SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH).format(adapterItem.created_at))

        holder.statusTextView.text = "not running"
        WendyConfig.addTaskStatusListenerForTask(adapterItem.task_id, holder.statusTextView)

        holder.runTaskButton.setOnClickListener {
            listener?.manuallyRunPressed(adapterItem)
        }
        holder.resolveErrorButton.visibility = if (PendingTasks.shared.doesErrorExist(adapterItem.task_id)) View.VISIBLE else View.GONE
        holder.resolveErrorButton.setOnClickListener {
            listener?.resolveErrorPressed(adapterItem)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingTasksRecyclerViewAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_pending_task_recyclerview, parent, false))
    }

}