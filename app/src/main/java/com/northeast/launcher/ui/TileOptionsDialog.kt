package com.northeast.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.northeast.launcher.apps.LaunchableApp
import com.northeast.launcher.ui.theme.LocalAccent
import com.northeast.launcher.ui.theme.Woodland

/** Hold-OK menu for a home tile. */
@Composable
fun TileOptionsDialog(
    app: LaunchableApp,
    onMove: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .background(Woodland.SkyBottom, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(app.label, style = MaterialTheme.typography.titleLarge, color = Woodland.Birch, maxLines = 1)
            val colors = ButtonDefaults.colors(
                containerColor = Woodland.Bark,
                contentColor = Woodland.Birch,
                focusedContainerColor = LocalAccent.current,
                focusedContentColor = Woodland.SkyTop,
            )
            Button(onClick = onMove, colors = colors, modifier = Modifier.width(240.dp).focusRequester(first)) {
                Text("Move")
            }
            Button(onClick = onRemove, colors = colors, modifier = Modifier.width(240.dp)) {
                Text("Remove from home")
            }
            Button(onClick = onDismiss, colors = colors, modifier = Modifier.width(240.dp)) {
                Text("Cancel")
            }
        }
    }
}
