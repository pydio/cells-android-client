package com.pydio.android.cells.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.betterMime
import com.pydio.cells.api.SdkNames

/**
 * Cells icons. Material icons are [ImageVector]s, custom icons are drawable resource IDs.
 */
object CellsIcons {
    val About = Icons.Outlined.Info
    val AccountCircle = Icons.Outlined.AccountCircle
    val Add = Icons.Rounded.Add
    val ArrowBack = Icons.Rounded.ArrowBack
    val ArrowDropDown = Icons.Default.ArrowDropDown
    val ArrowDropUp = Icons.Default.ArrowDropUp
    val AsGrid = Icons.Outlined.GridView
    val AsList = Icons.Outlined.ViewList
    val AsSmallerGrid = Icons.Default.ViewCompact
    val Bookmark = Icons.Outlined.StarBorder
    val ButtonFavorite = Icons.Outlined.StarBorder
    val ButtonOffline = Icons.Outlined.CloudDownload
    val ButtonShare = Icons.Outlined.Share
    val Cancel = Icons.Outlined.Cancel
    val CancelSearch = Icons.Filled.Close
    val Check = Icons.Default.Check
    val CellThumb = Icons.Outlined.FolderShared
    val ClearCache = Icons.Default.FolderDelete
    val Close = Icons.Default.Close
    val CopyTo = Icons.Outlined.FileCopy
    val CreateFolder = Icons.Outlined.CreateNewFolder
    val Delete = Icons.Outlined.Delete
    val DeleteForever = Icons.Outlined.DeleteForever
    val DownloadFile = Icons.Outlined.FileDownload
    val DownloadToDevice = Icons.Outlined.CloudDownload
    val Edit = Icons.Default.Edit
    val EmptyFolder = Icons.Outlined.Folder
    val EmptyRecycle = Icons.Outlined.DeleteForever
    val ErrorDecorator = Icons.Default.PriorityHigh
    val ExpandLess = Icons.Rounded.ExpandLess
    val FilterBy = Icons.Default.FilterList
    val Fullscreen = Icons.Rounded.Fullscreen
    val Grid3x3 = Icons.Rounded.Grid3x3
    val ImportFile = Icons.Outlined.Backup
    val Jobs = Icons.Outlined.WorkHistory
    val KeepOffline = Icons.Outlined.DownloadForOffline
    val KeepOfflineOld = Icons.Default.FileDownloadDone
    val Link = Icons.Outlined.Link
    val Login = Icons.Default.Login
    val Logout = Icons.Default.Logout
    val Logs = Icons.Outlined.Build
    val Menu = Icons.Default.Menu
    val Metered = Icons.Default.NetworkCheck
    val MoreVert = Icons.Default.MoreVert
    val MoveTo = Icons.Outlined.DriveFileMove
    val MyFiles = Icons.Default.FolderShared
    val MyFilesThumb = Icons.Outlined.FolderSpecial
    val New = Icons.Outlined.FiberNew
    val NoInternet = Icons.Default.CloudOff
    val NoValidCredentials = Icons.Outlined.PersonOff
    val OpenLocation = Icons.Default.OpenInBrowser
    val Pause = Icons.Default.Pause
    val Person = Icons.Rounded.Person
    val PlayArrow = Icons.Rounded.PlayArrow
    val Processing = Icons.Outlined.RocketLaunch
    val QRCode = Icons.Default.QrCode
    val Refresh = Icons.Default.Refresh
    val Rename = Icons.Outlined.DriveFileRenameOutline
    val RestoreFromTrash = Icons.Outlined.RestoreFromTrash
    val Resume = Icons.Default.Replay
    val Search = Icons.Default.Search
    val Settings = Icons.Outlined.Settings
    val Share = Icons.Default.Share
    val SortBy = Icons.Default.Sort
    val SwitchAccount = Icons.Default.SwitchAccount
    val TakePicture = Icons.Outlined.PhotoCamera
    val Transfers = Icons.Default.ImportExport
    val Unknown = Icons.Default.QuestionMark
    val UploadFile = Icons.Outlined.UploadFile
    val WorkspaceThumb = Icons.Outlined.Folder
}

object CellsDrawableIcons {
    const val Bookmark = R.drawable.ic_baseline_star_24
//    val FileDownload = R.drawable.ic_outline_file_download_24
//    val FileUpload = R.drawable.ic_outline_file_upload_24
}

enum class CellsIconType {
    // Workspace Roots
    WS_PERSONAL, WS_CELL, WS_DEFAULT,

    // Folders
    FOLDER, RECYCLE,

    // Documents
    WORD, CALC, PRESENTATION, PDF, DOCUMENT,

    // Media
    IMAGE, VIDEO, AUDIO,

    // Other Files
    FILE, CODE, ZIP
}

fun getIconTypeFromMime(originalMime: String, sortName: String?): CellsIconType {
    val mime = betterMime(originalMime, sortName)
    return when {
        // WS Types
        mime == SdkNames.WS_TYPE_PERSONAL -> CellsIconType.WS_PERSONAL
        mime == SdkNames.WS_TYPE_CELL -> CellsIconType.WS_CELL
        mime == SdkNames.WS_TYPE_DEFAULT -> CellsIconType.WS_DEFAULT
        mime == SdkNames.NODE_MIME_WS_ROOT -> {
            // Tweak: we deduce type of ws root from the sort name. Not very clean
            val prefix = sortName ?: ""
            when {
                prefix.startsWith("1_2") -> CellsIconType.WS_PERSONAL
                prefix.startsWith("1_8") -> CellsIconType.WS_CELL
                else -> CellsIconType.WS_DEFAULT
            }
        }
        // Folders
        mime == SdkNames.NODE_MIME_FOLDER -> CellsIconType.FOLDER
        mime == SdkNames.NODE_MIME_RECYCLE -> CellsIconType.RECYCLE
        // Files
        mime.startsWith("image/", true) -> CellsIconType.IMAGE
        mime.startsWith("audio/", true) -> CellsIconType.AUDIO
        mime.startsWith("video/", true) -> CellsIconType.VIDEO
        mime == "application/rtf" || mime == "text/plain"
        -> CellsIconType.DOCUMENT

        mime == "application/vnd.oasis.opendocument.text"
                || mime == "application/msword"
                || mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        -> CellsIconType.WORD

        mime == "text/csv" || mime == "application/vnd.ms-excel"
                || mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        -> CellsIconType.CALC

        mime == "application/vnd.oasis.opendocument.presentation"
                || mime == "application/vnd.ms-powerpoint"
                || mime == "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        -> CellsIconType.PRESENTATION

        mime == "application/pdf"
        -> CellsIconType.PDF

        mime == "application/x-httpd-php" ||
                mime == "application/xml" ||
                mime == "text/javascript" ||
                mime == "application/xhtml+xml"
        -> CellsIconType.CODE

        mime == "application/zip" ||
                mime == "application/x-7z-compressed" ||
                mime == "application/x-tar" ||
                mime == "application/java-archive"
        -> CellsIconType.ZIP

        else -> CellsIconType.FILE
    }
}

//fun getDrawableFromType(type: CellsIconType): Int {
//    return when (type) {
//        // Workspace Roots
//        CellsIconType.WS_PERSONAL -> R.drawable.aa_200_folder_shared_48px
//        CellsIconType.WS_CELL -> R.drawable.file_cells_logo
//        CellsIconType.WS_DEFAULT -> R.drawable.aa_200_folder_48px
//        // Folders
//        CellsIconType.FOLDER -> R.drawable.aa_200_folder_48px
//        CellsIconType.RECYCLE -> R.drawable.file_trash_outline
//        // Documents
//        CellsIconType.WORD -> R.drawable.file_word_outline
//        CellsIconType.CALC -> R.drawable.file_excel_outline
//        CellsIconType.PRESENTATION -> R.drawable.file_powerpoint_outline
//        CellsIconType.PDF -> R.drawable.file_pdf_box
//        CellsIconType.DOCUMENT -> R.drawable.file_document_outline
//        // Media
//        CellsIconType.IMAGE -> R.drawable.file_image_outline
//        CellsIconType.VIDEO -> R.drawable.ic_outline_audio_file_24
//        CellsIconType.AUDIO -> R.drawable.ic_outline_video_file_24
//        // Other Files
//        CellsIconType.CODE -> R.drawable.file_code_outline
//        CellsIconType.ZIP -> R.drawable.file_zip_outline
//        CellsIconType.FILE -> R.drawable.file_outline
////         else -> R.drawable.file_outline
//    }
//}

@Composable
fun getIconAndColorFromType(type: CellsIconType): Pair<Int, Color> {
    val defCol = MaterialTheme.colorScheme.onSurface
    return when (type) {
        // Workspace Roots
        CellsIconType.WS_PERSONAL -> R.drawable.folder_shared_24px to defCol
        CellsIconType.WS_CELL -> R.drawable.file_cells_logo to defCol
        CellsIconType.WS_DEFAULT -> R.drawable.folder_24px to defCol
        // Folders
        CellsIconType.FOLDER -> R.drawable.folder_24px to defCol
        CellsIconType.RECYCLE -> R.drawable.delete_24px to defCol
        // Documents
        CellsIconType.WORD -> R.drawable.file_word_outline to CellsColor.material_blue
        CellsIconType.CALC -> R.drawable.file_excel_outline to CellsColor.material_green
        CellsIconType.PRESENTATION -> R.drawable.file_powerpoint_outline to defCol
        CellsIconType.PDF -> R.drawable.picture_as_pdf_40px to CellsColor.material_red
        CellsIconType.DOCUMENT -> R.drawable.description_40px to CellsColor.material_blue
        // Media
        CellsIconType.IMAGE -> R.drawable.image_40px to CellsColor.material_deep_orange
        CellsIconType.VIDEO -> R.drawable.video_file_40px to CellsColor.material_orange
        CellsIconType.AUDIO -> R.drawable.audio_file_40px to CellsColor.material_deep_orange
        // Other Files
        CellsIconType.CODE -> R.drawable.file_code_outline to CellsColor.material_yellow
        CellsIconType.ZIP -> R.drawable.folder_zip_40px to CellsColor.material_yellow
        CellsIconType.FILE -> R.drawable.description_40px to defCol
//         else -> R.drawable.file_outline
    }
}
