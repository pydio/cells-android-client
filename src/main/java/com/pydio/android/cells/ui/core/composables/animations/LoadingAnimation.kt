package com.pydio.android.cells.ui.core.composables.animations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsIcons

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    circleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = .3f),
    animationDelay: Int = 2000
) {

    var rotation = remember { mutableStateOf(0f) } // starting point

    val rotationAnimate = animateFloatAsState(
        targetValue = rotation.value,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDelay
            )
        )
    )

    LaunchedEffect(Unit) {    // Initialise target
        rotation.value = 360f
    }

    Box(
        modifier = modifier
            .padding(all = dimensionResource(id = R.dimen.list_thumb_padding))
            .wrapContentSize(Alignment.Center)
    ) {
        Icon(
            imageVector = CellsIcons.Refresh,
            contentDescription = "",
            tint = circleColor,
            modifier = Modifier
//                .size(size = 24.dp)
                .rotate(rotationAnimate.value)
        )
    }
}