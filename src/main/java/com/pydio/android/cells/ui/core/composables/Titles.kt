package com.pydio.android.cells.ui.core.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.cells.utils.Str

@Composable
fun DialogTitle(
    icon: ImageVector?,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_inner_padding)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {

        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier
                .weight(1f)
                .wrapContentWidth(Alignment.Start)
//                .padding(horizontal = dimensionResource(R.dimen.margin_xsmall))
                .alpha(.8f)
        )
    }
}

@Composable
fun DefaultTitleText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier
            .wrapContentWidth(Alignment.Start)
            .alpha(.8f)
    )
}

@Composable
fun MainTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text.uppercase(),
        color = color,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .paddingFromBaseline(top = 32.dp, bottom = 8.dp)
            .alpha(.9f)
    )
}

@Composable
fun MenuTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text.uppercase(),
        color = color,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
//            .paddingFromBaseline(top = 36.dp, bottom = 6.dp)
//            .alpha(.9f)
    )
}


@Composable
fun TitleDescColumnBloc(
    title: String,
    desc: String?,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .alpha(.9f)
            .padding(top = dimensionResource(R.dimen.margin_header))
    )

    if (Str.notEmpty(desc)) {
        Text(
            text = desc!!,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
                .alpha(.9f)
                .padding(bottom = dimensionResource(R.dimen.margin_header))
        )
    }
}
