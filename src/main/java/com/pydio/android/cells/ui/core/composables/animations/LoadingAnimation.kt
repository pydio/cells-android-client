package com.pydio.android.cells.ui.core.composables.animations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsIcons

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    circleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = .3f),
    animationDelay: Int = 2000
) {

    val rotation = remember { mutableFloatStateOf(0f) } // starting point

    val rotationAnimate = animateFloatAsState(
        targetValue = rotation.value,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDelay
            )
        ),
        label = "loading_anim_value",
    )

    LaunchedEffect(Unit) {    // Initialise target
        rotation.value = 360f
    }

    Box(
        modifier = modifier
            .padding(dimensionResource(R.dimen.list_thumb_padding))
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        Icon(
            imageVector = CellsIcons.Refresh,
            contentDescription = stringResource(id = R.string.loading_message),
            tint = circleColor,
            modifier = Modifier.rotate(rotationAnimate.value)
        )
    }
}