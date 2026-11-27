package com.northeast.launcher.ui

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.northeast.launcher.R
import com.northeast.launcher.data.ClockFormat
import com.northeast.launcher.data.HomeRules
import com.northeast.launcher.data.LauncherSettings
import com.northeast.launcher.data.SceneryMode
import com.northeast.launcher.data.Season
import com.northeast.launcher.ui.theme.LocalLook
import com.northeast.launcher.ui.theme.Woodland
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The static woodland backdrop: background and up to three tinted pine-ridge layers.
 * Nothing animates; vectors are rasterized once and cached by Compose.
 * [content] is inset by the "Screen edge padding" setting for TVs that crop the picture.
 */
@Composable
fun Scenery(content: @Composable BoxScope.() -> Unit) {
    val look = LocalLook.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(look.background),
    ) {
        when (look.scenery) {
            SceneryMode.FULL -> {
                RidgeLayer(R.drawable.pine_ridge_far, look.ridge.far)
                RidgeLayer(R.drawable.pine_ridge_mid, look.ridge.mid)
                RidgeLayer(R.drawable.pine_ridge_near, look.ridge.near)
            }
            SceneryMode.FRONT_ONLY -> RidgeLayer(R.drawable.pine_ridge_near, look.ridge.near)
            SceneryMode.NONE -> Unit
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(look.edgePadding),
            content = content,
        )
    }
}

@Composable
private fun BoxScope.RidgeLayer(@DrawableRes res: Int, color: Color) {
    val tint = remember(color) { ColorFilter.tint(color) }
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        colorFilter = tint,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
    )
}

/** Wall-clock time that updates once a minute, on the minute. One ticker drives the clock, dimming and season. */
@Composable
fun rememberMinuteTicker(): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.longValue = System.currentTimeMillis()
            delay(60_000L - now.longValue % 60_000L)
        }
    }
    return now
}

/** Clock, date, greeting and season name (each optional), top-right. */
@Composable
fun TopInfo(nowMillis: Long, settings: LauncherSettings, season: Season, modifier: Modifier = Modifier) {
    if (!settings.showClock && !settings.showDate && !settings.showGreeting && !settings.showSeasonName) return
    val context = LocalContext.current
    val timeFormat = remember(context, settings.clockFormat) {
        when (settings.clockFormat) {
            ClockFormat.SYSTEM -> DateFormat.getTimeFormat(context)
            ClockFormat.H12 -> SimpleDateFormat("h:mm a", Locale.getDefault())
            ClockFormat.H24 -> SimpleDateFormat("HH:mm", Locale.getDefault())
        }
    }
    val dateFormat = remember {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd"), Locale.getDefault())
    }
    val date = Date(nowMillis)
    val hour = remember(nowMillis) { Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY) }
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        if (settings.showClock) {
            Text(timeFormat.format(date), style = MaterialTheme.typography.headlineSmall, color = Woodland.Birch)
        }
        if (settings.showDate) {
            Text(dateFormat.format(date), style = MaterialTheme.typography.titleSmall, color = Woodland.BirchMuted)
        }
        if (settings.showGreeting) {
            Text(HomeRules.greeting(hour), style = MaterialTheme.typography.titleSmall, color = Woodland.BirchMuted)
        }
        if (settings.showSeasonName) {
            Text(season.displayName, style = MaterialTheme.typography.labelMedium, color = Woodland.BirchFaint)
        }
    }
}
