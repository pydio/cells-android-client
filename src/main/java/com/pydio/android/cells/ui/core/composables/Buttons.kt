package com.pydio.android.cells.ui.core.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsIcons

@Composable
fun SwitchAccountButton(
    openAccounts: () -> Unit,
    modifier: Modifier
) {
    Surface(
        tonalElevation = dimensionResource(R.dimen.switch_account_btn_tonal_elevation),
        shadowElevation = dimensionResource(R.dimen.switch_account_btn_shadow_elevation),
        modifier = modifier
            .clickable { openAccounts() }
            .alpha(.7f)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
    ) {
        Icon(
            imageVector = CellsIcons.SwitchAccount,
            contentDescription = stringResource(R.string.choose_account),
            modifier = Modifier
                .padding(all = dimensionResource(R.dimen.margin_xxsmall))
                .size(dimensionResource(R.dimen.switch_account_btn_size))
        )
    }
}