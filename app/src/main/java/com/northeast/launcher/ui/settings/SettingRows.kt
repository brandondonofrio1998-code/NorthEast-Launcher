package com.northeast.launcher.ui.settings

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.northeast.launcher.ui.theme.LocalAccent
import com.northeast.launcher.ui.theme.Woodland

private val RowShape = RoundedCornerShape(10.dp)

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = LocalAccent.current,
        modifier = Modifier.padding(start = 8.dp, top = 22.dp, bottom = 6.dp),
    )
}

/**
 * One settings row. OK runs [onClick]; ◀ ▶ run [onLeft]/[onRight] when given (used to step
 * through choices). [value] is shown on the right.
 */
@Composable
fun SettingRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onFocus: (() -> Unit)? = null,
) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .onFocusChanged { if (it.isFocused) onFocus?.invoke() }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> onLeft?.let { it(); true } ?: false
                    KeyEvent.KEYCODE_DPAD_RIGHT -> onRight?.let { it(); true } ?: false
                    else -> false
                }
            },
        shape = ClickableSurfaceDefaults.shape(shape = RowShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Woodland.Bark.copy(alpha = 0.55f),
            contentColor = Woodland.Birch,
            focusedContainerColor = Woodland.BarkFocused,
            focusedContentColor = Woodland.Birch,
            pressedContainerColor = Woodland.BarkDim,
            pressedContentColor = Woodland.Birch,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, accent), shape = RowShape),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = Woodland.BirchMuted)
                }
            }
            if (value != null) {
                Text(
                    text = if (focused && (onLeft != null || onRight != null)) "‹  $value  ›" else value,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (focused) accent else Woodland.BirchMuted,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/** A setting with a fixed list of options. OK and ▶ go to the next option, ◀ to the previous. */
@Composable
fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    description: String? = null,
    onFocus: (() -> Unit)? = null,
) {
    val index = options.indexOf(selected).coerceAtLeast(0)
    fun pick(delta: Int) = onSelect(options[(index + delta).mod(options.size)])
    SettingRow(
        title = title,
        value = label(selected),
        description = description,
        onClick = { pick(+1) },
        onLeft = { pick(-1) },
        onRight = { pick(+1) },
        onFocus = onFocus,
    )
}

@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    description: String? = null,
    onFocus: (() -> Unit)? = null,
) {
    SettingRow(
        title = title,
        value = if (checked) "On" else "Off",
        description = description,
        onClick = { onChange(!checked) },
        onLeft = { onChange(!checked) },
        onRight = { onChange(!checked) },
        onFocus = onFocus,
    )
}

/**
 * Runs an action. With [confirm], the first OK only arms it ("Press OK again to confirm")
 * and the second runs it; moving away disarms it.
 */
@Composable
fun ActionRow(
    title: String,
    onClick: () -> Unit,
    description: String? = null,
    value: String? = "›",
    confirm: Boolean = false,
    onFocus: (() -> Unit)? = null,
) {
    var armed by remember { mutableStateOf(false) }
    SettingRow(
        title = title,
        value = if (armed) "Press OK again to confirm" else value,
        description = description,
        onClick = {
            if (!confirm || armed) {
                armed = false
                onClick()
            } else {
                armed = true
            }
        },
        modifier = Modifier.onFocusChanged { if (!it.hasFocus) armed = false },
        onFocus = onFocus,
    )
}
