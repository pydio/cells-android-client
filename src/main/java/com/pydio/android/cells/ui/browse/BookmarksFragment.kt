package com.pydio.android.cells.ui.browse

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentBookmarkListBinding
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.menus.TreeNodeMenuFragment
import com.pydio.android.cells.utils.externallyView
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class BookmarksFragment : Fragment() {

    private val logTag = BookmarksFragment::class.java.simpleName

    private val prefs: CellsPreferences by inject()
    private val nodeService: NodeService by inject()

    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private val bookmarksVM: BookmarksViewModel by viewModel()

    private lateinit var binding: FragmentBookmarkListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_bookmark_list, container, false
        )
        findNavController().addOnDestinationChangedListener(ChangeListener())
        // Insure the option menu (and toolbar icons) get refreshed
        setHasOptionsMenu(true)

        return binding.root
    }

    private fun onClicked(node: RTreeNode, command: String) {
        when (command) {
            AppNames.ACTION_OPEN -> navigateTo(node)
            AppNames.ACTION_MORE -> {
                val action = BookmarksFragmentDirections.openMoreMenu(
                    arrayOf(node.encodedState),
                    TreeNodeMenuFragment.CONTEXT_BOOKMARKS
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
                bookmarksVM.afterCreate(accountID)
                configureRecyclerAdapter(bookmarksVM)
                bookmarksVM.triggerRefresh()
            }
        }
    }

    private fun configureRecyclerAdapter(viewModel: BookmarksViewModel) {
        val prefLayout = prefs.getPreference(AppNames.PREF_KEY_CURR_RECYCLER_LAYOUT)
        val asGrid = AppNames.RECYCLER_LAYOUT_GRID == prefLayout
        val adapter: ListAdapter<RTreeNode, out RecyclerView.ViewHolder?>
        if (asGrid) {
            binding.bookmarkList.layoutManager = GridLayoutManager(activity, 3)
            adapter = NodeGridAdapter { node, action -> onClicked(node, action) }
        } else {
            binding.bookmarkList.layoutManager = LinearLayoutManager(requireActivity())
            adapter = NodeListAdapter { node, action -> onClicked(node, action) }
        }
        binding.bookmarkList.adapter = adapter
        viewModel.bookmarks.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyContent.visibility = View.VISIBLE
                binding.bookmarkList.visibility = View.GONE
            } else {
                binding.bookmarkList.visibility = View.VISIBLE
                binding.emptyContent.visibility = View.GONE
                adapter.submitList(it)
            }
        }
    }

    private fun navigateTo(node: RTreeNode) {
        if (node.isFolder()) {
            val action = MainNavDirections.openFolder(node.encodedState)
            findNavController().navigate(action)
            return
        }

        // TODO enable carousel from bookmark
//        if (isPreViewable(node)) {
//            val intent = Intent(requireActivity(), CarouselActivity::class.java)
//            intent.putExtra(AppNames.EXTRA_STATE, node.encodedState)
//            startActivity(intent)
//            Log.d(logTag, "open carousel for ${node.getStateID()}, mime type: ${node.mime}")
//            return
//        }


        lifecycleScope.launch {
            nodeService.getLocalFile(node, activeSessionVM.isServerReachable())?.let {
                externallyView(requireContext(), it, node)
                return@launch
            }

            if (!activeSessionVM.isServerReachable()) {
                showMessage(
                    requireContext(),
                resources.getString(R.string.empty_cache) + "\n" +
                        resources.getString(R.string.server_unreachable)
                )
                return@launch
            }

            val action = MainNavDirections.launchDownload(node.encodedState, true)
            findNavController().navigate(action)
        }
    }

    private inner class ChangeListener : NavController.OnDestinationChangedListener {

        override fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        ) {
            Log.i(logTag, "Destination changed to ${destination.displayName}")
        }
    }
}

