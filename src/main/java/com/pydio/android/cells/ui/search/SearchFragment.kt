package com.pydio.android.cells.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentSearchBinding
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.browse.NodeListAdapter
import com.pydio.android.cells.ui.menus.TreeNodeMenuFragment
import com.pydio.android.cells.utils.externallyView
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SearchFragment : Fragment() {

    private val logTag = SearchFragment::class.simpleName

    private val nodeService: NodeService by inject()
    private val args: SearchFragmentArgs by navArgs()
    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private val searchVM: SearchViewModel by viewModel { parametersOf(StateID.fromId(args.state).accountId) }

    private lateinit var binding: FragmentSearchBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_search, container, false
        )
        val adapter = NodeListAdapter { node, action -> onClicked(node, action) }
        adapter.showPath()
        binding.hits.adapter = adapter

        searchVM.setQuery(args.query)

        searchVM.isLoading.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it
        }

        binding.swipeRefresh.setOnRefreshListener {
            searchVM.doQuery()
//            // Does nothing yet.
//            binding.swipeRefresh.isRefreshing = false
        }

        searchVM.hits.observe(viewLifecycleOwner) {

            if (it.isEmpty()) {
                binding.emptyContent.viewEmptyContentLayout.visibility = View.VISIBLE
                val msg = when {
                    !activeSessionVM.isServerReachable()
                    -> resources.getString(R.string.cannot_search_remote) + "\n" +
                            resources.getString(R.string.server_unreachable)
                    searchVM.isLoading.value == true
                    -> resources.getString(R.string.loading_message)
                    else
                    -> String.format(
                        resources.getString(R.string.no_result_for_search),
                        searchVM.queryString
                    )
                }
                binding.emptyContentDesc = msg
                adapter.submitList(listOf())
            } else {
                binding.emptyContent.viewEmptyContentLayout.visibility = View.GONE
                adapter.submitList(it)
            }
        }
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        searchVM.doQuery()
        val currActivity = requireActivity() as AppCompatActivity
        val bg = resources.getDrawable(R.drawable.bar_bg_search, requireActivity().theme)
        currActivity.supportActionBar?.let { bar ->
            bar.setBackgroundDrawable(bg)
            bar.title = "Searching: ${searchVM.queryString}..."
        }
    }

    override fun onPause() {
        super.onPause()
        val currActivity = requireActivity() as AppCompatActivity
        val bg = resources.getDrawable(R.drawable.empty, requireActivity().theme)
        currActivity.supportActionBar?.setBackgroundDrawable(bg)
    }

    fun updateQuery(query: String) {
        searchVM.query(query)
    }

    private fun onClicked(node: RTreeNode, command: String) {
        Log.d(logTag, "Clicked on ${node.name} -> $command")
        when (command) {
            AppNames.ACTION_OPEN -> navigateTo(node)
            AppNames.ACTION_MORE -> {
                val action = SearchFragmentDirections.openMoreMenu(
                    arrayOf(node.encodedState), TreeNodeMenuFragment.CONTEXT_SEARCH
                )
                findNavController().navigate(action)
            }
            else -> return // Unknown action, returns
        }
    }

    private fun navigateTo(node: RTreeNode) {
        if (node.isFolder()) {
            val action = MainNavDirections.openFolder(node.encodedState)
            findNavController().navigate(action)
            return
        }

        // TODO enable carousel from search page ?
//        if (isPreViewable(node)) {
//            val intent = Intent(requireActivity(), CarouselActivity::class.java)
//            intent.putExtra(AppNames.EXTRA_STATE, node.encodedState)
//            startActivity(intent)
//            Log.d(logTag, "open carousel for ${node.getStateID()}, mime type: ${node.mime}")
//            return
//        }


        lifecycleScope.launch {
            // We just retrieved results search, no need to check if the node is up to date
            nodeService.getLocalFile(node, false)?.let {
                externallyView(requireContext(), it, node)
                return@launch
            }

            if (!activeSessionVM.isServerReachable()) {
                showMessage(requireContext(), resources.getString(R.string.server_unreachable))
                return@launch
            }

            val action = MainNavDirections.launchDownload(node.encodedState, true)
            findNavController().navigate(action)
        }
    }
}
