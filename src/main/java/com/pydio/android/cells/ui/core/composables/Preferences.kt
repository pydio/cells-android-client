package com.pydio.android.cells.ui.core.composables

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.UseCellsTheme

@Composable
fun PreferenceSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun ListSetting(
    label: String,
    currKey: String,
    keys: Array<String>,
    labels: Array<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }

    var selectedIndex = keys.indexOfFirst { it == currKey }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {

        Column(
            modifier = modifier.clickable(onClick = { expanded = true }),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (selectedIndex >= 0) labels[selectedIndex] else "-",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
        ) {
            keys.forEachIndexed { index, s ->
                DropdownMenuItem(
                    text = { Text(text = labels[index]) },
                    onClick = {
                        onSelect(s)
                        expanded = false
                    })
            }
        }
    }
}

@Composable
fun SwitchSetting(
    label: String,
    description: String?,
    isSelected: Boolean,
    onItemClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Switch(
            modifier = Modifier.semantics { contentDescription = label },
            checked = isSelected,
            onCheckedChange = { onItemClick(!isSelected) }
        )
    }

}

@Composable
fun TextSetting(label: String, currValue: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface

        )
        Text(
            text = currValue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PreferenceDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Divider(
        modifier = modifier.fillMaxWidth(),
        color = color.copy(alpha = .6f),
        thickness = 1.dp,
    )
}

@Preview(name = "Light Mode Item")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode Item"
)
@Composable
private fun PreferenceItemPreview() {

    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = dimensionResource(id = R.dimen.margin_small))
    UseCellsTheme {
        Column(
            horizontalAlignment = Alignment.Start,
        ) {

            PreferenceSectionTitle("Customise List", modifier)
//            ListSetting("Sort Order", "Modified (newest first)", modifier)
            SwitchSetting(
                "Download thumbs",
                "Also retrieve thumbnails when on a metered network",
                true,
                {},
                modifier,
            )
            TextSetting(label = "When file is greater than (in MB)", currValue = "2")
        }
    }
}

@Preview(name = "Light Mode List")
@Composable
private fun PreferenceListPreview() {
    UseCellsTheme {
//         PreferenceList() {}
    }
}
