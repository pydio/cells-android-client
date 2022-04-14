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
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentTransferListBinding
import com.pydio.android.cells.db.runtime.RTransfer

class TransferListFragment : Fragment() {

    private val logTag = TransferListFragment::class.java.simpleName

    private val transferVM: TransferViewModel by viewModel()
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
                val action = TransferListFragmentDirections.openTransferMenu(node.transferId)
                findNavController().navigate(action)
            }
            else -> return // do nothing
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val connexionAlarmBtn = menu.findItem(R.id.clear_transfer_table)
        connexionAlarmBtn.isVisible = true

        connexionAlarmBtn.setOnMenuItemClickListener {
            lifecycleScope.launch {
                transferVM.transferService.clearTerminated()
            }
            return@setOnMenuItemClickListener true
        }
    }
}
