package com.pydio.android.cells.ui.core.composables

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons

private const val logTag = "Thumb.kt"

enum class Type {
    AUTH, JOB
}

/**
 * A simple thumb with decoration for lists
 */
@Composable
fun Decorated(
    type: Type,
    status: String,
    content: @Composable () -> Unit,
) {
    Surface(
        tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
        modifier = Modifier
            .padding(all = dimensionResource(id = R.dimen.list_thumb_margin))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.list_thumb_corner_radius)))
            .size(48.dp)

    ) {
        content()

        val decMod = Modifier
            .size(dimensionResource(R.dimen.list_thumb_decorator_size))
            .wrapContentSize(Alignment.BottomEnd)

        when (type) {
            Type.AUTH -> AuthDecorator(
                authStatus = status,
                modifier = decMod,
            )

            Type.JOB -> JobDecorator(
                status = status,
                modifier = decMod,
            )
        }
    }
}

@Composable
private fun JobDecorator(status: String, modifier: Modifier) {

    val imageId = when (status) {
        AppNames.JOB_STATUS_NEW -> CellsIcons.New
        AppNames.JOB_STATUS_PROCESSING -> CellsIcons.Processing
        AppNames.JOB_STATUS_ERROR -> CellsIcons.ErrorDecorator
        AppNames.JOB_STATUS_CANCELLED -> CellsIcons.Pause
        AppNames.JOB_STATUS_DONE -> CellsIcons.Check
        else -> {
            Log.e(logTag, "Adding unknown decorator for status $status")
            CellsIcons.Unknown
        }
    }

    val color = when (status) {
        AppNames.JOB_STATUS_NEW -> MaterialTheme.colorScheme.primary
        AppNames.JOB_STATUS_PROCESSING -> MaterialTheme.colorScheme.primary
        AppNames.JOB_STATUS_CANCELLED -> CellsColor.warning
        AppNames.JOB_STATUS_ERROR -> CellsColor.danger
        AppNames.JOB_STATUS_DONE -> CellsColor.ok
        else -> CellsColor.warning
    }

    Icon(
        imageVector = imageId,
        contentDescription = status,
        tint = color,
        modifier = modifier
    )
}

@Composable
private fun AuthDecorator(authStatus: String, modifier: Modifier) {

    val imageId = when (authStatus) {
        //AUTH_STATUS_NEW -> R.drawable.icon_folder
        AppNames.AUTH_STATUS_NO_CREDS -> R.drawable.ic_outline_running_with_errors_24
        AppNames.AUTH_STATUS_EXPIRED -> R.drawable.ic_outline_running_with_errors_24
        AppNames.AUTH_STATUS_UNAUTHORIZED -> R.drawable.ic_outline_running_with_errors_24
        AppNames.AUTH_STATUS_REFRESHING -> R.drawable.ic_baseline_wifi_protected_setup_24
        AppNames.AUTH_STATUS_CONNECTED -> R.drawable.ic_baseline_check_24
        else -> R.drawable.empty
    }

    val colorFilter = when (authStatus) {
        AppNames.AUTH_STATUS_NO_CREDS -> CellsColor.danger
        AppNames.AUTH_STATUS_EXPIRED -> CellsColor.danger
        AppNames.AUTH_STATUS_UNAUTHORIZED -> CellsColor.danger
        AppNames.AUTH_STATUS_REFRESHING -> CellsColor.warning
        AppNames.AUTH_STATUS_CONNECTED -> CellsColor.ok
        else -> CellsColor.danger
    }

    Image(
        painter = painterResource(imageId),
        contentDescription = authStatus,
        colorFilter = ColorFilter.tint(colorFilter),
        modifier = modifier
    )
}

//@Deprecated("Rather use decorated with type")
//@Composable
//fun AuthDecorated(
//    authStatus: String,
//    content: @Composable () -> Unit,
//) {
//    Surface(
//        tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
//        modifier = Modifier
//            .padding(all = dimensionResource(id = R.dimen.list_thumb_margin))
//            .clip(RoundedCornerShape(dimensionResource(R.dimen.list_thumb_corner_radius)))
//            .size(48.dp)
//
//    ) {
//
//        content()
//
//        AuthDecorator(
//            authStatus = authStatus,
//            modifier = Modifier
//                .size(dimensionResource(R.dimen.list_thumb_decorator_size))
//                .wrapContentSize(Alignment.BottomEnd)
//        )
//    }
//}
