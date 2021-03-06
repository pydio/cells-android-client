package com.pydio.android.cells.ui.menus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.MoreMenuAddBinding
import com.pydio.android.cells.databinding.MoreMenuBookmarksBinding
import com.pydio.android.cells.databinding.MoreMenuBrowseBinding
import com.pydio.android.cells.databinding.MoreMenuMultiBinding
import com.pydio.android.cells.databinding.MoreMenuOfflineRootsBinding
import com.pydio.android.cells.databinding.MoreMenuRecycleBinding
import com.pydio.android.cells.databinding.MoreMenuSearchBinding
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.tasks.copyNodes
import com.pydio.android.cells.tasks.createFolder
import com.pydio.android.cells.tasks.deleteFromRecycle
import com.pydio.android.cells.tasks.displayAsQRCode
import com.pydio.android.cells.tasks.emptyRecycle
import com.pydio.android.cells.tasks.moveNodes
import com.pydio.android.cells.tasks.moveNodesToRecycle
import com.pydio.android.cells.tasks.moveToRecycle
import com.pydio.android.cells.tasks.rename
import com.pydio.android.cells.transfer.ChooseTargetContract
import com.pydio.android.cells.transfer.FileExporter
import com.pydio.android.cells.transfer.FileImporter
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.utils.showLongMessage
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * More menu fragment: it is used to present the end-user with various possible actions
 * depending on the context.
 */
class TreeNodeMenuFragment : BottomSheetDialogFragment() {

    private val logTag = TreeNodeMenuFragment::class.java.simpleName

    companion object {
        const val CONTEXT_BROWSE = "browse"
        const val CONTEXT_ADD = "add"
        const val CONTEXT_RECYCLE = "from_recycle"
        const val CONTEXT_BOOKMARKS = "from_bookmarks"
        const val CONTEXT_SEARCH = "from_search"
        const val CONTEXT_OFFLINE = "from_offline"

        const val ACTION_DOWNLOAD_TO_DEVICE = "download_to_device"
        const val ACTION_OPEN_IN_WORKSPACES = "open_in_workspaces"
        const val ACTION_OPEN_PARENT_IN_WORKSPACES = "open_parent_in_workspaces"
        const val ACTION_RENAME = "rename"
        const val ACTION_COPY = "copy"
        const val ACTION_MOVE = "move"
        const val ACTION_DELETE = "delete"
        const val ACTION_TOGGLE_BOOKMARK = "toggle_bookmark"
        const val ACTION_TOGGLE_OFFLINE = "toggle_offline"
        const val ACTION_TOGGLE_SHARED = "toggle_shared"
        const val ACTION_PUBLIC_LINK_COPY = "copy_public_link"
        const val ACTION_DISPLAY_AS_QRCODE = "display_as_qrcode"
        const val ACTION_PUBLIC_LINK_SHARE = "share_public_link"
        const val ACTION_EMPTY_RECYCLE = "empty_recycle"
        const val ACTION_RESTORE_FROM_RECYCLE = "restore_from_recycle"
        const val ACTION_DELETE_PERMANENTLY = "delete_permanently"
        const val ACTION_CREATE_FOLDER = "create_folder"
        const val ACTION_IMPORT_FILES = "import_files"
        const val ACTION_IMPORT_FROM_CAMERA = "import_from_camera"
        const val ACTION_FORCE_RESYNC = "force_resync"
    }

    private val nodeService: NodeService by inject()

    private val args: TreeNodeMenuFragmentArgs by navArgs()
    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private val treeNodeMenuVM: TreeNodeMenuViewModel by viewModel {
        val stateIds = mutableListOf<StateID>()
        for (encoded in args.selected) {
            stateIds.add(StateID.fromId(encoded))
            // val currState = StateID.fromId(encoded)
            // Log.e(logTag, "configuring more menu: $currState")
            // stateIds.add(currState)
        }
        parametersOf(stateIds, args.contextType)
    }

    // Only *one* of the below bindings is not null, depending on the context
    private var browseBinding: MoreMenuBrowseBinding? = null
    private var offlineRootsBinding: MoreMenuOfflineRootsBinding? = null
    private var addBinding: MoreMenuAddBinding? = null
    private var searchBinding: MoreMenuSearchBinding? = null
    private var bookmarkBinding: MoreMenuBookmarksBinding? = null
    private var recycleBinding: MoreMenuRecycleBinding? = null
    private var multiBinding: MoreMenuMultiBinding? = null

    // Contracts for file transfers to and from the device
    private lateinit var fileImporter: FileImporter
    private lateinit var fileExporter: FileExporter

    private var launchCopy = registerForActivityResult(ChooseTargetContract()) {
        it?.let {
            dismiss() // close the "more" menu
            copyNodes(requireContext(), treeNodeMenuVM.stateIDs, it, nodeService)
        }
    }

    private var launchMove = registerForActivityResult(ChooseTargetContract()) {
        it?.let {
            dismiss()
            moveNodes(requireContext(), treeNodeMenuVM.stateIDs, it, nodeService)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(logTag, "onCreate")

        // Communication with the device to import files / take pictures, video, etc.
        fileImporter = FileImporter(
            requireActivity().activityResultRegistry,
            treeNodeMenuVM,
            logTag,
            this,
        )
        lifecycle.addObserver(fileImporter)

        // Download cached or remote files to device
        fileExporter = FileExporter(
            requireActivity().activityResultRegistry,
            treeNodeMenuVM.stateIDs[0],
            logTag,
            this,
        )
        lifecycle.addObserver(fileExporter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // TODO we must observe the LiveData or the variables are empty when we want to use them.
        //   Understand and find a more elegant / standard way to do it.
        treeNodeMenuVM.node.observe(viewLifecycleOwner) { it?.let {} }
        treeNodeMenuVM.nodes.observe(viewLifecycleOwner) { it?.let {} }

        // Handle specific corner cases: no or more than one node
        if (treeNodeMenuVM.stateIDs.isEmpty()) {
            return null
        } else if (treeNodeMenuVM.stateIDs.size > 1) {
            return inflateMultiSelectedLayout(inflater, container)
        }

        Log.e(logTag, "Creating more menu for single node, context: ${treeNodeMenuVM.contextType}")

        // Provide correct UI for a single node depending on the context
        return when (treeNodeMenuVM.contextType) {
            CONTEXT_BROWSE -> inflateBrowseLayout(inflater, container)
            CONTEXT_ADD -> inflateAddLayout(inflater, container)
            CONTEXT_RECYCLE -> inflateRecycleLayout(inflater, container)
            CONTEXT_BOOKMARKS -> inflateBookmarkLayout(inflater, container)
            CONTEXT_SEARCH -> inflateSearchLayout(inflater, container)
            CONTEXT_OFFLINE -> inflateOfflineLayout(inflater, container)
            else -> null
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(logTag, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i(logTag, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i(logTag, "onStop")
    }

    /* BROWSE CONTEXT */

    private fun inflateBrowseLayout(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View {
        browseBinding = DataBindingUtil.inflate(
            inflater, R.layout.more_menu_browse, container, false
        )
        val binding = browseBinding as MoreMenuBrowseBinding
        treeNodeMenuVM.node.observe(this) {
            it?.let {
                bind(binding, it)
            }
        }
        binding.executePendingBindings()
        return binding.root
    }

    private fun bind(binding: MoreMenuBrowseBinding, node: RTreeNode) {
        binding.node = node

//         binding.openWith.setOnClickListener { onClicked(node, ACTION_OPEN_WITH) }
        binding.download.setOnClickListener { onClicked(ACTION_DOWNLOAD_TO_DEVICE) }
        binding.rename.setOnClickListener { onClicked(ACTION_RENAME) }
        binding.copyTo.setOnClickListener { onClicked(ACTION_COPY) }
        binding.moveTo.setOnClickListener { onClicked(ACTION_MOVE) }
        binding.delete.setOnClickListener { onClicked(ACTION_DELETE) }
        binding.bookmarkSwitch.setOnClickListener { onClicked(ACTION_TOGGLE_BOOKMARK) }

        if (!node.isShared()) {
            binding.sharedSwitch.setOnClickListener { onClicked(ACTION_TOGGLE_SHARED) }
            binding.publicLinkOff.visibility = View.VISIBLE
            binding.publicLinkOn.visibility = View.GONE
        } else {
            binding.publicLinkOff.visibility = View.GONE
            binding.publicLinkOn.visibility = View.VISIBLE
            binding.displayAsQrcode.setOnClickListener { onClicked(ACTION_DISPLAY_AS_QRCODE) }
            binding.publicLinkDelete.setOnClickListener { onClicked(ACTION_TOGGLE_SHARED) }
            binding.publicLinkShareTo.setOnClickListener { onClicked(ACTION_PUBLIC_LINK_SHARE) }
            binding.publicLinkCopyToClipboard.setOnClickListener { onClicked(ACTION_PUBLIC_LINK_COPY) }
        }

        // Offline is not supported when remote server is P8
        var legacy = false
        activeSessionVM.sessionView.value?.let {
            legacy = it.isLegacy
        }
        binding.offlineRoot.visibility = if (legacy) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.offlineSwitch.setOnClickListener { onClicked(ACTION_TOGGLE_OFFLINE) }


        binding.executePendingBindings()
    }

    /* OFFLINE CONTEXT */

    private fun inflateOfflineLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        offlineRootsBinding = DataBindingUtil.inflate(
            inflater, R.layout.more_menu_offline_roots, container, false
        )
        val binding = offlineRootsBinding as MoreMenuOfflineRootsBinding
        treeNodeMenuVM.node.observe(this) {
            it?.let {
                bind(binding, it)
            }
        }
        binding.executePendingBindings()
        return binding.root
    }

    private fun bind(binding: MoreMenuOfflineRootsBinding, node: RTreeNode) {

        binding.node = node
        binding.forceResync.setOnClickListener { onClicked(ACTION_FORCE_RESYNC) }
        binding.download.setOnClickListener { onClicked(ACTION_DOWNLOAD_TO_DEVICE) }
        binding.offlineSwitch.setOnClickListener { onClicked(ACTION_TOGGLE_OFFLINE) }
        binding.openParentInWorkspace.setOnClickListener {
            onClicked(ACTION_OPEN_PARENT_IN_WORKSPACES)
        }
        // TODO
        // binding.openWith.setOnClickListener { onClicked(node, ACTION_OPEN_WITH) }
        binding.executePendingBindings()
    }

    /* ADD LIST CONTEXT (triggered from FAB while browsing) */

    private fun inflateAddLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        addBinding = DataBindingUtil.inflate(
            inflater, R.layout.more_menu_add, container, false
        )
        val binding = addBinding as MoreMenuAddBinding
        treeNodeMenuVM.node.observe(this) {
            it?.let {
                bind(binding, it)
            }
        }
        binding.executePendingBindings()
        return binding.root
    }

    private fun bind(binding: MoreMenuAddBinding, node: RTreeNode) {
        binding.node = node

        binding.createFolder.setOnClickListener { onClicked(ACTION_CREATE_FOLDER) }
        binding.importFiles.setOnClickListener { onClicked(ACTION_IMPORT_FILES) }
        if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            binding.importFromCamera.setOnClickListener { onClicked(ACTION_IMPORT_FROM_CAMERA) }
        } else {
            binding.importFromCamera.visibility = View.GONE
        }

        binding.executePendingBindings()
    }

    /* SEARCH CONTEXT */

    private fun inflateSearchLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        searchBinding = DataBindingUtil.inflate(
            inflater, R.layout.more_menu_search, container, false
        )
        val binding = searchBinding as MoreMenuSearchBinding
        treeNodeMenuVM.node.observe(this) {
            Log.e(logTag, "observing node for search more menu: ${it?.getStateID()}")

            it?.let {
                bind(binding, it)
            }
        }
        binding.executePendingBindings()
        return binding.root
    }

    private fun bind(binding: MoreMenuSearchBinding, node: RTreeNode) {
        binding.node = node

        binding.openInWorkspace.setOnClickListener { onClicked(ACTION_OPEN_IN_WORKSPACES) }
        binding.openParentInWorkspace.setOnClickListener {
            onClicked(ACTION_OPEN_PARENT_IN_WORKSPACES)
        }

        binding.executePendingBindings()
    }

    /* RECYCLE and WITHIN CONTEXT */

    private fun inflateRecycleLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        recycleBinding = DataBindingUtil.inflate(
            inflater, R.layout.more_menu_recycle, container, false
        )
        val binding = recycleBinding as MoreMenuRecycleBinding
        treeNodeMenuVM.node.observe(this) {
            it?.let {
                bind(binding, it)
            }
        }
        binding.executePendingBindings()
        return binding.root
    }

    private fun bind(binding: MoreMenuRecycleBinding, node: RTreeNode) {
        binding.node = node
        binding.emptyRecycle.setOnClickListener { onClicked(ACTION_EMPTY_RECYCLE) }
        binding.restoreFromRecycle.setOnClickListener {
            onClicked(ACTION_RESTORE_FROM_RECYCLE)
        }
        binding.deletePermanently.setOnClickListener { onClicked(ACTION_DELETE_PERMANENTLY) }
//         binding.openWith.setOnClickListener { onClicked(node, ACTION_OPEN_WITH) }
        binding.executePendingBindings()
    }

    /* BOOKMARK LIST CONTEXT */

    private fun inflateBookmarkLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        bookmarkBinding = DataBindingUtil.inflate(
            inflater, R.layout.more_menu_bookmarks, container, false
        )
        val binding = bookmarkBinding as MoreMenuBookmarksBinding
        treeNodeMenuVM.node.observe(this) { it?.let { bind(binding, it) } }
        binding.executePendingBindings()
        return binding.root
    }

    private fun bind(binding: MoreMenuBookmarksBinding, node: RTreeNode) {
        binding.node = node
        binding.bookmarkSwitch.setOnClickListener { onClicked(ACTION_TOGGLE_BOOKMARK) }
        binding.download.setOnClickListener { onClicked(ACTION_DOWNLOAD_TO_DEVICE) }
        binding.openParentInWorkspace.setOnClickListener {
            onClicked(ACTION_OPEN_PARENT_IN_WORKSPACES)
        }
        binding.executePendingBindings()
    }

    /* MULTI SELECTION CONTEXT */

    private fun inflateMultiSelectedLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        multiBinding = DataBindingUtil.inflate(
            inflater, R.layout.more_menu_multi, container, false
        )
        val binding = multiBinding as MoreMenuMultiBinding
        bind(binding, treeNodeMenuVM.stateIDs.size)
        binding.executePendingBindings()
        return binding.root
    }

    private fun bind(binding: MoreMenuMultiBinding, selectionSize: Int) {
        binding.selectionSize = selectionSize
        binding.copyTo.setOnClickListener { onClicked(ACTION_COPY) }
        binding.moveTo.setOnClickListener { onClicked(ACTION_MOVE) }
        binding.delete.setOnClickListener { onClicked(ACTION_DELETE) }
        binding.executePendingBindings()
    }

    /* GENERIC METHODS */

    private fun onClicked(actionId: String) {
        if (treeNodeMenuVM.stateIDs.size == 1) {
            return onSingleClicked(actionId)
        }
        Log.e(logTag, "onClicked for multi selection more menu")

        // TODO add sanity checks in the various called commands to insure we do not launch a
        //   forbidden action (typically moving or copying a node inside itself).

        // Log.e(logTag, "One node: " + treeNodeMenuVM.node.value?.getStateID())
        // Log.e(logTag, "All nodes size: " + treeNodeMenuVM.nodes.value?.size)

        val node = treeNodeMenuVM.node.value ?: return
        val parent = StateID.fromId(node.encodedState).parent()

        val moreMenu = this
        lifecycleScope.launch {
            when (actionId) {
                ACTION_COPY -> {
                    launchCopy.launch(Pair(parent, AppNames.ACTION_COPY))
                }
                ACTION_MOVE -> {
                    launchMove.launch(Pair(parent, AppNames.ACTION_MOVE))
                }
                ACTION_DELETE -> {
                    moveNodesToRecycle(requireContext(), treeNodeMenuVM.stateIDs, nodeService)
                    moreMenu.dismiss()
                }
                // TODO handle case when we are in the recycle ?
//                ACTION_DELETE_PERMANENTLY -> {
//                    deleteFromRecycle(requireContext(), node)
//                    moreMenu.dismiss()
//                }
//                ACTION_RESTORE_FROM_RECYCLE -> {
//                    CellsApp.instance.nodeService.restoreNode(node.getStateID())?.let {
//                        showLongMessage(requireContext(), it)
//                    }
//                    moreMenu.dismiss()
//                }

                // TODO Also add this ?
//                ACTION_DOWNLOAD_TO_DEVICE -> {
//                    fileExporter.pickTargetLocation(node)
//                }
            }
        }
    }

    private fun onSingleClicked(actionOpenWith: String) {
        // val moreMenu = this
        val node = treeNodeMenuVM.node.value ?: return
        Log.d(logTag, "${node.getStateID()} -> $actionOpenWith")
        lifecycleScope.launch {
            when (actionOpenWith) {
                // Impact remote server
                //  TODO handle a loading state
                ACTION_CREATE_FOLDER -> {
                    createFolder(requireContext(), node.getStateID(), nodeService)
                    doDismiss()
                }
                ACTION_RENAME -> {
                    rename(requireContext(), node, nodeService)
                    doDismiss()
                }
                ACTION_COPY -> {
                    launchCopy.launch(
                        Pair(
                            StateID.fromId(node.encodedState).parent(),
                            AppNames.ACTION_COPY
                        )
                    )
                }
                ACTION_MOVE -> {
                    launchMove.launch(
                        Pair(
                            StateID.fromId(node.encodedState).parent(),
                            AppNames.ACTION_MOVE
                        )
                    )
                }
                ACTION_DELETE -> {
                    moveToRecycle(requireContext(), node, nodeService)
                    doDismiss()
                }
                ACTION_EMPTY_RECYCLE -> {
                    emptyRecycle(requireContext(), node, nodeService)
                    doDismiss()
                }
                ACTION_DELETE_PERMANENTLY -> {
                    deleteFromRecycle(requireContext(), node, nodeService)
                    doDismiss()
                }
                ACTION_RESTORE_FROM_RECYCLE -> {
                    nodeService.restoreNode(node.getStateID())?.let {
                        showLongMessage(requireContext(), it)
                    }
                    doDismiss()
                }
                ACTION_TOGGLE_BOOKMARK -> {
                    nodeService.toggleBookmark(node)
                    doDismiss()
                }
                ACTION_TOGGLE_SHARED -> {
                    // TODO ask confirmation
                    nodeService.toggleShared(node)?.let {
                        // If we created a link we get it as result and put it in the clipboard directly
                        val clipboard =
                            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                        if (clipboard != null) {
                            val clip = ClipData.newPlainText(node.name, it)
                            clipboard.setPrimaryClip(clip)
                            showMessage(
                                requireContext(),
                                resources.getString(R.string.link_copied_to_clip)
                            )
                        }
                    }
                    doDismiss()
                }
                ACTION_PUBLIC_LINK_COPY -> {
                    val clipboard =
                        requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val link = node.getShareAddress()
                    if (clipboard != null && link != null) {
                        val clip = ClipData.newPlainText(node.name, link)
                        clipboard.setPrimaryClip(clip)
                        showMessage(
                            requireContext(),
                            resources.getString(R.string.link_copied_to_clip)
                        )
                    } else { // Should never happen.
                        showMessage(
                            requireContext(),
                            resources.getString(R.string.link_copy_failed)
                        )
                    }
                    doDismiss()
                }
                ACTION_DISPLAY_AS_QRCODE -> {
                    displayAsQRCode(requireContext(), node)
                    doDismiss()
                }
                ACTION_PUBLIC_LINK_SHARE -> {
                    node.getShareAddress()?.let { link ->
                        val shareIntent = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, link)
                        startActivity(shareIntent)
                    }
                    doDismiss()
                }
                ACTION_TOGGLE_OFFLINE -> {
                    nodeService.toggleOffline(node)
                    doDismiss()
                }
                ACTION_FORCE_RESYNC -> {
                    nodeService.syncOfflineRoot(node)
                    doDismiss()
                }
                // In-app navigation
                ACTION_OPEN_IN_WORKSPACES -> {
                    // CellsApp.instance.setCurrentState(StateID.fromId(node.encodedState))
                    findNavController().navigate(MainNavDirections.openFolder(node.encodedState))
                }
                ACTION_OPEN_PARENT_IN_WORKSPACES -> {
                    val parentState = StateID.fromId(node.encodedState).parent()
                    // CellsApp.instance.setCurrentState(parentState)
                    findNavController().navigate(MainNavDirections.openFolder(parentState.id))
                }
                // Transfer to and from device
                ACTION_IMPORT_FILES -> {
                    fileImporter.selectFiles()
                    // dismissal must be done in the ResultContract receiver or we miss the return.
                    // moreMenu.dismiss()
                }
                ACTION_IMPORT_FROM_CAMERA -> {
                    fileImporter.takePicture(node.getStateID())
                }
                ACTION_DOWNLOAD_TO_DEVICE -> {
                    fileExporter.pickTargetLocation(node)
                }
//                ACTION_OPEN_WITH ->
//                    CellsApp.instance.nodeService.getOrDownloadFileToCache(node)?.let {
//                        val intent = openWith(requireContext(), it, node)
//                        // Insure we won't crash if there no activity to handle this kind of intent
//                        if (activity?.packageManager?.resolveActivity(intent, 0) != null) {
//                            startActivity(intent)
//                        } else {
//                            showLongMessage(requireContext(), "No app found to open this file")
//                        }
//                        moreMenu.dismiss()
//                    }
            }
        }
    }

    private fun doDismiss() {
        try {
            dismiss()
        } catch (e: Exception) {
            Log.e(logTag, "cannot dismiss more menu for ${treeNodeMenuVM.stateIDs}: ${e.message}")
        }
    }

}
