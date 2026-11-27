package com.northeast.launcher.home

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.northeast.launcher.MainActivity
import com.northeast.launcher.data.SettingsStore
import com.northeast.launcher.data.SharedPrefsKeyValueStore

/**
 * Takes over the remote's Home button on Google TV, where the stock launcher otherwise
 * keeps it even after another launcher is chosen (docs/FEASIBILITY.md §3).
 *
 * It only filters key events: it cannot read the screen (canRetrieveWindowContent=false)
 * and subscribes to no accessibility events. It acts on Home and, optionally, one extra
 * remote button chosen in Settings → Remote button test. Everything else passes through.
 */
class HomeButtonService : AccessibilityService() {

    private val settings by lazy { SettingsStore(SharedPrefsKeyValueStore(this)) }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (KeyLog.testing) {
            KeyLog.record(event, viaService = true)
            // Swallow Home during the test so it shows up in the list instead of leaving.
            return event.keyCode == KeyEvent.KEYCODE_HOME
        }
        val current = settings.load()
        if (!current.homeTakeover) return false
        val isHomeKey = event.keyCode == KeyEvent.KEYCODE_HOME ||
            (current.extraHomeKey != 0 && event.keyCode == current.extraHomeKey)
        if (!isHomeKey) return false
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) goHome()
        return true
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setClass(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(intent)
        } catch (_: RuntimeException) {
            // Never crash the service: if this fails the system's own Home behaviour is all that's lost.
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        fun component(context: Context) = ComponentName(context, HomeButtonService::class.java)

        /** Whether the user (or ADB) has turned this service on in the system's accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val me = component(context)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        /** The ADB commands that turn the service on, keeping any other accessibility services. */
        fun adbEnableCommands(context: Context): List<String> {
            val me = component(context).flattenToString()
            return listOf(
                "adb shell settings put secure enabled_accessibility_services " +
                    "\"\$(adb shell settings get secure enabled_accessibility_services | sed 's/^null\$//' | tr -d '\\r'):$me\"",
                "adb shell settings put secure accessibility_enabled 1",
            )
        }
    }
}
