package com.pydio.android.cells.ui.browse

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
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentOffineRootListBinding
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.menus.TreeNodeMenuFragment
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class OfflineRootsFragment : Fragment() {

    private val logTag = OfflineRootsFragment::class.java.simpleName

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

                binding.forceRefresh.setOnRefreshListener { offlineVM.forceRefresh() }
                offlineVM.isLoading.observe(viewLifecycleOwner) {
                    binding.forceRefresh.isRefreshing = it
                }
            }
        }
    }

    private fun configureRecyclerAdapter() {
        val prefLayout = CellsApp.instance.getPreference(AppNames.PREF_KEY_CURR_RECYCLER_LAYOUT)
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
                val action = MainNavDirections.openFolder(node.encodedState)
                findNavController().navigate(action)
            } else {

                // FIXME implement file viewing
                Log.i(logTag, "OPEN: ${node.encodedState}")

//                val file = CellsApp.instance.nodeService.getOrDownloadFileToCache(node)
//                file?.let {
//                    val intent = externallyView(requireContext(), file, node)
//                    try {
//                        startActivity(intent)
//                        val msg = "Opened ${it.name} (${intent.type}) with external viewer"
//                        Log.e(tag, "Intent success: $msg")
//                    } catch (e: Exception) {
//                        val msg = "Cannot open ${it.name} (${intent.type}) with external viewer"
//                        Toast.makeText(requireActivity().application, msg, Toast.LENGTH_LONG).show()
//                        Log.e(tag, "Call to intent failed: $msg")
//                        e.printStackTrace()
//                    }
//                }
            }
        }
}