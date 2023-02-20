package com.pydio.android.cells.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.pydio.android.cells.R

/**
 * Cells icons. Material icons are [ImageVector]s, custom icons are drawable resource IDs.
 */

object CellsIcons {
    val AccountCircle = Icons.Outlined.AccountCircle
    val Add = Icons.Rounded.Add
    val ArrowBack = Icons.Rounded.ArrowBack
    val ArrowDropDown = Icons.Default.ArrowDropDown
    val ArrowDropUp = Icons.Default.ArrowDropUp
    val Bookmark = Icons.Default.Star
    val Cancel = Icons.Outlined.Cancel
    val Check = Icons.Default.Check
    val CellThumb = Icons.Default.FolderShared
    val ClearCache = Icons.Default.FolderDelete
    val Close = Icons.Default.Close
    val CopyTo = Icons.Default.FileCopy
    val CreateFolder = Icons.Default.CreateNewFolder
    val Delete = Icons.Outlined.Delete
    val DeleteForever = Icons.Default.DeleteForever
    val DownloadFile = Icons.Outlined.FileDownload
    val DownloadToDevice = Icons.Outlined.CloudDownload
    val Edit = Icons.Default.Edit
    val EmptyRecycle = Icons.Default.DeleteForever
    val ErrorDecorator = Icons.Default.PriorityHigh
    val ExpandLess = Icons.Rounded.ExpandLess
    val Fullscreen = Icons.Rounded.Fullscreen
    val Grid3x3 = Icons.Rounded.Grid3x3
    val ImportFile = Icons.Default.Backup
    val KeepOffline = Icons.Default.DownloadForOffline
    val KeepOfflineOld = Icons.Default.FileDownloadDone
    val Link = Icons.Default.Link
    val Login = Icons.Default.Login
    val Logout = Icons.Default.Logout
    val Menu = Icons.Default.Menu
    val Metered = Icons.Default.NetworkCheck
    val MoreVert = Icons.Default.MoreVert
    val MoveTo = Icons.Default.DriveFileMove
    val MyFiles = Icons.Default.FolderShared
    val MyFilesThumb = Icons.Default.FolderSpecial
    val NoInternet = Icons.Default.CloudOff
    val OpenLocation = Icons.Default.OpenInBrowser
    val Pause = Icons.Default.Pause
    val Person = Icons.Rounded.Person
    val PlayArrow = Icons.Rounded.PlayArrow
    val Processing = Icons.Outlined.RocketLaunch
    val QRCode = Icons.Default.QrCode
    val Rename = Icons.Outlined.DriveFileRenameOutline
    val RestoreFromTrash = Icons.Default.RestoreFromTrash
    val Resume = Icons.Default.Replay
    val Search = Icons.Default.Search
    val Settings = Icons.Rounded.Settings
    val Share = Icons.Default.Share
    val SwitchAccount = Icons.Default.SwitchAccount
    val TakePicture = Icons.Default.PhotoCamera
    val Transfers = Icons.Default.ImportExport
    val Unknown = Icons.Default.QuestionMark
    val UploadFile = Icons.Outlined.UploadFile
    val WorkspaceThumb = Icons.Default.Folder
}

object CellsDrawableIcons {
    val Bookmark = R.drawable.ic_baseline_star_24
//    val FileDownload = R.drawable.ic_outline_file_download_24
//    val FileUpload = R.drawable.ic_outline_file_upload_24
}