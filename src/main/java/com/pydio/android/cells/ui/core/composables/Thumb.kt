package com.pydio.android.cells.ui.core.composables

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
import com.pydio.android.cells.ui.theme.CellsVectorIcons
import com.pydio.android.cells.ui.theme.danger
import com.pydio.android.cells.ui.theme.ok
import com.pydio.android.cells.ui.theme.warning

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
        AppNames.JOB_STATUS_PROCESSING -> CellsVectorIcons.Processing
        AppNames.JOB_STATUS_ERROR -> CellsVectorIcons.ErrorDecorator
        AppNames.JOB_STATUS_CANCELLED -> CellsVectorIcons.Pause
        AppNames.JOB_STATUS_DONE -> CellsVectorIcons.Check
        else -> CellsVectorIcons.Unknown
    }

    val color = when (status) {
        AppNames.JOB_STATUS_PROCESSING -> MaterialTheme.colorScheme.primary
        AppNames.JOB_STATUS_CANCELLED -> warning
        AppNames.JOB_STATUS_ERROR -> danger
        AppNames.JOB_STATUS_DONE -> ok
        else -> warning
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
        AppNames.AUTH_STATUS_NO_CREDS -> danger
        AppNames.AUTH_STATUS_EXPIRED -> danger
        AppNames.AUTH_STATUS_UNAUTHORIZED -> danger
        AppNames.AUTH_STATUS_REFRESHING -> warning
        AppNames.AUTH_STATUS_CONNECTED -> ok
        else -> danger
    }

    Image(
        painter = painterResource(imageId),
        contentDescription = authStatus,
        colorFilter = ColorFilter.tint(colorFilter),
        modifier = modifier
    )
}

@Deprecated("Rather use decorated with type")
@Composable
fun AuthDecorated(
    authStatus: String,
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

        AuthDecorator(
            authStatus = authStatus,
            modifier = Modifier
                .size(dimensionResource(R.dimen.list_thumb_decorator_size))
                .wrapContentSize(Alignment.BottomEnd)
        )
    }
}
