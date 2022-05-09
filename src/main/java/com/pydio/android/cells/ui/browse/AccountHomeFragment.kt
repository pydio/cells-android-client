package com.pydio.android.cells.ui.browse

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.pydio.cells.transport.StateID
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentAccountHomeBinding
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.utils.showLongMessage

/**
 * Displays the landing page for a given account, mainly the workspace list.
 * This is the default entry point for the MainActivity.
 */
class AccountHomeFragment : Fragment() {

    private val logTag = AccountHomeFragment::class.simpleName
    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private lateinit var binding: FragmentAccountHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(logTag, "onCreateView ${activeSessionVM.accountId}")

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_account_home, container, false
        )

        binding.forceRefresh.setOnRefreshListener { activeSessionVM.forceRefresh() }

        binding.switchAccountButton.setOnClickListener {
            findNavController().navigate(MainNavDirections.openAccountList())
        }

        activeSessionVM.isLoading.observe(viewLifecycleOwner) {
            binding.forceRefresh.isRefreshing = it
        }
        activeSessionVM.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { showLongMessage(requireContext(), msg) }
        }

        return binding.root
    }

    override fun onResume() {
        Log.d(logTag, "onResume: ${activeSessionVM.accountId}")
        super.onResume()

        activeSessionVM.workspaces.observe(viewLifecycleOwner) {
            it?.let {
                val adapter = WorkspaceListAdapter { slug, action -> onWsClicked(slug, action) }

                binding.workspaces.adapter = adapter
                val columns = resources.getInteger(R.integer.grid_ws_column_number)
                val manager = GridLayoutManager(requireContext(), columns)
                manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int) =
                        when (adapter.getItemViewType(position)) {
                            AppNames.ITEM_TYPE_HEADER -> columns
                            else -> 1
                        }
                }
                binding.workspaces.layoutManager = manager

                if (it.isEmpty()) {
                    binding.emptyContent.visibility = View.VISIBLE
                    binding.workspaces.visibility = View.GONE
                } else {
                    binding.workspaces.visibility = View.VISIBLE
                    binding.emptyContent.visibility = View.GONE
                    adapter.addHeaderAndSubmitList(it)
                }
            }
        }

        activeSessionVM.liveSession.observe(viewLifecycleOwner) {
            it?.let { liveSession ->
                (requireActivity() as AppCompatActivity).supportActionBar?.let { bar ->
                    bar.title = liveSession.serverLabel ?: liveSession.url
                }

                binding.session = liveSession
            }
        }

        activeSessionVM.resume()
    }

    private fun onWsClicked(slug: String, command: String) {
        val activeSession = activeSessionVM.liveSession.value ?: return
        when (command) {
            AppNames.ACTION_OPEN -> {
                val targetState = StateID.fromId(activeSession.accountID).withPath("/${slug}")
                findNavController().navigate(MainNavDirections.openFolder(targetState.id))
            }
            else -> return // do nothing
        }
    }

    override fun onPause() {
        val idStr = activeSessionVM.liveSession.value?.accountID ?: "No active session"
        Log.d(logTag, "Pausing: $idStr")
        super.onPause()
        activeSessionVM.pause()
    }
}
