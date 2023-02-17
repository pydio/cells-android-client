package com.pydio.android.cells.ui.browsexml

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentOfflineRootListBinding
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.menus.TreeNodeMenuFragment
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.externallyView
import com.pydio.android.cells.utils.showLongMessage
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class OfflineRootsFragment : Fragment() {

    private val logTag = OfflineRootsFragment::class.java.simpleName

    private val prefs: CellsPreferences by inject()
    private val nodeService: NodeService by inject()

    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private val offlineVM: OfflineRootsViewModel by viewModel()

    private lateinit var binding: FragmentOfflineRootListBinding

    private var adapter: ListAdapter<RLiveOfflineRoot, out RecyclerView.ViewHolder?>? = null
    private val observer = OfflineRootObserver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_offline_root_list, container, false
        )

        offlineVM.isLoading.observe(viewLifecycleOwner) {
            binding.forceRefresh.isRefreshing = it
        }
        offlineVM.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { showLongMessage(requireContext(), msg) }
        }

        binding.forceRefresh.setOnRefreshListener { doLaunchRefresh() }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activeSessionVM.sessionView.observe(viewLifecycleOwner) { activeSession ->
            activeSession?.let { session ->
                val accountID = StateID.fromId(session.accountID)
                offlineVM.afterCreate(accountID)
                preconfigureAdapter()

                // TODO this should not be there (a.k.a adding an observer in an observed prop)
                offlineVM.runningSync.observe(viewLifecycleOwner) {
                    if (it != null) {
                        prepareSyncHeader(it)
                        binding.syncHeader.includedHeaderContent.visibility = View.VISIBLE
                    } else {
                        binding.syncHeader.includedHeaderContent.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun preconfigureAdapter() {

        val liveSharedPreferences = LiveSharedPreferences(prefs.get())
        liveSharedPreferences
            .getString(AppKeys.CURR_RECYCLER_LAYOUT, AppNames.RECYCLER_LAYOUT_LIST)
            .observe(viewLifecycleOwner) {
                it?.let {
                    configureRecyclerAdapter(it)
                    if (offlineVM.offlineRoots.value != null && (offlineVM.offlineRoots.value as List<RLiveOfflineRoot>).isNotEmpty()) {
                        adapter?.submitList(offlineVM.offlineRoots.value as List<RLiveOfflineRoot>)
                    }
                }
            }

        liveSharedPreferences
            .getString(AppKeys.CURR_RECYCLER_ORDER, AppNames.DEFAULT_SORT_ENCODED)
            .observe(viewLifecycleOwner) {
                it?.let {
                    if (offlineVM.orderHasChanged(it)) {
                        offlineVM.offlineRoots.removeObserver(observer)
                        offlineVM.reQuery(it)
                        offlineVM.offlineRoots.observe(viewLifecycleOwner, observer)
                    }
                }
            }
    }

    private fun configureRecyclerAdapter(listLayout: String) {
        when (listLayout) {
            AppNames.RECYCLER_LAYOUT_GRID -> {
                val cols = resources.getInteger(R.integer.grid_default_column_number)
                binding.offlineRootList.layoutManager = GridLayoutManager(activity, cols)
                adapter = OfflineRootsGridAdapter { node, action -> onClicked(node, action) }
            }
            AppNames.RECYCLER_LAYOUT_LIST -> {
                binding.offlineRootList.layoutManager = LinearLayoutManager(requireActivity())
                adapter = OfflineRootsListAdapter { node, action -> onClicked(node, action) }
            }
        }
        binding.offlineRootList.adapter = adapter
        offlineVM.offlineRoots.observe(viewLifecycleOwner, observer)
    }

    private fun prepareSyncHeader(runningSync: RJob) {
        binding.syncHeader.syncAccount = runningSync
        if (runningSync.progress > 1 && runningSync.total > 0) {
            binding.syncHeader.progress.isIndeterminate = false
            val progress = (100 * runningSync.progress / runningSync.total).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.syncHeader.progress.setProgress(progress, true)
            } else {
                binding.syncHeader.progress.progress = progress
            }
        }
        binding.syncHeader.executePendingBindings()
    }

    private fun doLaunchRefresh() {
        // TODO avoid invalid state access to late init var
        offlineVM.runningSync.value?.let {
            val timeSinceStart = currentTimestamp() - it.startTimestamp
            val timeSinceUpdate = currentTimestamp() - it.updateTimestamp
            if (timeSinceStart < 300 && timeSinceUpdate < 120) {
                val m = "start: ${timeSinceStart}s ago, update: ${timeSinceUpdate}s ago"
                Log.e(logTag, m)
                showLongMessage(
                    requireContext(),
                    resources.getString(R.string.sync_already_running)
                )
                offlineVM.setLoading(false)
                return
            }
        }
        offlineVM.forceRefresh()
    }

    private fun onClicked(node: RLiveOfflineRoot, command: String) {
        when (command) {
            AppNames.ACTION_OPEN -> navigateTo(node)
            AppNames.ACTION_MORE -> {
                val action = OfflineRootsFragmentDirections.openMoreMenu(
                    arrayOf(node.encodedState),
                    TreeNodeMenuFragment.CONTEXT_OFFLINE
                )
                findNavController().navigate(action)
            }
            else -> return // do nothing
        }
    }

    private fun navigateTo(node: RLiveOfflineRoot) =
        lifecycleScope.launch {
            if (node.isFolder()) {
                findNavController().navigate(MainNavDirections.openFolder(node.encodedState))
                return@launch
            }

            val sid = node.getStateID()
            Log.d(logTag, "About to navigate to $sid, mime type: ${node.mime}")
            // TODO implement opening the carousel from here
            val treeNode = nodeService.getLocalNode(sid) ?: run {
                Log.e(logTag, "trying to open a node $sid that is unknown by the index")
                return@launch
            }
            nodeService.getLocalFile(treeNode, activeSessionVM.canDownloadFiles())?.let {
                externallyView(requireContext(), it, treeNode)
                return@launch
            }

            if (!activeSessionVM.canListMeta()) {
                showMessage(
                    requireContext(),
                    resources.getString(R.string.cannot_download_file) + "\n" +
                            resources.getString(R.string.server_unreachable)
                )
                return@launch
            }
            val action = MainNavDirections.launchDownload(node.encodedState, node.size, true)
            findNavController().navigate(action)
        }

    private fun wrapSubmit(roots: List<RLiveOfflineRoot>) {
        if (roots.isEmpty()) {
            val msg = resources.getString(R.string.no_offline_root_for_account)
            binding.emptyContentDesc = msg
            adapter?.submitList(listOf())
            binding.emptyContent.viewEmptyContentLayout.visibility = View.VISIBLE
        } else {
            Log.e(logTag, "Submitting new list with ${roots.size} elements")
            Log.d(logTag, "$roots")
            adapter?.submitList(roots)
            binding.emptyContent.viewEmptyContentLayout.visibility = View.GONE
        }
    }

    inner class OfflineRootObserver : Observer<List<RLiveOfflineRoot>> {

        override fun onChanged(it: List<RLiveOfflineRoot>) {
            it?.let {
                wrapSubmit(it)
            }
        }
    }
}
