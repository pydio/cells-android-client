package com.pydio.android.cells.ui.core.composables

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.UseCellsTheme

@Composable
fun PreferenceSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    desc: String? = null,
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    desc?.let {
        Text(
            text = it,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
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
    val selectedIndex = keys.indexOfFirst { it == currKey }

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
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small)),
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
            enabled = isEnabled,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputMegabytes(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {

    val focusManager = LocalFocusManager.current
    // var isTextFieldFocused by remember { mutableStateOf(false) }

//    Column(modifier = modifier) {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.titleSmall,
//            color = MaterialTheme.colorScheme.onSurface
//        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = isEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Define a threshold, in MB") },
            modifier = Modifier.fillMaxWidth(.8f),
        )
        TextButton(
            onClick = { focusManager.clearFocus() },
            enabled = isEnabled,
        ) {
            Text("OK")
        }
    }
//    }
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
