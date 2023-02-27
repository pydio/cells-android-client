package com.pydio.android.cells.ui.aaLegacy.transferxml

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.databinding.ListItemTransferBinding
import com.pydio.android.cells.db.nodes.RTransfer

class TransferListAdapter(
    private val onItemClicked: (node: RTransfer, command: String) -> Unit
) : ListAdapter<RTransfer, TransferListAdapter.ViewHolder>(TransferDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClicked)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: ListItemTransferBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RTransfer, onItemClicked: (node: RTransfer, command: String) -> Unit) {
            binding.transfer = item
            binding.moreButton.setOnClickListener {
                onItemClicked(item, AppNames.ACTION_MORE)
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemTransferBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class TransferDiffCallback : DiffUtil.ItemCallback<RTransfer>() {

    override fun areItemsTheSame(oldItem: RTransfer, newItem: RTransfer): Boolean {
        return oldItem.encodedState == newItem.encodedState
    }

    override fun areContentsTheSame(oldItem: RTransfer, newItem: RTransfer): Boolean {
        return  oldItem.doneTimestamp == newItem.doneTimestamp
                && oldItem.error == newItem.error
                && oldItem.progress == newItem.progress
    }
}
