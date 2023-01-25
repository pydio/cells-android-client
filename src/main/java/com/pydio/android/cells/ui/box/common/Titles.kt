package com.pydio.android.cells.ui.box.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import com.pydio.android.cells.R

@Composable
fun DefaultTitleText(
    text: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = dimensionResource(R.dimen.card_padding))
        .padding(top = dimensionResource(R.dimen.margin_medium))
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
