package com.northeast.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import com.northeast.launcher.apps.AppCatalog
import com.northeast.launcher.data.PinnedAppsRepository
import com.northeast.launcher.data.SettingsStore
import com.northeast.launcher.data.SharedPrefsKeyValueStore
import com.northeast.launcher.ui.LauncherApp
import com.northeast.launcher.ui.LauncherState

/** The HOME activity. Holds the launcher state and tells the UI when to refresh. */
class MainActivity : ComponentActivity() {

    private lateinit var state: LauncherState
    private val refreshRequests = mutableIntStateOf(0)
    private val homePresses = mutableIntStateOf(0)
    private val returns = mutableIntStateOf(0)

    /** Installs, uninstalls and updates change the app list; refresh when they happen. */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshRequests.intValue++
        }
    }
    private var receiverRegistered = false
    private var startedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SharedPrefsKeyValueStore(this)
        state = LauncherState(
            catalog = AppCatalog(this),
            repository = PinnedAppsRepository(store),
            settingsStore = SettingsStore(store),
        )
        setContent {
            LauncherApp(
                state = state,
                refreshRequests = refreshRequests.intValue,
                homePresses = homePresses.intValue,
                returns = returns.intValue,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
        // Catch anything that changed while another app was in front.
        refreshRequests.intValue++
        // Coming back from an app (not the first start): lets "Return to last app" decide focus.
        if (startedOnce) returns.intValue++
        startedOnce = true
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(packageReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.intValue++
    }
}
