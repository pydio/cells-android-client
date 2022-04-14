package com.pydio.android.cells.ui.transfer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.cells.transport.StateID
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.databinding.ListItemAccountBinding
import com.pydio.android.cells.db.accounts.RLiveSession

class SessionListAdapter(
    private val onItemClicked: (stateID: StateID, command: String) -> Unit
) : ListAdapter<RLiveSession, SessionListAdapter.ViewHolder>(SessionsDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent).with(onItemClicked)
    }

    class ViewHolder private constructor(val binding: ListItemAccountBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RLiveSession) {
            binding.session = item
            binding.executePendingBindings()
        }

        fun with(
            onItemClicked: (stateID: StateID, command: String) -> Unit
        ): ViewHolder {
            binding.root.setOnClickListener {
                binding.session?.let {
                    onItemClicked(StateID.fromId(it.accountID), AppNames.ACTION_OPEN)
                }
            }
            return this
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemAccountBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class SessionsDiffCallback : DiffUtil.ItemCallback<RLiveSession>() {

    override fun areItemsTheSame(oldItem: RLiveSession, newItem: RLiveSession): Boolean {
        return oldItem.accountID == newItem.accountID
    }

    override fun areContentsTheSame(oldItem: RLiveSession, newItem: RLiveSession): Boolean {
        return oldItem.authStatus == newItem.authStatus
    }
}
