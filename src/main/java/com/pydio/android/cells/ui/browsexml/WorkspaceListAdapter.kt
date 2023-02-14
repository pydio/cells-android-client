package com.pydio.android.cells.ui.browsexml

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.cells.api.SdkNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.databinding.GridHeaderBinding
import com.pydio.android.cells.databinding.GridItemWorkspaceBinding
import com.pydio.android.cells.db.accounts.RWorkspace

/**
 * Custom adapter used for the account landing page.
 * It manages a sorted list of workspaces and Cells with headers.
 */
class WorkspaceListAdapter(
    private val onItemClicked: (slug: String, action: String) -> Unit
) : ListAdapter<WsDataItem, RecyclerView.ViewHolder>(WorkspaceDiffCallback()) {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            AppNames.ITEM_TYPE_HEADER -> HeaderHolder.from(parent)
            AppNames.ITEM_TYPE_WS -> WsCardHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is WsDataItem.HeaderItem -> AppNames.ITEM_TYPE_HEADER
            is WsDataItem.WorkspaceItem -> AppNames.ITEM_TYPE_WS
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {
            is WsCardHolder -> {
                val nodeItem = getItem(position) as WsDataItem.WorkspaceItem
                holder.bind(nodeItem.wsNode, onItemClicked)
            }
            is HeaderHolder -> {
                val headerItem = getItem(position) as WsDataItem.HeaderItem
                holder.bind(headerItem.type)
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<RWorkspace>) {
        adapterScope.launch {
            // Workspace list are usually very small (a worse hundreds of elements) => we go the simple way
            val wss = mutableListOf<WsDataItem>(WsDataItem.HeaderItem(SdkNames.WS_TYPE_DEFAULT))
            val cells = mutableListOf<WsDataItem>(WsDataItem.HeaderItem(SdkNames.WS_TYPE_CELL))

            for (node in list) {
                // TODO hard-coded prefix. enhance
                if (node.sortName?.startsWith("8_") == true) {
                    cells.add(WsDataItem.WorkspaceItem(node))
                } else {
                    wss.add(WsDataItem.WorkspaceItem(node))
                }
            }

            var items = listOf<WsDataItem>()
            if (wss.size > 1) {
                items = wss
            }
            if (cells.size > 1) {
                items = items + cells
            }

            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    class WsCardHolder private constructor(val binding: GridItemWorkspaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RWorkspace, onItemClicked: (slug: String, command: String) -> Unit) {
            binding.workspace = item
            binding.root.setOnClickListener {
                binding.workspace?.let {
                    onItemClicked(it.slug, AppNames.ACTION_OPEN)
                }
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): WsCardHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = GridItemWorkspaceBinding.inflate(layoutInflater, parent, false)
                return WsCardHolder(binding)
            }
        }
    }

    class HeaderHolder private constructor(val binding: GridHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(type: String) {
            binding.type = type
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): HeaderHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = GridHeaderBinding.inflate(layoutInflater, parent, false)
                return HeaderHolder(binding)
            }
        }
    }
}

class WorkspaceDiffCallback : DiffUtil.ItemCallback<WsDataItem>() {

    override fun areItemsTheSame(oldItem: WsDataItem, newItem: WsDataItem): Boolean {
        return oldItem.encodedState == newItem.encodedState
    }

    override fun areContentsTheSame(oldItem: WsDataItem, newItem: WsDataItem): Boolean {
        return oldItem == newItem
    }
}

sealed class WsDataItem {

    abstract val encodedState: String

    data class WorkspaceItem(val wsNode: RWorkspace) : WsDataItem() {
        override val encodedState = wsNode.encodedState
    }

    data class HeaderItem(val type: String) : WsDataItem() {
        override val encodedState: String = type
    }
}
