package com.pydio.android.cells.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.android.cells.databinding.ListItemJobBinding
import com.pydio.android.cells.db.runtime.RJob

class JobListAdapter(
    private val onItemClicked: (node: RJob, command: String) -> Unit
) : ListAdapter<RJob, JobListAdapter.ViewHolder>(JobDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClicked)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: ListItemJobBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RJob, onItemClicked: (node: RJob, command: String) -> Unit) {
            binding.job = item
            if (item.isFail() || item.isDone()) {
                binding.progress.visibility = View.GONE
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemJobBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class JobDiffCallback : DiffUtil.ItemCallback<RJob>() {

    override fun areItemsTheSame(oldItem: RJob, newItem: RJob): Boolean {
        return oldItem.jobId == newItem.jobId
    }

    override fun areContentsTheSame(oldItem: RJob, newItem: RJob): Boolean {
        return oldItem.doneTimestamp == newItem.doneTimestamp
                && oldItem.message == newItem.message
                && oldItem.progress == newItem.progress
                && oldItem.status == newItem.status
                && oldItem.updateTimestamp == newItem.updateTimestamp
    }
}
