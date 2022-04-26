package com.pydio.android.cells.ui.browse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CarouselActivity
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentBrowseFolderBinding
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.menus.TreeNodeMenuFragment
import com.pydio.android.cells.ui.utils.LoadingDialogFragment
import com.pydio.android.cells.utils.BackStackAdapter
import com.pydio.android.cells.utils.dumpBackStack
import com.pydio.android.cells.utils.externallyView
import com.pydio.android.cells.utils.isPreViewable
import com.pydio.android.cells.utils.resetToHomeStateIfNecessary
import com.pydio.android.cells.utils.showLongMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * Main fragment when browsing a given account. It displays the whole content
 * of a workspaces or of one of its child folder, providing following features:
 * - action on a given node
 * - multi selection
 * - navigation to another folder, an external viewer for a node
 *   or a carousel to display supported files in the current folder.
 */
class BrowseFolderFragment : Fragment() {

    private val logTag = BrowseFolderFragment::class.java.simpleName

    private val nodeService: NodeService by inject()

    private val args: BrowseFolderFragmentArgs by navArgs()
    private val browseFolderVM: BrowseFolderViewModel by viewModel { parametersOf(args.state) }
    private lateinit var binding: FragmentBrowseFolderBinding

    // Temp solution to provide a scrim during long running operations
    private var loadingDialog: LoadingDialogFragment? = null

    private var mode: ActionMode? = null
    private var actionModeCallback: PrimaryActionModeCallback? = null
    private var tracker: SelectionTracker<String>? = null

    private lateinit var adapter: ListAdapter<RTreeNode, out RecyclerView.ViewHolder?>
    private val observer = ChildObserver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_browse_folder, container, false
        )

//        browseFolderVM.afterCreate(StateID.fromId(args.state))

        configureRecyclerAdapter()

        val backPressedCallback = BackStackAdapter.initialised(
            parentFragmentManager,
            findNavController(),
            StateID.fromId(args.state)
        )
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )

        setHasOptionsMenu(true)

        browseFolderVM.currentFolder.observe(viewLifecycleOwner) {
            it?.let {
                if (it.isRecycle() || it.isInRecycle()) {
                    binding.addNodeFab.visibility = View.GONE
                } else {
                    binding.addNodeFab.visibility = View.VISIBLE
//                    binding.addNodeFab.setOnClickListener { onFabClicked() }
                }
            }
        }
        // TODO workspace root is not a RTreeNode => we must handle it explicitly.
        if (browseFolderVM.stateId.isWorkspaceRoot) {
            binding.addNodeFab.visibility = View.VISIBLE
        }
        // Put this also in observer when the above has been fixed
        binding.addNodeFab.setOnClickListener { onFabClicked() }

        binding.forceRefresh.setOnRefreshListener { browseFolderVM.forceRefresh() }

        browseFolderVM.isLoading.observe(viewLifecycleOwner) {
            binding.forceRefresh.isRefreshing = it
        }
        browseFolderVM.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { showLongMessage(requireContext(), msg) }
        }
        return binding.root
    }

    private fun configureRecyclerAdapter() {

        // Manage grid or linear layouts
        val prefLayout = CellsApp.instance.getPreference(AppNames.PREF_KEY_CURR_RECYCLER_LAYOUT)
        val asGrid = AppNames.RECYCLER_LAYOUT_GRID == prefLayout
        val trackerBuilder: SelectionTracker.Builder<String>?
        if (asGrid) {
            val columns = resources.getInteger(R.integer.grid_default_column_number)
            binding.nodes.layoutManager = GridLayoutManager(requireActivity(), columns)
            adapter = NodeGridAdapter { node, action -> onClicked(node, action) }
            binding.nodes.adapter = adapter
            trackerBuilder = SelectionTracker.Builder(
                "grid_multi_selection",
                binding.nodes,
                NodeGridItemKeyProvider(adapter as NodeGridAdapter),
                NodeGridItemDetailsLookup(binding.nodes),
                StorageStrategy.createStringStorage()
            )
        } else {
            binding.nodes.layoutManager = LinearLayoutManager(requireActivity())
            adapter = NodeListAdapter { node, action -> onClicked(node, action) }
            binding.nodes.adapter = adapter
            trackerBuilder = SelectionTracker.Builder(
                "list_multi_selection",
                binding.nodes,
                NodeListItemKeyProvider(adapter as NodeListAdapter),
                NodeListItemDetailsLookup(binding.nodes),
                StorageStrategy.createStringStorage()
            )
        }

        // Manage multi selection
        trackerBuilder.let { builder ->
            val tmpTracker = builder.withSelectionPredicate(
                SelectionPredicates.createSelectAnything()
            ).build()
            if (adapter is NodeListAdapter) {
                (adapter as NodeListAdapter).tracker = tmpTracker
            } else if (adapter is NodeGridAdapter) {
                (adapter as NodeGridAdapter).tracker = tmpTracker
            }
            tmpTracker.addObserver(
                object : SelectionTracker.SelectionObserver<String>() {

                    override fun onItemStateChanged(key: String, selected: Boolean) {
                        super.onItemStateChanged(key, selected)
                        Log.d(logTag, "onItemStateChanged for $key, selected: $selected")
                    }

                    override fun onSelectionChanged() {
                        super.onSelectionChanged()
                        val itemNb = tracker?.selection?.size() ?: 0
                        if (mode == null && itemNb > 0 && tracker != null) {
                            // browseFolderVM.setSelection(tracker!!.selection)
                            actionModeCallback = PrimaryActionModeCallback()
                            actionModeCallback!!.startActionMode(
                                requireView(),
                                R.menu.main_multi_select_menu,
                                object : OnActionItemClickListener {
                                    override fun onActionItemClick(item: MenuItem): Boolean {
                                        Log.e(logTag, "onActionItemClick for: ${item.title}")

                                        val selected = tracker?.selection?.map { it } ?: return true

                                        // Log.e(fTag, "... Listing selected:")
                                        // for (curr in selected){
                                        //     Log.e(fTag, "- $curr")
                                        // }

                                        val action = BrowseFolderFragmentDirections
                                            .openMoreMenu(
                                                selected.toTypedArray(),
                                                TreeNodeMenuFragment.CONTEXT_BROWSE
                                            )
                                        findNavController().navigate(action)
                                        return true
                                    }
                                })
                        } else if (itemNb > 0) {
                            mode?.title = String.format(
                                resources.getQuantityString(
                                    R.plurals.selected_count,
                                    itemNb
                                ), itemNb
                            )
                        } else {
                            actionModeCallback?.finishActionMode()
                            actionModeCallback = null
                            mode = null
                        }
                    }
                })
            tracker = tmpTracker
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.e(logTag, "onSaveInstanceState for: ${browseFolderVM.stateId}")
        outState.putString(AppNames.EXTRA_STATE, browseFolderVM.stateId.id)
    }

    override fun onResume() {
        Log.d(logTag, "onResume")
        super.onResume()
        dumpBackStack(logTag, parentFragmentManager)

        // We must insure the Observed LiveData has been correctly updated
        // Otherwise we won't see sort order changes directly0
        browseFolderVM.resume()
        observer.let {
            browseFolderVM.children.removeObserver(it)
        }
        browseFolderVM.children.observe(viewLifecycleOwner, observer)

        (requireActivity() as AppCompatActivity).supportActionBar?.let { bar ->
            bar.setDisplayHomeAsUpEnabled(true)
            browseFolderVM.currentFolder.observe(viewLifecycleOwner) {
                it?.let {
                    bar.title = when {
                        it.isRecycle() -> resources.getString(R.string.recycle_bin_label)
                        else -> it.name
                    }
                } ?: run {
                    Log.e(logTag, "could not set app bar title, no node is defined")
                    Log.e(logTag, "Corresponding state: ${browseFolderVM.stateId}")
                }
            }
        }
    }

    override fun onPause() {
        Log.i(logTag, "onPause")
        browseFolderVM.pause()
        super.onPause()
    }

    override fun onDetach() {
        Log.i(logTag, "onDetach")
        super.onDetach()
        resetToHomeStateIfNecessary(parentFragmentManager, browseFolderVM.stateId)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        Log.i(logTag, "onViewStateRestored")
        super.onViewStateRestored(savedInstanceState)
    }

    private fun onClicked(node: RTreeNode, command: String) {
        Log.i(logTag, "Clicked on ${browseFolderVM.stateId} -> $command")
        when (command) {
            AppNames.ACTION_OPEN -> navigateTo(node)
            AppNames.ACTION_MORE -> {
                val action = BrowseFolderFragmentDirections
                    .openMoreMenu(
                        arrayOf(node.encodedState),
                        if (node.isInRecycle() || node.isRecycle()) {
                            TreeNodeMenuFragment.CONTEXT_RECYCLE
                        } else {
                            TreeNodeMenuFragment.CONTEXT_BROWSE
                        }
                    )
                findNavController().navigate(action)
            }
            else -> return // Unknown action, log warning and returns
        }
    }

    private fun onFabClicked() {
        val action = BrowseFolderFragmentDirections.openMoreMenu(
            arrayOf(browseFolderVM.stateId.id),
            TreeNodeMenuFragment.CONTEXT_ADD
        )
        findNavController().navigate(action)
    }

    private fun navigateTo(node: RTreeNode) = lifecycleScope.launch {
        if (node.isFolder()) {
            CellsApp.instance.setCurrentState(StateID.fromId(node.encodedState))
            findNavController().navigate(MainNavDirections.openFolder(node.encodedState))
            return@launch
        }

        Log.i(logTag, "About to navigate to ${node.getStateID()}, mime type: ${node.mime}")

        if (isPreViewable(node)) {
//        if (node.mime.startsWith("image") || node.mime.startsWith("\"image")) {
            val intent = Intent(requireActivity(), CarouselActivity::class.java)
            intent.putExtra(AppNames.EXTRA_STATE, node.encodedState)
            startActivity(intent)
            Log.d(logTag, "open carousel for ${node.getStateID()}, mime type: ${node.mime}")
            return@launch
        }

        // TODO double check. It smells.
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        browseFolderVM.setLoading(true)
        showProgressDialog()

        val file = nodeService.getOrDownloadFileToCache(node)

        browseFolderVM.setLoading(false)
        requireActivity().window.setFlags(
            0,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        file?.let {
            externallyView(requireContext(), file, node)
            loadingDialog?.dismiss()
        }
    }

    private fun showProgressDialog() {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        loadingDialog = LoadingDialogFragment.newInstance()
        loadingDialog?.show(fm, "fragment_edit_name")
    }

    override fun onAttach(context: Context) {
        Log.i(logTag, "onAttach")
        super.onAttach(context)
    }

    inner class PrimaryActionModeCallback : ActionMode.Callback {

        private var onActionItemClickListener: OnActionItemClickListener? = null

        @MenuRes
        private var menuResId: Int = 0

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            Log.i(tag, "onCreateActionMode")
            this@BrowseFolderFragment.mode = mode
            mode.menuInflater.inflate(menuResId, menu)
            // TODO optimistic: we rely on the fact that the action mode is always opened with a single selected element.
            mode.title = String.format(resources.getQuantityString(R.plurals.selected_count, 1), 1)
            binding.addNodeFab.visibility = View.GONE
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Log.i(tag, "onPrepareActionMode")
            // TODO we must probably redraw the list at this point to show
            //   a more explicit "selection" layout.
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            // Clear selection
            tracker?.clearSelection()

            // Re-show the FAB if necessary
            val inRecycle = browseFolderVM.currentFolder.value?.let {
                it.isRecycle() || it.isInRecycle()
            } ?: false
            if (!inRecycle) {
                binding.addNodeFab.visibility = View.VISIBLE
            }

            this@BrowseFolderFragment.mode?.finish()
            this@BrowseFolderFragment.mode = null
            actionModeCallback = null

            Log.i(tag, "onDestroyActionMode")
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            onActionItemClickListener?.onActionItemClick(item)
            Log.i(tag, "onActionItemClicked")
            mode.finish()
            return true
        }

        fun startActionMode(
            view: View,
            @MenuRes menuResId: Int,
            listener: OnActionItemClickListener
        ) {
            this.menuResId = menuResId
            view.startActionMode(this)
            this.onActionItemClickListener = listener
        }

        fun finishActionMode() {
            Log.i(tag, "finishActionMode")
            mode?.finish()
        }
    }

    inner class ChildObserver : Observer<List<RTreeNode>> {
        override fun onChanged(it: List<RTreeNode>?) {
            it?.let {
                if (it.isEmpty()) {
                    binding.emptyContent.visibility = View.VISIBLE
                    adapter.submitList(listOf())
                } else {
                    binding.emptyContent.visibility = View.GONE
                    adapter.submitList(it)
                }
            }
        }
    }
}

interface OnActionItemClickListener {
    fun onActionItemClick(item: MenuItem): Boolean
}
