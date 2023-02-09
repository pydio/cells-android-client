package com.pydio.android.cells.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.pydio.android.cells.R

/**
 * Cells icons. Material icons are [ImageVector]s, custom icons are drawable resource IDs.
 */

object CellsVectorIcons {
    val AccountCircle = Icons.Outlined.AccountCircle
    val Add = Icons.Rounded.Add
    val ArrowBack = Icons.Rounded.ArrowBack
    val ArrowDropDown = Icons.Default.ArrowDropDown
    val ArrowDropUp = Icons.Default.ArrowDropUp
    val Cancel = Icons.Outlined.Cancel
    val Check = Icons.Default.Check
    val Close = Icons.Default.Close
    val Delete = Icons.Outlined.Delete
    val DownloadFile = Icons.Outlined.FileDownload
    val ErrorDecorator = Icons.Default.PriorityHigh
    val ExpandLess = Icons.Rounded.ExpandLess
    val Fullscreen = Icons.Rounded.Fullscreen
    val Grid3x3 = Icons.Rounded.Grid3x3
    val MoreVert = Icons.Default.MoreVert
    val Pause = Icons.Default.Pause
    val Person = Icons.Rounded.Person
    val PlayArrow = Icons.Rounded.PlayArrow
    val Processing = Icons.Outlined.RocketLaunch
    val Unknown = Icons.Default.QuestionMark
    val Resume = Icons.Default.Replay
    val Search = Icons.Rounded.Search
    val Settings = Icons.Rounded.Settings
    val UploadFile = Icons.Outlined.UploadFile
}

object CellsDrawableIcons {
    val Bookmark = R.drawable.ic_baseline_star_24
//    val FileDownload = R.drawable.ic_outline_file_download_24
//    val FileUpload = R.drawable.ic_outline_file_upload_24
}