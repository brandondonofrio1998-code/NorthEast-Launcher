package com.northeast.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * M1 scaffold. Proves the app builds, installs and registers as a HOME launcher.
 * The real home screen (pinned-app grid, woodland theme) replaces this in M2.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ScaffoldScreen() }
    }
}

@Composable
private fun ScaffoldScreen() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF16241C)),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "NorthEast Launcher",
                style = MaterialTheme.typography.displaySmall,
                color = Color(0xFFF2EFE6),
            )
            Text(
                text = "Scaffold build — the home screen arrives in M2.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB9C2B4),
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ScaffoldScreenPreview() = ScaffoldScreen()
