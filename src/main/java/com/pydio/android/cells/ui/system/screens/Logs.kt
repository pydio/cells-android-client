package com.pydio.android.cells.ui.system.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.R
import com.pydio.android.cells.db.runtime.RLog
import com.pydio.android.cells.ui.core.composables.TopBarWithActions
import com.pydio.android.cells.ui.core.composables.lists.EmptyList
import com.pydio.android.cells.ui.core.composables.lists.WithListTheme
import com.pydio.android.cells.ui.system.models.LogListVM
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.android.cells.utils.timestampToString
import org.koin.androidx.compose.koinViewModel

@Composable
fun LogScreen(
    openDrawer: () -> Unit,
    logVM: LogListVM = koinViewModel()
) {
    val logs = logVM.logs.collectAsState(listOf())
    LogScreen(
        logs = logs.value,
        openDrawer = openDrawer,
        clearLogs = logVM::clearAllLogs
    )
}

@Composable
fun LogScreen(
    logs: List<RLog>,
    openDrawer: () -> Unit,
    clearLogs: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopBarWithActions(
                title = stringResource(R.string.log_list_title),
                openDrawer = openDrawer,
                actions = {
                    IconButton(onClick = clearLogs) {
                        Icon(
                            imageVector = CellsIcons.Delete,
                            contentDescription = stringResource(R.string.clear_all_logs)
                        )
                    }
                },
            )
        }
    ) { innerPadding ->

        Box(modifier = Modifier.padding(innerPadding)) {
            if (logs.isEmpty()) {
                EmptyList(
                    listContext = ListContext.SYSTEM,
                    desc = stringResource(R.string.log_list_empty_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(.5f)
                        .wrapContentSize(Alignment.Center)
                )
            }
            WithListTheme {
                LogList(logs)
            }
        }
    }
}

@Composable
fun LogList(logs: List<RLog>) {
    LazyColumn(
        Modifier.fillMaxWidth()
    ) {
        items(logs) { log ->
            LogListItem(
                timestamp = log.timestamp,
                level = log.getLevelString(),
                callerId = log.callerId,
                message = log.message ?: "",
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.card_padding))
            )
        }
    }
}

@Composable
private fun LogListItem(
    timestamp: Long,
    level: String,
    callerId: String?,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.card_padding),
                vertical = dimensionResource(R.dimen.margin_xsmall)
            )
        ) {
            LogItemTitle(
                timestamp, level, callerId, Modifier
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LogItemTitle(
    timestamp: Long,
    level: String,
    callerId: String?,
    modifier: Modifier = Modifier
) {
    // return "[$level] $ts - Job #${item.callerId}"
    val ts = timestampToString(timestamp, "dd-MM HH:mm:ss")

    val text = buildAnnotatedString {

        val bgColor = when (level) {
            AppNames.ERROR -> CellsColor.danger
            AppNames.WARNING -> CellsColor.warning
            AppNames.DEBUG -> CellsColor.debug
            else -> CellsColor.info
        }
        val frontColor = contentColorFor(bgColor)

        append(ts)
        append(" ")
        withStyle(style = SpanStyle(background = bgColor, color = frontColor)) {
            append(" ${level.uppercase()} ")
        }
        append(" ")
        // append(stringResource(R.string.read_time, post.metadata.readTimeMinutes))
        append("Job: ${callerId ?: "-"}")
    }
    Text(
        text = text, style = MaterialTheme.typography.bodyMedium, modifier = modifier
    )
}

@Preview
@Composable
private fun LogListItemPreview(
) {
    UseCellsTheme {
        LogListItem(0L, AppNames.ERROR, "0xcafe-babe-babecafe", "status", Modifier)
    }
}
