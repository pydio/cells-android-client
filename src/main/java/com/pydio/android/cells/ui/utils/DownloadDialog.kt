package com.pydio.android.cells.ui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.DialogDownloadBinding
import com.pydio.android.cells.reactive.NetworkStatus
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.utils.externallyView
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.utils.Log
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.io.File

class DownloadDialog : DialogFragment(), KoinComponent {

    private val logTag = DownloadDialog::class.java.simpleName

    private val prefs: CellsPreferences by inject()
    private val networkService: NetworkService by inject()

    private val limit =
        prefs.getString(AppKeys.METERED_ASK_B4_DL_FILES_SIZE, "2").toInt() * 1024 * 1024

    private val args: DownloadDialogArgs by navArgs()

    private val downloadVM: DownloadViewModel by viewModel {
        parametersOf(args.state, args.forwardAfterDownload)
    }

    private lateinit var binding: DialogDownloadBinding

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater, R.layout.dialog_download, container, false
        )

        downloadVM.transferId.observe(viewLifecycleOwner) { transferId ->
            if (transferId < 1) {
                binding.transfer = null
            } else {
                binding.progress.isIndeterminate = false
                downloadVM.transfer?.observe(viewLifecycleOwner) {
                    it?.let {
                        val sizeValue = Formatter.formatShortFileSize(this.context, it.byteSize)
                        binding.loadingMessage.text =
                            "$sizeValue - ${resources.getString(R.string.download_wait_message)}"
                        binding.transfer = it
                        val newValue = it.progress * 100 / it.byteSize
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            binding.progress.setProgress(newValue.toInt(), true)
                        } else {
                            binding.progress.progress = newValue.toInt()
                        }
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
                // Sometimes we get an error before starting the effective download
                // -> we do not want the indeterminate progress when we are in error
                binding.progress.isIndeterminate = false

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

    override fun onResume() {
        super.onResume()
        when (networkService.networkStatus) {
            is NetworkStatus.Unavailable,
            is NetworkStatus.Unknown,
            is NetworkStatus.Roaming,
            -> {
                showMessage(requireContext(), "Cannot download file with no connection")
                dismiss()
            }
            is NetworkStatus.Metered
            -> {
                val applyLimit = prefs.getBoolean(AppKeys.APPLY_METERED_LIMITATION, true)
                val askIfGreaterThan = prefs.getBoolean(AppKeys.METERED_ASK_B4_DL_FILES, true)
                val bigEnough = askIfGreaterThan && (args.size > limit)

                Log.e(logTag, "limit: $limit, args.size: ${args.size}")
                Log.e(
                    logTag,
                    "applyLimit: $applyLimit, askIfGreaterThan: $askIfGreaterThan, bigEnough: $bigEnough"
                )

                if (applyLimit && bigEnough) {
                    confirmDownload(requireContext())
                } else {
                    downloadVM.launchDownload()
                }
            }
            else -> downloadVM.launchDownload()
        }
    }

    private fun confirmDownload(
        context: Context
    ): Boolean {
        val humanFriendlySize = Formatter.formatShortFileSize(context, args.size)
        MaterialAlertDialogBuilder(context)
            .setTitle(context.resources.getString(R.string.confirm_download_title))
            .setMessage(
                context.resources.getString(
                    R.string.confirm_download_desc,
                    humanFriendlySize
                )
            )
            .setPositiveButton(R.string.button_ok) { _, _ ->
                Log.e(logTag, "About to launch download")
                downloadVM.launchDownload()
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                dismiss()
            }
            .show()
        return true
    }
}
