package com.pydio.android.cells.ui.browse.composables

sealed class NodeAction(val id: String) {

    data object DownloadToDevice : NodeAction("download_to_device")
    data object ImportFile : NodeAction("import_file")
    data object CreateFolder : NodeAction("create_folder")
    data object Rename : NodeAction("rename")
    data object SelectTargetFolder : NodeAction("select_target_folder")
    data object CopyTo : NodeAction("copy_to")
    data object MoveTo : NodeAction("move_to")
    data object Delete : NodeAction("delete")
    data object RestoreFromTrash : NodeAction("restore_from_trash")
    data object PermanentlyRemove : NodeAction("permanently_remove")
    data object EmptyRecycle : NodeAction("empty_recycle")
    data object CopyToClipboard : NodeAction("copy_to_Clipboard")
    data object CreateShare : NodeAction("create_share")
    data object ShareWith : NodeAction("share_with")
    data object ShowQRCode : NodeAction("show_qr_code")
    data object RemoveLink : NodeAction("remove_link")
    data object TakePicture : NodeAction("take_picture")
    data object ForceResync : NodeAction("force_re_sync")
    data object OpenInApp : NodeAction("open_in_app")
    data object OpenParentLocation : NodeAction("open_parent_location")
    data object SortBy : NodeAction("sort_by")
    data object AsList : NodeAction("as_list")
    data object AsGrid : NodeAction("as_grid")
    class ToggleOffline(val isChecked: Boolean) : NodeAction("toggle_offline")
    class ToggleBookmark(val isChecked: Boolean) : NodeAction("toggle_bookmark")

//    data object AsSmallerGrid : NodeAction("as_smaller_grid")
}