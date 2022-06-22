package com.pydio.android.cells.ui.browse

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentOffineRootListBinding
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
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

    private lateinit var binding: FragmentOffineRootListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_offine_root_list, container, false
        )
        setHasOptionsMenu(true)

        return binding.root
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

    override fun onResume() {
        super.onResume()
        activeSessionVM.sessionView.observe(viewLifecycleOwner) { activeSession ->
            activeSession?.let { session ->

                val accountID = StateID.fromId(session.accountID)
                offlineVM.afterCreate(accountID)

                configureRecyclerAdapter()

                binding.forceRefresh.setOnRefreshListener {
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
                            return@setOnRefreshListener
                        }
                    }
                    offlineVM.forceRefresh()
                }
                offlineVM.isLoading.observe(viewLifecycleOwner) {
                    binding.forceRefresh.isRefreshing = it
                }

                offlineVM.runningSync.observe(viewLifecycleOwner) {
                    it?.let { runningSync ->
                        binding.syncHeader.includedHeaderContent.visibility = View.VISIBLE
                        binding.syncHeader.syncAccount = runningSync
                        if (runningSync.progress > 1 && runningSync.total > 0) {
                            binding.syncHeader.progress.isIndeterminate = false
//                            binding.syncHeader.jobName.text =
//                                resources.getText(R.string.sync_in_progress)
                            val progress = (100 * runningSync.progress / runningSync.total).toInt()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                binding.syncHeader.progress.setProgress(progress, true)
                            } else {
                                binding.syncHeader.progress.progress = progress
                            }
//                        } else {
//                            binding.syncHeader.jobName.text =
//                                resources.getText(R.string.sync_in_progress)
                        }
                        binding.syncHeader.executePendingBindings()
                    } ?: let {
                        binding.syncHeader.includedHeaderContent.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun configureRecyclerAdapter() {
        val prefLayout = prefs.getString(
            AppNames.PREF_KEY_CURR_RECYCLER_LAYOUT,
            AppNames.RECYCLER_LAYOUT_LIST
        )
        val asGrid = AppNames.RECYCLER_LAYOUT_GRID == prefLayout
        val adapter: ListAdapter<RLiveOfflineRoot, out RecyclerView.ViewHolder?>
        if (asGrid) {
            binding.offlineRootList.layoutManager = GridLayoutManager(activity, 3)
            adapter = OfflineRootsGridAdapter { node, action -> onClicked(node, action) }
        } else {
            binding.offlineRootList.layoutManager = LinearLayoutManager(requireActivity())
            adapter = OfflineRootsListAdapter { node, action -> onClicked(node, action) }
        }
        binding.offlineRootList.adapter = adapter

        offlineVM.offlineRoots.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyContent.visibility = View.VISIBLE
            } else {
                binding.emptyContent.visibility = View.GONE
            }
            adapter.submitList(it)
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
            val treeNode = nodeService.getNode(sid) ?: run {
                Log.e(logTag, "trying to open a node $sid that is unknown by the index")
                return@launch
            }
            nodeService.getLocalFile(treeNode, activeSessionVM.isServerReachable())?.let {
                externallyView(requireContext(), it, treeNode)
                return@launch
            }

            if (!activeSessionVM.isServerReachable()) {
                showMessage(
                    requireContext(),
                    resources.getString(R.string.cannot_download_file) + "\n" +
                            resources.getString(R.string.server_unreachable)
                )
                return@launch
            }

            val action = MainNavDirections.launchDownload(node.encodedState, true)
            findNavController().navigate(action)
        }
}