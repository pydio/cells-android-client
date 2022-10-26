package com.pydio.android.cells.ui.transfer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentTransferListBinding
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/** Display a list of transfers for the current active account */
class TransferListFragment : Fragment() {

    private val logTag = TransferListFragment::class.java.simpleName

    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private val transferVM: TransferViewModel by viewModel { parametersOf(activeSessionVM.accountId) }

    private lateinit var binding: FragmentTransferListBinding
    private var adapter: TransferListAdapter = TransferListAdapter(this::onClicked)
    private val observer = TransferObserver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_transfer_list, container, false)
        binding.transferList.layoutManager = LinearLayoutManager(activity)
        adapter = TransferListAdapter(this::onClicked)
        binding.transferList.adapter = adapter
        setupMenu()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        transferVM.getTransfersWithLiveFilter().observe(viewLifecycleOwner, observer)
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // TODO handle the case where activeSessionVM has no accountID
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.transfer_options, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (activeSessionVM.accountId == null){
                    Log.e(logTag, "cannot call item selected action, no session VM is active")
                }

                val accountID = StateID.fromId(activeSessionVM.accountId)
                when (menuItem.itemId) {
                    R.id.clear_table -> {
                        lifecycleScope.launch {
                            transferVM.transferService.clearTerminated(accountID)
                        }
                        return true
                    }
                    R.id.open_filter_by -> {
                        val action = MainNavDirections.openPreferenceList(
                            AppKeys.TRANSFER_FILTER_BY_STATUS,
                            AppNames.FILTER_BY_STATUS,
                            AppNames.JOB_STATUS_NO_FILTER
                        )
                        findNavController().navigate(action)
                        return true
                    }
                    R.id.open_sort_by -> {
                        val action = MainNavDirections.openPreferenceList(
                            AppKeys.TRANSFER_SORT_BY,
                            AppKeys.JOB_SORT_BY,
                            AppNames.JOB_SORT_BY_DEFAULT
                        )
                        findNavController().navigate(action)
                        return true
                    }
                    else -> return false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onClicked(node: RTransfer, command: String) {
        Log.i(logTag, "Clicked on ${node.encodedState} -> $command")
        when (command) {
            // AppNames.ACTION_OPEN -> navigateTo(node)
            AppNames.ACTION_MORE -> {
                val action = TransferListFragmentDirections.openTransferMenu(
                    node.encodedState,
                    node.transferId
                )
                findNavController().navigate(action)
            }
            else -> return // do nothing
        }
    }

    inner class TransferObserver : Observer<List<RTransfer>> {

        override fun onChanged(transfers: List<RTransfer>?) {
            transfers?.let {
                if (it.isEmpty()) {
                    var msg = resources.getString(R.string.no_transfer_for_account)
                    if (transferVM.oldFilter != AppNames.JOB_STATUS_NO_FILTER) {
                        msg += "\n " + resources.getString(R.string.filter_by_status_title)
                        msg += ": " + transferVM.oldFilter
                    }
                    binding.emptyContentDesc = msg
                    adapter.submitList(listOf())
                    binding.emptyContent.viewEmptyContentLayout.visibility = View.VISIBLE
                } else {
                    binding.emptyContent.viewEmptyContentLayout.visibility = View.GONE
                    // Log.e(logTag, "Submitting new list with ${it.size} elements")
                    // Log.d(logTag, "$it")
                    adapter.submitList(it)
                }
            }
        }
    }
}
