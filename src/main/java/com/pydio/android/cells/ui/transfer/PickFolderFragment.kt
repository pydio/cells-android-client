package com.pydio.android.cells.ui.transfer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.UploadNavigationDirections
import com.pydio.android.cells.databinding.FragmentPickFolderBinding
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.tasks.createFolder
import com.pydio.android.cells.utils.showLongMessage
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class PickFolderFragment : Fragment() {

    private val logTag = PickFolderFragment::class.java.simpleName

    private lateinit var binding: FragmentPickFolderBinding

    private val nodeService: NodeService by inject()
    private val pickFolderVM: PickFolderViewModel by viewModel()
    private val chooseTargetVM: ChooseTargetViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_pick_folder, container, false
        )
        setHasOptionsMenu(true)

        val stateID = if (savedInstanceState?.getString(AppKeys.EXTRA_STATE) != null) {
            val encodedState = savedInstanceState.getString(AppKeys.EXTRA_STATE)
            StateID.fromId(encodedState)
        } else {
            val args: PickFolderFragmentArgs by navArgs()
            StateID.fromId(args.state)
        }

        pickFolderVM.afterCreate(stateID)

        val adapter = FolderListAdapter(stateID, chooseTargetVM.actionContext) { state, action ->
            onClicked(state, action)
        }

        if (Str.empty(stateID.workspace)) {
            binding.addNodeFab.visibility = View.GONE
        } else {
            binding.addNodeFab.visibility = View.VISIBLE
            binding.addNodeFab.setOnClickListener { onFabClicked() }
        }

        // Wire swipe to refresh
        binding.pickFolderForceRefresh.setOnRefreshListener {
            if (Str.empty(stateID.workspace)) { // Does nothing for the time being
                binding.pickFolderForceRefresh.isRefreshing = false
            } else {
                pickFolderVM.forceRefresh()
            }
        }

        pickFolderVM.isLoading.observe(viewLifecycleOwner) {
            binding.pickFolderForceRefresh.isRefreshing = it
        }

        pickFolderVM.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { showLongMessage(requireContext(), msg) }
        }

        binding.folders.adapter = adapter

        pickFolderVM.children.observe(viewLifecycleOwner) { adapter.addHeaderAndSubmitList(it) }
        return binding.root
    }

    private fun onClicked(stateID: StateID, command: String) {
        when (command) {
            AppNames.ACTION_OPEN -> {
                val action = if (stateID.id == AppNames.CELLS_ROOT_ENCODED_STATE) {
                    UploadNavigationDirections.actionPickSession()
                } else {
                    UploadNavigationDirections.actionPickFolder(stateID.id)
                }
                findNavController().navigate(action)
            }
            else -> return // do nothing
        }
    }

    private fun onFabClicked() {
        createFolder(requireContext(), pickFolderVM.stateID, nodeService)
    }

    override fun onResume() {
        Log.d(logTag, "onResume: ${pickFolderVM.stateID}")
        super.onResume()
        chooseTargetVM.setCurrentState(pickFolderVM.stateID)
        pickFolderVM.resume()

        (requireActivity() as AppCompatActivity).supportActionBar?.let { bar ->
            bar.setDisplayHomeAsUpEnabled(false)
            bar.title = when (chooseTargetVM.actionContext) {
                AppNames.ACTION_UPLOAD -> resources.getString(R.string.choose_target_for_share_title)
                AppNames.ACTION_COPY -> resources.getString(R.string.choose_target_for_copy_title)
                AppNames.ACTION_MOVE -> resources.getString(R.string.choose_target_for_move_title)
                else -> resources.getString(R.string.choose_target_subtitle)
            }
            // TODO configure ellipsize from start (or middle?) rather than from the end
            bar.subtitle = if (Str.empty(pickFolderVM.stateID.workspace)) {
                "${pickFolderVM.stateID.username}@${pickFolderVM.stateID.serverHost}"
            } else {
                pickFolderVM.stateID.path
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pickFolderVM.pause()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putSerializable(AppKeys.EXTRA_STATE, pickFolderVM.stateID.id)
        super.onSaveInstanceState(savedInstanceState)
    }
}
