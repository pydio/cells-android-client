package com.pydio.android.cells.ui.aaLegacy.browsexml

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.databinding.ListItemNodeBinding
import com.pydio.android.cells.databinding.ListItemSearchBinding
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.menus.TreeNodeMenuFragment
import com.pydio.cells.utils.Log

/**
 * Custom adapter for browsing folders with a recycler view using a list layout.
 * Also provides necessary callbacks to use the RecyclerView.Selection library
 * that enables selecting more than one element at a time.
 */
class NodeListAdapter(
    private val context: String,
    private val onItemClicked: (node: RTreeNode, command: String) -> Unit
) : ListAdapter<RTreeNode, RecyclerView.ViewHolder>(TreeNodeDiffCallback()) {

    // private val logTag = NodeListAdapter::class.simpleName
    private var tracker: SelectionTracker<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (context) {
            TreeNodeMenuFragment.CONTEXT_BROWSE -> BrowseViewHolder.from(parent)
                .with(onItemClicked)
            TreeNodeMenuFragment.CONTEXT_SEARCH -> SearchViewHolder.from(parent)
                .with(onItemClicked)
            else -> throw ClassCastException("Unknown context $context")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BrowseViewHolder -> {
                val item = getItem(position)
                tracker?.let {
                    holder.bind(item, it.isSelected(item.encodedState))
                } ?: run {
                    holder.bind(item)
                }
            }
            is SearchViewHolder -> {
                holder.bind(getItem(position))
            }
        }
    }

    fun setTracker(tracker: SelectionTracker<String>) {
        this.tracker = tracker
    }

    fun doGetKey(position: Int): String {
        val item = getItem(position)
        return item.encodedState
    }

    fun doGetPosition(key: String): Int {
        // TODO "brut force" retrieval of an item... Must be enhanced.
        for ((i, node) in currentList.withIndex()) {
            if (node.encodedState == key) {
                return i
            }
        }
        return -1
    }

    class BrowseViewHolder private constructor(val binding: ListItemNodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val tag = BrowseViewHolder::class.simpleName

        fun bind(item: RTreeNode, isSelected: Boolean = false) {
            binding.node = item
            binding.rowLayout.isActivated = isSelected
            binding.executePendingBindings()
        }

        fun with(
            onItemClicked: (node: RTreeNode, command: String) -> Unit
        ): BrowseViewHolder {

            binding.root.setOnClickListener {
                Log.i(tag, "onItemClicked")
                binding.node?.let { onItemClicked(it, AppNames.ACTION_OPEN) }
            }

            binding.moreButton.setOnClickListener {
                binding.node?.let { onItemClicked(it, AppNames.ACTION_MORE) }
            }

            return this
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> =
            object : ItemDetailsLookup.ItemDetails<String>() {
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): String {
                    val node = binding.node
                    if (node == null) {
                        Log.e("NodeListAdapter.BrowseViewHolder", "getSelectionKey for $node")
                        return "no state"
                    }
                    return node.encodedState
                }
            }

        companion object {
            fun from(parent: ViewGroup): BrowseViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemNodeBinding.inflate(layoutInflater, parent, false)
                return BrowseViewHolder(binding)
            }
        }
    }

    class SearchViewHolder private constructor(val binding: ListItemSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // private val tag = SearchViewHolder::class.simpleName

        fun bind(item: RTreeNode) {
            binding.node = item
            // binding.rowLayout.isActivated = isSelected
            binding.executePendingBindings()
        }

        fun with(onItemClicked: (node: RTreeNode, command: String) -> Unit): SearchViewHolder {

            binding.root.setOnClickListener {
                // Log.d(tag, "onItemClicked")
                binding.node?.let { onItemClicked(it, AppNames.ACTION_OPEN) }
            }

            binding.moreButton.setOnClickListener {
                binding.node?.let { onItemClicked(it, AppNames.ACTION_MORE) }
            }
            return this
        }

//        fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> =
//            object : ItemDetailsLookup.ItemDetails<String>() {
//                override fun getPosition(): Int = adapterPosition
//                override fun getSelectionKey(): String {
//                    val node = binding.node
//                    if (node == null) {
//                        Log.e("NodeListAdapter.ViewHolder", "getSelectionKey for $node")
//                        return "no state"
//                    }
//                    return node.encodedState
//                }
//            }

        companion object {
            fun from(parent: ViewGroup): SearchViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemSearchBinding.inflate(layoutInflater, parent, false)
                return SearchViewHolder(binding)
            }
        }
    }
}

class NodeListItemKeyProvider(private val adapter: NodeListAdapter) :
    ItemKeyProvider<String>(SCOPE_MAPPED) {

    override fun getKey(position: Int): String {
        return adapter.doGetKey(position)
    }

    override fun getPosition(key: String): Int {
        return adapter.doGetPosition(key)
    }
}

class NodeListItemDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<String>() {

    private val logTag = NodeListItemDetailsLookup::class.simpleName

    override fun getItemDetails(event: MotionEvent): ItemDetails<String>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            val childViewHolder = recyclerView.getChildViewHolder(view)
            if (childViewHolder is NodeListAdapter.BrowseViewHolder) {
                return childViewHolder.getItemDetails()
            } else {
                Log.e(logTag, "unexpected holder type: $childViewHolder ")
            }
        }
        return null
    }
}

class TreeNodeDiffCallback : DiffUtil.ItemCallback<RTreeNode>() {

    override fun areItemsTheSame(oldItem: RTreeNode, newItem: RTreeNode): Boolean {
        return oldItem.encodedState == newItem.encodedState
    }

    override fun areContentsTheSame(oldItem: RTreeNode, newItem: RTreeNode): Boolean {
        return oldItem.isContentEquals(newItem)
    }
}
