package com.pydio.android.cells.ui.core.composables

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.LoginStatus
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons

private const val LOG_TAG = "Thumb.kt"

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
            .padding(dimensionResource(R.dimen.list_thumb_decorator_padding))
            .wrapContentSize(Alignment.BottomEnd)
            .size(dimensionResource(R.dimen.list_thumb_decorator_size))

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
        JobStatus.NEW.id -> CellsIcons.New
        JobStatus.PROCESSING.id -> CellsIcons.Processing
        JobStatus.PAUSED.id -> CellsIcons.Pause
        JobStatus.CANCELLED.id -> CellsIcons.Cancel
        JobStatus.WARNING.id -> CellsIcons.ErrorDecorator
        JobStatus.ERROR.id -> CellsIcons.ErrorDecorator
        JobStatus.DONE.id -> CellsIcons.Check
        else -> {
            Log.e(LOG_TAG, "Adding unknown decorator for status $status")
            CellsIcons.Unknown
        }
    }

    val color = when (status) {
        JobStatus.NEW.id -> CellsColor.warning
        JobStatus.PROCESSING.id -> CellsColor.warning
        JobStatus.PAUSED.id -> CellsColor.warning
        JobStatus.CANCELLED.id -> CellsColor.danger
        JobStatus.WARNING.id -> CellsColor.warning
        JobStatus.ERROR.id -> CellsColor.danger
        JobStatus.DONE.id -> CellsColor.ok
        else -> CellsColor.danger
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
        LoginStatus.NoCreds.id -> R.drawable.ic_outline_running_with_errors_24
        LoginStatus.Expired.id -> R.drawable.ic_outline_running_with_errors_24
        LoginStatus.Unauthorized.id -> R.drawable.ic_outline_running_with_errors_24
        LoginStatus.Refreshing.id -> R.drawable.ic_baseline_wifi_protected_setup_24
        LoginStatus.Connected.id -> R.drawable.ic_baseline_check_24
        else -> R.drawable.empty
    }

    val colorFilter = when (authStatus) {
        LoginStatus.NoCreds.id -> CellsColor.danger
        LoginStatus.Expired.id -> CellsColor.danger
        LoginStatus.Unauthorized.id -> CellsColor.danger
        LoginStatus.Refreshing.id -> CellsColor.warning
        LoginStatus.Connected.id -> CellsColor.ok
        else -> CellsColor.danger
    }

    Image(
        painter = painterResource(imageId),
        contentDescription = authStatus,
        colorFilter = ColorFilter.tint(colorFilter),
        modifier = modifier
    )
}
