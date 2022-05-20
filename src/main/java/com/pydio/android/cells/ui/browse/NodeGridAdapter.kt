package com.pydio.android.cells.ui.browse

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.databinding.GridItemNodeBinding
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.FileService

class NodeGridAdapter(
    private val onItemClicked: (node: RTreeNode, command: String) -> Unit
) : ListAdapter<RTreeNode, NodeGridAdapter.ViewHolder>(TreeNodeDiffCallback()), KoinComponent {

    private val fileService: FileService by inject()
    var tracker: SelectionTracker<String>? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val thumbDirPath =
            fileService.dataParentPath(item.getAccountID(), AppNames.LOCAL_FILE_TYPE_THUMB)
        tracker?.let {
            holder.bind(item, thumbDirPath, it.isSelected(item.encodedState))
        } ?: run {
            holder.bind(item, thumbDirPath)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return from(parent).with(onItemClicked)
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

    inner class ViewHolder(val binding: GridItemNodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RTreeNode, thumbDirPath: String?, isSelected: Boolean = false) {
            binding.node = item
            binding.thumbDirPath = thumbDirPath

            binding.nodeCard.isActivated = isSelected
            binding.nodeCard.isChecked = isSelected

            if (this@NodeGridAdapter.tracker?.hasSelection() ?: false) {
                binding.moreBtnLayout.visibility = View.GONE
            } else {
                binding.moreBtnLayout.visibility = View.VISIBLE
            }
//            binding.rowLayout.isActivated = isSelected
            //   binding.nodeDetails.isActivated = isSelected
            binding.executePendingBindings()
        }

        fun with(
            onItemClicked: (node: RTreeNode, command: String) -> Unit
        ): ViewHolder {

            binding.root.setOnClickListener {
                binding.node?.let { onItemClicked(it, AppNames.ACTION_OPEN) }
            }

            binding.gridItemMoreButton.setOnClickListener {
                binding.node?.let { onItemClicked(it, AppNames.ACTION_MORE) }
            }
            return this
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> =
            object : ItemDetailsLookup.ItemDetails<String>() {
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): String = binding.node!!.encodedState
            }

    }

    fun from(parent: ViewGroup): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = GridItemNodeBinding.inflate(layoutInflater, parent, false)
        return this.ViewHolder(binding)
    }
}

class NodeGridItemKeyProvider(private val adapter: NodeGridAdapter) :
    ItemKeyProvider<String>(SCOPE_MAPPED) {

    override fun getKey(position: Int): String {
        return adapter.doGetKey(position)
    }

    override fun getPosition(key: String): Int {
        return adapter.doGetPosition(key)
    }
}

class NodeGridItemDetailsLookup(private val recyclerView: RecyclerView) :

    ItemDetailsLookup<String>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<String>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as NodeGridAdapter.ViewHolder)
                .getItemDetails()
        }
        return null
    }
}
