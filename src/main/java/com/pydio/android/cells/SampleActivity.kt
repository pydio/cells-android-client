package com.pydio.android.cells

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.pydio.android.cells.ui.browse.screens.rotateBy
import com.pydio.android.cells.ui.theme.UseCellsTheme

/**
 * Test new components should not be enabled when shipping to production
 */
class SampleActivity : ComponentActivity() {

    private val logTag = "SampleActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate")
        super.onCreate(savedInstanceState)
        setContent {
            UseCellsTheme {
                // ComponentExamplePage()
                PagerExamplePage()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerExamplePage(
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Icons.Outlined.Person,
        Icons.Outlined.RocketLaunch,
        Icons.Outlined.PhotoCamera,
        Icons.Outlined.Refresh,
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { items.size }
    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        key = { it }
    ) { page -> OneImage(items, page) }
}

@Composable
private fun OneImage(
    items: List<ImageVector>,
    page: Int
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var angle by remember { mutableFloatStateOf(0f) }

//    val modifierNoGesture = Modifier
//        .fillMaxSize()
//        .background(MaterialTheme.colorScheme.surfaceVariant)

    val imageModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .pointerInput(Unit) {
            detectTransformGestures(
                onGesture = { gestureCentroid, gesturePan, gestureZoom, gestureRotate ->
                    val oldScale = zoom
                    val newScale = (zoom * gestureZoom).coerceIn(1f..10f)

                    offset = (offset + gestureCentroid / oldScale).rotateBy(gestureRotate) -
                            (gestureCentroid / newScale + gesturePan / oldScale)
                    angle += gestureRotate
                    zoom = newScale
                }
            )
        }
        .graphicsLayer {
            translationX = -offset.x * zoom
            translationY = -offset.y * zoom
            scaleX = zoom
            scaleY = zoom
            rotationZ = angle
            TransformOrigin(0f, 0f).also { transformOrigin = it }
        }

    Icon(
        imageVector = items[page],
        contentDescription = "",
        modifier = imageModifier
        //         modifier = modifierNoGesture
    )
}

@Composable
private fun ComponentExamplePage() {
    val ctx = LocalContext.current
    val showDialog1 = remember { mutableStateOf(false) }
    val itemModifier = Modifier
        .fillMaxWidth()
        .padding(
            horizontal = dimensionResource(R.dimen.list_horizontal_padding),
            vertical = dimensionResource(R.dimen.list_vertical_padding)
        )
        .wrapContentWidth(Alignment.CenterHorizontally)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.margin_large))
    ) {
        Text(
            text = "Sample Page to test new Material3 components with compose",
            modifier = itemModifier
        )
        Button(
            onClick = {
                Toast
                    .makeText(ctx, "This is a toast", Toast.LENGTH_LONG)
                    .show()
            },
            modifier = itemModifier
        ) {
            Text(text = "Show Toast")
        }
        Button(
            onClick = { showDialog1.value = true },
            modifier = itemModifier,
        )
        { Text(text = "Show Dialog") }
    }
    if (showDialog1.value) {
        Dialog1 { showDialog1.value = false }
    }
}

@Composable
fun Dialog1(dismiss: () -> Unit) {
    val ctx = LocalContext.current
    val textValue = remember {
        mutableStateOf("")
    }
    val updateValue: (String) -> Unit = { textValue.value = it }
    AlertDialog(
        title = { Text("Alert Dialog") },
        text = { DialogContent(textValue.value, updateValue) },
        confirmButton = {
            TextButton(
                onClick = {
                    Toast.makeText(ctx, "OK button clicked", Toast.LENGTH_LONG).show()
                    dismiss()
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    Toast.makeText(ctx, "Cancel button clicked!!", Toast.LENGTH_LONG).show()
                    dismiss()
                }
            ) { Text("Cancel") }
        },
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            securePolicy = SecureFlagPolicy.Inherit
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogContent(value: String, updateValue: (String) -> Unit) {
    Column {
        Text("Dialog Content.\n**WARNING**: you are about to click")
        TextField(value = value, onValueChange = { updateValue(it) })
    }
}
