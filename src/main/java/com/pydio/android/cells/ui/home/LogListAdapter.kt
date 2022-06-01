package com.pydio.android.cells.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.android.cells.databinding.ListItemLogBinding
import com.pydio.android.cells.db.runtime.RLog

class LogListAdapter(
    private val onItemClicked: (node: RLog, command: String) -> Unit
) : ListAdapter<RLog, LogListAdapter.ViewHolder>(LogDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClicked)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: ListItemLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RLog, onItemClicked: (node: RLog, command: String) -> Unit) {
            binding.log = item
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemLogBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class LogDiffCallback : DiffUtil.ItemCallback<RLog>() {

    override fun areItemsTheSame(oldItem: RLog, newItem: RLog): Boolean {
        return oldItem.logId == newItem.logId
    }

    override fun areContentsTheSame(oldItem: RLog, newItem: RLog): Boolean {
        // log records never changes..
        return true
    }
}
