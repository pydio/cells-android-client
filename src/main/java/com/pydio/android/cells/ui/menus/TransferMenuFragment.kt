package com.pydio.android.cells.ui.menus

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.MoreMenuTransferBinding
import com.pydio.android.cells.db.runtime.RTransfer

/**
 * Menu that presents the end user with some further actions
 * for a given transfer record.
 */
class TransferMenuFragment : BottomSheetDialogFragment() {

    private val logTag = TransferMenuFragment::class.java.simpleName
    private val args: TransferMenuFragmentArgs by navArgs()
    private val transferMenuVM: TransferMenuViewModel by viewModel { parametersOf(args.transferUid) }
    private lateinit var binding: MoreMenuTransferBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.more_menu_transfer, container, false
        )
        transferMenuVM.rTransfer.observe(viewLifecycleOwner) {
            it?.let { transfer ->
                binding.rTransfer = transfer
                binding.executePendingBindings()
            }
        }

        binding.deleteRecord.setOnClickListener {
            binding.rTransfer?.let {
                onClicked(it, AppNames.ACTION_DELETE_RECORD)
            }
        }
        binding.openParentInWorkspace.setOnClickListener {
            binding.rTransfer?.let {
                onClicked(it, AppNames.ACTION_OPEN_PARENT_IN_WORKSPACES)
            }
        }
        return binding.root
    }

    private fun onClicked(rTransfer: RTransfer, action: String) {
        Log.i(logTag, "${rTransfer.getStateId()} -> $action")
        val moreMenu = this
        lifecycleScope.launch {
            when (action) {
                // Impact remote server
                AppNames.ACTION_DELETE_RECORD -> {
                    transferMenuVM.transferService.deleteRecord(rTransfer.transferId)
                    moreMenu.dismiss()
                }
                // In-app navigation
                AppNames.ACTION_OPEN_PARENT_IN_WORKSPACES -> {
                    val parentState = StateID.fromId(rTransfer.encodedState).parentFolder()
                    CellsApp.instance.setCurrentState(parentState)
                    findNavController().navigate(MainNavDirections.openFolder(parentState.id))
                }
            }
        }
    }
}
