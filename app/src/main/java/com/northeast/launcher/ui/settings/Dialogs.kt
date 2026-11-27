package com.northeast.launcher.ui.settings

import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.northeast.launcher.data.AppKey
import com.northeast.launcher.data.Pin
import com.northeast.launcher.home.HomeButtonService
import com.northeast.launcher.home.KeyLog
import com.northeast.launcher.ui.LauncherState
import com.northeast.launcher.ui.Scenery
import com.northeast.launcher.ui.theme.LocalAccent
import com.northeast.launcher.ui.theme.Woodland

/** A full-screen sub-screen drawn over the current one. Back calls [onBack]. */
@Composable
fun FullScreenDialog(onBack: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize()) { Scenery(content) }
    }
}

@Composable
private fun woodlandButtonColors() = ButtonDefaults.colors(
    containerColor = Woodland.Bark,
    contentColor = Woodland.Birch,
    focusedContainerColor = LocalAccent.current,
    focusedContentColor = Woodland.SkyTop,
)

@Composable
private fun Title(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, color = Woodland.Birch)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Woodland.BirchMuted)
}

// ---------------------------------------------------------------------------------------------
// Arrange apps
// ---------------------------------------------------------------------------------------------

/** Vertical list of home apps. OK picks one up, ▲ ▼ move it, OK or Back puts it down. */
@Composable
fun ArrangeDialog(state: LauncherState, onDone: () -> Unit) {
    var moving by remember { mutableStateOf<AppKey?>(null) }
    val pinned = state.pinned
    val listState = rememberLazyListState()
    val requesters = remember { HashMap<AppKey, FocusRequester>() }
    fun requesterFor(key: AppKey) = requesters.getOrPut(key) { FocusRequester() }

    LaunchedEffect(Unit) { pinned.firstOrNull()?.let { runCatching { requesterFor(it.key).requestFocus() } } }
    LaunchedEffect(moving, pinned.map { it.key }) {
        val key = moving ?: return@LaunchedEffect
        val index = pinned.indexOfFirst { it.key == key }
        if (index >= 0) {
            listState.scrollToItem((index - 2).coerceAtLeast(0))
            withFrameNanos { }
            runCatching { requesterFor(key).requestFocus() }
        }
    }

    FullScreenDialog(onBack = { if (moving != null) moving = null else onDone() }) {
        Column(Modifier.fillMaxSize().padding(start = 56.dp, top = 40.dp, end = 56.dp)) {
            Title(
                "Arrange home screen apps",
                if (moving == null) "OK picks up an app. Back when you're done." else "▲ ▼ to move  ·  OK to put it down",
            )
            if (pinned.isEmpty()) {
                Text("No apps on the home screen yet.", color = Woodland.BirchMuted, modifier = Modifier.padding(top = 24.dp))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.width(640.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(pinned, key = { _, app -> app.key.flatten() }) { index, app ->
                    val isMoving = app.key == moving
                    SettingRow(
                        title = "${index + 1}.  ${app.label}",
                        value = if (isMoving) "▲ ▼  moving" else null,
                        onClick = { moving = if (isMoving) null else app.key },
                        modifier = Modifier
                            .focusRequester(requesterFor(app.key))
                            .onPreviewKeyEvent { event ->
                                if (!isMoving || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_UP -> { state.moveEarlier(app); true }
                                    KeyEvent.KEYCODE_DPAD_DOWN -> { state.moveLater(app); true }
                                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> true
                                    else -> false
                                }
                            },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Remote button test
// ---------------------------------------------------------------------------------------------

/**
 * Lists the buttons pressed and the key codes they send. Keys the system keeps from apps
 * (Home, and on some remotes others) only show up when the Home-button service is on.
 */
@Composable
fun RemoteTestDialog(state: LauncherState, onDone: () -> Unit) {
    val context = LocalContext.current
    val serviceOn = remember { HomeButtonService.isEnabled(context) }
    DisposableEffect(Unit) {
        KeyLog.startTest()
        onDispose { KeyLog.stopTest() }
    }
    val records by KeyLog.records.collectAsState()
    val candidate = records.firstOrNull { !KeyLog.isReserved(it.keyCode) }
    val doneButton = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { doneButton.requestFocus() } }

    FullScreenDialog(onBack = onDone) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 56.dp, top = 40.dp, end = 56.dp)
                .onPreviewKeyEvent { event ->
                    KeyLog.record(event.nativeKeyEvent, viaService = false)
                    false // never block: the remote must still move around this screen
                },
        ) {
            Title(
                "Remote button test",
                "Press any button on your remote. Back leaves." +
                    if (serviceOn) "" else "\nTurn on the Home-button service to also see Home and other system buttons.",
            )
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onDone, colors = woodlandButtonColors(), modifier = Modifier.focusRequester(doneButton)) {
                    Text("Done")
                }
                if (candidate != null) {
                    Button(
                        onClick = { state.updateSettings { it.copy(extraHomeKey = candidate.keyCode) } },
                        colors = woodlandButtonColors(),
                    ) {
                        val current = state.settings.extraHomeKey == candidate.keyCode
                        Text(if (current) "${candidate.name} is your extra Home button" else "Use ${candidate.name} as an extra Home button")
                    }
                }
            }
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (records.isEmpty()) {
                    Text("Waiting for a button…", color = Woodland.BirchFaint, style = MaterialTheme.typography.titleMedium)
                }
                records.forEachIndexed { i, r ->
                    Row(
                        Modifier
                            .width(760.dp)
                            .background(Woodland.Bark.copy(alpha = if (i == 0) 0.9f else 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium, color = if (i == 0) LocalAccent.current else Woodland.Birch, modifier = Modifier.width(300.dp))
                        Text("code ${r.keyCode}", fontFamily = FontFamily.Monospace, color = Woodland.BirchMuted, modifier = Modifier.width(140.dp))
                        Text("scan ${r.scanCode}", fontFamily = FontFamily.Monospace, color = Woodland.BirchMuted, modifier = Modifier.width(140.dp))
                        if (r.viaService) Text("via service", color = Woodland.BirchFaint)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Home button setup
// ---------------------------------------------------------------------------------------------

@Composable
fun HomeButtonDialog(state: LauncherState, onDone: () -> Unit) {
    val context = LocalContext.current
    val serviceOn = HomeButtonService.isEnabled(context)
    val commands = remember { HomeButtonService.adbEnableCommands(context) }
    val doneButton = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { doneButton.requestFocus() } }

    FullScreenDialog(onBack = onDone) {
        Column(
            Modifier.fillMaxSize().padding(start = 56.dp, top = 40.dp, end = 72.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Title(
                "Home button setup",
                "Google TV keeps the Home button for its own home screen. NorthEast's small Home-button " +
                    "service takes it back. It only listens for the Home button (and an extra button if you " +
                    "choose one). It can't see your screen and nothing leaves the TV.",
            )
            StatusLine("Home-button service", if (serviceOn) "On" else "Off", serviceOn)
            StatusLine("Takeover setting", if (state.settings.homeTakeover) "On" else "Off", state.settings.homeTakeover)
            if (!serviceOn) {
                Text("Turn it on", style = MaterialTheme.typography.titleLarge, color = Woodland.Birch, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Option A — on the TV: Settings → System → Accessibility → NorthEast Home button → On. " +
                        "Many Google TV versions block this for sideloaded apps (the switch is greyed out). If so, use option B.",
                    color = Woodland.BirchMuted,
                )
                Text("Option B — from your computer, with the TV connected over ADB:", color = Woodland.BirchMuted)
                commands.forEach { cmd ->
                    Text(
                        cmd,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = Woodland.Birch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Woodland.Bark.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = onDone, colors = woodlandButtonColors(), modifier = Modifier.focusRequester(doneButton)) { Text("Done") }
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    },
                    colors = woodlandButtonColors(),
                ) { Text("Open accessibility settings") }
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String, good: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(if (good) LocalAccent.current else Woodland.BirchFaint, RoundedCornerShape(6.dp)))
        Text("$label:", color = Woodland.BirchMuted, style = MaterialTheme.typography.titleMedium)
        Text(value, color = Woodland.Birch, style = MaterialTheme.typography.titleMedium)
    }
}

// ---------------------------------------------------------------------------------------------
// PIN
// ---------------------------------------------------------------------------------------------

/** Asks for the settings PIN. Number buttons on the remote work too. */
@Composable
fun PinUnlockDialog(storedHash: String, onUnlocked: () -> Unit, onCancel: () -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    PinPad(
        title = "Enter settings PIN",
        message = error ?: "Settings are locked.",
        onComplete = { pin ->
            if (Pin.verify(pin, storedHash)) onUnlocked() else error = "That PIN isn't right. Try again."
        },
        onCancel = onCancel,
    )
}

/** Asks for a new PIN twice, then calls [onSet] with the stored form. */
@Composable
fun PinSetDialog(onSet: (String) -> Unit, onCancel: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    PinPad(
        title = if (first == null) "Choose a 4-digit PIN" else "Enter it again",
        message = error ?: if (first == null) {
            "You'll need it to open settings. If it's ever forgotten, see docs/TROUBLESHOOTING.md."
        } else {
            "Confirm your new PIN."
        },
        onComplete = { pin ->
            val firstPin = first
            when {
                firstPin == null -> { first = pin; error = null }
                firstPin == pin -> onSet(Pin.create(pin))
                else -> { first = null; error = "The PINs didn't match. Start again." }
            }
        },
        onCancel = onCancel,
        resetKey = first,
    )
}

@Composable
private fun PinPad(
    title: String,
    message: String,
    onComplete: (String) -> Unit,
    onCancel: () -> Unit,
    resetKey: Any? = null,
) {
    var digits by remember(resetKey) { mutableStateOf("") }
    // Clear the dots after a wrong attempt so the next try starts fresh.
    LaunchedEffect(message) { digits = "" }
    fun press(d: Char) {
        if (digits.length >= Pin.LENGTH) return
        digits += d
        if (digits.length == Pin.LENGTH) {
            val entered = digits
            digits = ""
            onComplete(entered)
        }
    }
    fun backspace() { digits = digits.dropLast(1) }
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }

    FullScreenDialog(onBack = onCancel) {
        Column(
            Modifier
                .align(Alignment.Center)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val code = event.nativeKeyEvent.keyCode
                    when (code) {
                        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> { press('0' + (code - KeyEvent.KEYCODE_0)); true }
                        KeyEvent.KEYCODE_DEL -> { backspace(); true }
                        else -> false
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Woodland.Birch)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Woodland.BirchMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(Pin.LENGTH) { i ->
                    Box(
                        Modifier
                            .size(18.dp)
                            .background(if (i < digits.length) LocalAccent.current else Woodland.Bark, RoundedCornerShape(9.dp)),
                    )
                }
            }
            val rows = listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9'), listOf('⌫', '0', '✕'))
            rows.forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { c, key ->
                        Button(
                            onClick = {
                                when (key) {
                                    '⌫' -> backspace()
                                    '✕' -> onCancel()
                                    else -> press(key)
                                }
                            },
                            colors = woodlandButtonColors(),
                            modifier = Modifier
                                .width(88.dp)
                                .then(if (r == 0 && c == 0) Modifier.focusRequester(firstKey) else Modifier),
                        ) {
                            Text(
                                when (key) {
                                    '⌫' -> "Delete"
                                    '✕' -> "Cancel"
                                    else -> key.toString()
                                },
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Opens the system's own settings app (the stock launcher's gear may be unreachable). */
fun openTvSettings(context: android.content.Context): Boolean = runCatching {
    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}.isSuccess
