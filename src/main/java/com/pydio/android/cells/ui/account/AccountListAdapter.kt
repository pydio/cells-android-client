package com.pydio.android.cells.ui.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.databinding.ListItemAccountBinding
import com.pydio.android.cells.db.accounts.RLiveSession

class AccountListAdapter(
    private val onItemClicked: (accountID: String, action: String) -> Unit,
) : ListAdapter<RLiveSession, AccountListAdapter.ViewHolder>(LiveSessionDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, onItemClicked)
    }

    class ViewHolder(
        val binding: ListItemAccountBinding,
        val onItemClicked: (accountID: String, action: String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RLiveSession) {

            binding.session = item

            // TODO also retrieve user's avatar for configured account in the current remote
            binding.root.setOnClickListener {
                onItemClicked(item.accountID, AppNames.ACTION_OPEN)
            }

            binding.accountDeleteButton.setOnClickListener {
                onItemClicked(item.accountID, AppNames.ACTION_FORGET)
            }

            binding.accountAuthButton.setOnClickListener {
                if (item.authStatus == AppNames.AUTH_STATUS_CONNECTED) {
                    onItemClicked(item.accountID, AppNames.ACTION_LOGOUT)
                } else {
                    onItemClicked(item.accountID, AppNames.ACTION_LOGIN)
                }
            }

            binding.executePendingBindings()
        }

        companion object {
            fun from(
                parent: ViewGroup,
                onItemClicked: (accountID: String, action: String) -> Unit
            ): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemAccountBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, onItemClicked)
            }
        }
    }

    class LiveSessionDiffCallback : DiffUtil.ItemCallback<RLiveSession>() {
        override fun areItemsTheSame(oldItem: RLiveSession, newItem: RLiveSession): Boolean {
            // Used when order changes for instance
            return oldItem.accountID == newItem.accountID
        }

        override fun areContentsTheSame(oldItem: RLiveSession, newItem: RLiveSession): Boolean {
            // This relies on Room's auto-generated equality to check
            // if the corresponding view needs to be redrawn.
            // This can be further configured in a more complex scenario
            return oldItem == newItem
        }
    }
}
