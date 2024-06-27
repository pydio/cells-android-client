package com.pydio.android.cells.ui.core.composables.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SmoothLinearProgressIndicator(
    indicatorProgress: Float,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val progressAnimDuration = 1000 // we update progress in the db every second
    val progressAnimation by animateFloatAsState(
        label = "Progress animation",
        targetValue = indicatorProgress,
        animationSpec = tween(durationMillis = progressAnimDuration, easing = FastOutSlowInEasing)
    )
    LinearProgressIndicator(
        modifier = modifier,
        progress = { progressAnimation }
    )
    LaunchedEffect(indicatorProgress) {
        progress = indicatorProgress
    }
}
