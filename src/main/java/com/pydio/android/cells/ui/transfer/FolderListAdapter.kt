package com.pydio.android.cells.ui.transfer

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.databinding.ListItemParentBinding
import com.pydio.android.cells.databinding.ListItemTargetLocationBinding
import com.pydio.android.cells.db.nodes.RTreeNode

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

class FolderListAdapter(
    private val currFolderStateID: StateID,
    private val activityContext: String,
    private val onItemClicked: (stateID: StateID, command: String) -> Unit
) : ListAdapter<DataItem, RecyclerView.ViewHolder>(PickFolderDiffCallback()) {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> HeaderHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> TreeNodeHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.HeaderItem -> ITEM_VIEW_TYPE_HEADER
            is DataItem.TreeNodeItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TreeNodeHolder -> {
                val nodeItem = getItem(position) as DataItem.TreeNodeItem
                holder.bind(nodeItem.treeNode, onItemClicked)
            }
            is HeaderHolder -> {
                holder.bind(StateID.fromId(getItem(position).encodedState), onItemClicked)
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<RTreeNode>?) {
        adapterScope.launch {

            val parentState =
                if (Str.empty(currFolderStateID.workspace)) null else currFolderStateID.parent()

            var items = when (list) {
                null -> listOf(DataItem.HeaderItem(parentState))
                else -> listOf(DataItem.HeaderItem(parentState)) + list.map {
                    DataItem.TreeNodeItem(
                        it
                    )
                }
            }

            if (activityContext != AppNames.ACTION_UPLOAD && currFolderStateID.fileName == null) {
                // Do not show "parent" header for account and ws roots
                items = when (list) {
                    null -> listOf()
                    else -> list.map { DataItem.TreeNodeItem(it) }
                }
            }

            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    class TreeNodeHolder private constructor(val binding: ListItemTargetLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RTreeNode, onItemClicked: (stateID: StateID, command: String) -> Unit) {
            binding.node = item
            binding.executePendingBindings()
            binding.root.setOnClickListener {
                binding.node?.let {
                    // Handle corner case when we list folder roots
                    val tmpID = it.getStateID()
                    onItemClicked(tmpID, AppNames.ACTION_OPEN)
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): TreeNodeHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemTargetLocationBinding.inflate(layoutInflater, parent, false)
                return TreeNodeHolder(binding)
            }
        }
    }

    class HeaderHolder private constructor(val binding: ListItemParentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(parentState: StateID, onItemClicked: (stateID: StateID, command: String) -> Unit) {
            binding.parentState = parentState
            binding.executePendingBindings()
            binding.root.setOnClickListener {
                binding.parentState?.let {
                    onItemClicked(it, AppNames.ACTION_OPEN)
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): HeaderHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemParentBinding.inflate(layoutInflater, parent, false)
                return HeaderHolder(binding)
            }
        }
    }
}

class PickFolderDiffCallback : DiffUtil.ItemCallback<DataItem>() {

    private val tag = "PickFolderDiffCallback"

    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {

        val same = oldItem.encodedState == newItem.encodedState
        if (!same) {
            Log.d(tag, "${oldItem.encodedState} != ${newItem.encodedState}")
        }
        return same
    }

    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return if (
            (oldItem is DataItem.HeaderItem && newItem is DataItem.TreeNodeItem) ||
            (oldItem is DataItem.TreeNodeItem && newItem is DataItem.HeaderItem)
        ) {
            false
        } else if (oldItem is DataItem.HeaderItem && newItem is DataItem.HeaderItem) {
            true
        } else
            (oldItem as DataItem.TreeNodeItem).treeNode.remoteModificationTS ==
                    (newItem as DataItem.TreeNodeItem).treeNode.remoteModificationTS
    }
}

sealed class DataItem {

    abstract val encodedState: String

    data class TreeNodeItem(val treeNode: RTreeNode) : DataItem() {
        override val encodedState = treeNode.encodedState
    }

    data class HeaderItem(val stateID: StateID?) : DataItem() {
        override val encodedState: String = stateID?.id ?: AppNames.CELLS_ROOT_ENCODED_STATE
    }
}
