package com.northeast.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.northeast.launcher.R
import com.northeast.launcher.ui.theme.Woodland
import kotlinx.coroutines.delay
import java.util.Date

private val skyBrush = Brush.verticalGradient(listOf(Woodland.SkyTop, Woodland.SkyBottom))

/**
 * The static woodland backdrop: spruce-green sky and the pine ridge along the bottom.
 * Nothing here animates; the vector is rasterized once and cached by Compose.
 */
@Composable
fun Scenery(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyBrush),
    ) {
        Image(
            painter = painterResource(R.drawable.pine_ridge),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        )
        content()
    }
}

/** Current time, in the device's 12/24-hour setting. Ticks once a minute, on the minute. */
@Composable
fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val format = remember(context) { DateFormat.getTimeFormat(context) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L)
        }
    }
    Text(
        text = format.format(Date(now)),
        style = MaterialTheme.typography.headlineSmall,
        color = Woodland.Birch,
        modifier = modifier,
    )
}
