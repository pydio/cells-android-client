package com.pydio.android.cells.ui.transfer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentTransferListBinding
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TransferListFragment : Fragment() {

    private val logTag = TransferListFragment::class.java.simpleName

    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private val transferVM: TransferViewModel by viewModel { parametersOf(activeSessionVM.accountId) }
    private lateinit var binding: FragmentTransferListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_transfer_list, container, false
        )

        binding.transferList.layoutManager = LinearLayoutManager(activity)
        val adapter = TransferListAdapter(this::onClicked)
        binding.transferList.adapter = adapter
        transferVM.transfers.observe(viewLifecycleOwner) { adapter.submitList(it) }

        return binding.root
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        activeSessionVM.accountId?.let { accId ->
            val clearListBtn = menu.findItem(R.id.clear_transfer_table)
            clearListBtn.isVisible = true
            clearListBtn.setOnMenuItemClickListener {
                lifecycleScope.launch {
                    transferVM.transferService.clearTerminated(StateID.fromId(accId))
                }
                return@setOnMenuItemClickListener true
            }
        }
    }
}
