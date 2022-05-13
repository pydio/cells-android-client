package com.pydio.android.cells.ui.utils

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.DialogDownloadBinding
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.utils.externallyView
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File

class DownloadDialog : DialogFragment() {

    private val logTag = DownloadDialog::class.java.simpleName

    private val args: DownloadDialogArgs by navArgs()
    private val transferService: TransferService by inject()

    // private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private val downloadVM: DownloadViewModel by viewModel {
        parametersOf(args.state, args.forwardAfterDownload)
    }

    private lateinit var binding: DialogDownloadBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater, R.layout.dialog_download, container, false
        )

        downloadVM.transferId.observe(viewLifecycleOwner) {
            if (it < 1) {
                binding.transfer = null
            } else {
                binding.progress.isIndeterminate = false
                downloadVM.transfer?.observe(viewLifecycleOwner) {
                    it?.let {
                        binding.transfer = it
                    }
                }
            }
        }

        downloadVM.success.observe(viewLifecycleOwner) {
            it?.let { treeNode ->
                if (downloadVM.forwardAfterDownload) {
                    downloadVM.transfer?.value?.let { transfer ->
                        externallyView(requireContext(), File(transfer.localPath), treeNode)
                    }
                }
                dismiss()
            }
        }

        downloadVM.errorMessage.observe(viewLifecycleOwner) {
            it?.let {
                binding.loadingMessage.text = it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.loadingMessage.setTextColor(
                        resources.getColor(
                            R.color.danger,
                            requireContext().theme
                        )
                    )
                }
                binding.cancelDownloadButton.text = resources.getString(R.string.button_ok)
                isCancelable = true
            }
        }

        binding.cancelDownloadButton.setOnClickListener {
            if (downloadVM.isProcessing) {
                downloadVM.cancelDownload()
            } else {
                dismiss()
            }
        }

        return binding.root
    }

}
