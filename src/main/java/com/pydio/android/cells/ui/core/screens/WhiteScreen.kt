package com.pydio.android.cells.ui.core.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.pydio.android.cells.R

private const val SplashWaitTime: Long = 2000

@Composable
fun WhiteScreen() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
//        Image(painterResource(id = R.drawable.pydio_logo), contentDescription = null)
    }
}
