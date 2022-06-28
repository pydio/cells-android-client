package com.pydio.android.cells.ui.browse

import android.os.Bundle
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
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentBookmarkListBinding
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.menus.TreeNodeMenuFragment
import com.pydio.android.cells.utils.externallyView
import com.pydio.android.cells.utils.showLongMessage
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Displays a list of all bookmarks for current account in a recycler view.
 * Note that we query the server to update the index with not yet cached nodes that are bookmarked.
 */
class BookmarksFragment : Fragment() {

    private val logTag = BookmarksFragment::class.java.simpleName

    private val prefs: CellsPreferences by inject()
    private val nodeService: NodeService by inject()

    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private val bookmarksVM: BookmarksViewModel by viewModel()

    private var adapter: ListAdapter<RTreeNode, out RecyclerView.ViewHolder?>? = null
    private val observer = BookmarkObserver()

    private lateinit var binding: FragmentBookmarkListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_bookmark_list, container, false
        )

        binding.forceRefresh.setOnRefreshListener { bookmarksVM.triggerRefresh() }

        bookmarksVM.isLoading.observe(viewLifecycleOwner) {
            binding.forceRefresh.isRefreshing = it
        }
        bookmarksVM.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { showLongMessage(requireContext(), msg) }
        }

        // findNavController().addOnDestinationChangedListener(ChangeListener())
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
                val currOrder = prefs.getString(
                    AppNames.PREF_KEY_CURR_RECYCLER_ORDER,
                    AppNames.DEFAULT_SORT_ENCODED
                )
                bookmarksVM.afterCreate(accountID, currOrder)
                bookmarksVM.triggerRefresh()
                preconfigureAdapter(accountID)
            }
        }
    }

    private fun preconfigureAdapter(accountID: StateID) {

        var liveSharedPreferences: LiveSharedPreferences? = null
        bookmarksVM.bookmarks.observe(viewLifecycleOwner) {
            if (liveSharedPreferences != null) {
                return@observe
            }
            liveSharedPreferences = LiveSharedPreferences(prefs.get())
            liveSharedPreferences!!
                .getString(AppNames.PREF_KEY_CURR_RECYCLER_LAYOUT, AppNames.RECYCLER_LAYOUT_LIST)
                .observe(viewLifecycleOwner) {
//                    val prefLayout = prefs.getPreference(
//                        AppNames.PREF_KEY_CURR_RECYCLER_LAYOUT,
//                        AppNames.RECYCLER_LAYOUT_LIST
//                    )

                    it?.let {
                        configureRecyclerAdapter(it)
                        bookmarksVM.bookmarks.removeObserver(observer)
                        bookmarksVM.bookmarks.observe(viewLifecycleOwner, observer)
                    }
                }

            liveSharedPreferences!!
                .getString(
                    AppNames.PREF_KEY_CURR_RECYCLER_ORDER, AppNames.DEFAULT_SORT_ENCODED
                )
                .observe(viewLifecycleOwner) {
                    it?.let {
                        if (bookmarksVM.orderHasChanged(it)) {
                            bookmarksVM.bookmarks.removeObserver(observer)
                            bookmarksVM.afterCreate(accountID, it)
                            bookmarksVM.bookmarks.observe(viewLifecycleOwner, observer)
                        }
                    }
                }
        }
    }

    private fun configureRecyclerAdapter(listLayout: String) {

        when (listLayout) {
            AppNames.RECYCLER_LAYOUT_GRID -> {
                val columns = resources.getInteger(R.integer.grid_default_column_number)
                binding.bookmarkList.layoutManager = GridLayoutManager(activity, columns)
                adapter = NodeGridAdapter { node, action -> onClicked(node, action) }
            }
            AppNames.RECYCLER_LAYOUT_LIST -> {
                binding.bookmarkList.layoutManager = LinearLayoutManager(requireActivity())
                adapter = NodeListAdapter { node, action -> onClicked(node, action) }
            }
        }

        binding.bookmarkList.adapter = adapter
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
            nodeService.getLocalFile(node, activeSessionVM.canDownloadFiles())?.let {
                externallyView(requireContext(), it, node)
                return@launch
            }

            if (!activeSessionVM.canListMeta()) {
                showMessage(
                    requireContext(),
                    resources.getString(R.string.cannot_download_file) + "\n" +
                            resources.getString(R.string.server_unreachable)
                )
                return@launch
            } else if (!activeSessionVM.canDownloadFiles()) {
                showMessage(
                    requireContext(),
                    resources.getString(R.string.no_download_on_metered)
                )
                return@launch
            }

            val action = MainNavDirections.launchDownload(node.encodedState, true)
            findNavController().navigate(action)
        }
    }

    inner class BookmarkObserver : Observer<List<RTreeNode>> {

        override fun onChanged(it: List<RTreeNode>?) {
            it?.let {
                if (it.isEmpty()) {
                    val msg = when {
                        !activeSessionVM.canListMeta()
                        -> resources.getString(R.string.empty_cache) + "\n" +
                                resources.getString(R.string.server_unreachable)
                        bookmarksVM.isLoading.value == true
                        -> resources.getString(R.string.loading_message)
                        else
                        -> resources.getString(R.string.no_bookmark_for_account)
                    }
                    binding.emptyContentDesc = msg
                    adapter?.submitList(listOf())
                    binding.emptyContent.viewEmptyContentLayout.visibility = View.VISIBLE

                } else {
                    binding.emptyContent.viewEmptyContentLayout.visibility = View.GONE
                    Log.e(logTag, "Submitting new list with ${it.size} elements")
                    Log.d(logTag, "${it}")
                    adapter?.submitList(it)
                }
            }
        }
    }
}
