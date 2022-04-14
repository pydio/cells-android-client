package com.pydio.android.cells.ui.viewer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.cells.transport.StateID
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.isPreViewable

/**
 * Holds a list of viewable images for the carousel. It takes care of preloading data in background
 * in advance in order to reduce loading time when the user wants to see a given image.
 */
class CarouselViewModel(private val nodeService: NodeService) : ViewModel() {

    private val logTag = CarouselViewModel::class.simpleName

    private lateinit var _allChildren: LiveData<List<RTreeNode>>
    val allChildren: LiveData<List<RTreeNode>>
        get() = _allChildren

    private val _elements = MutableLiveData<MutableList<RTreeNode>>().default(mutableListOf())
    val elements: LiveData<MutableList<RTreeNode>>
        get() = _elements

    private lateinit var _currActive: StateID
    val currActive: StateID
        get() = _currActive

    fun afterCreate(parentFolder: StateID, startElement: StateID) {
        _currActive = startElement
        // We cannot rely yet on the mime type that is most of the time not correct
        // elements = nodeService.listViewable(parentFolder, "image/")
        _allChildren = nodeService.ls(parentFolder)
        _allChildren.value?.let {
            updateElements(it)
        }

        Log.i(logTag, "afterCreate, startElement: $startElement")
    }

    fun <T : Any?> MutableLiveData<MutableList<T>>.default(initialValue: MutableList<T>) =
        apply { setValue(initialValue) }

    fun updateElements(unfiltered: List<RTreeNode>) {

        val viewables = mutableListOf<RTreeNode>()
        for (element in unfiltered) {
            if (isPreViewable(element)) {
                viewables.add(element)
            }
        }
        _elements.value?.addAll(viewables)
    }


    fun setActive(stateID: StateID) {
        _currActive = stateID
    }
}
