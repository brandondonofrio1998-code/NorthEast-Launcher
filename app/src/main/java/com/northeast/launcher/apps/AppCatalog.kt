package com.northeast.launcher.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.northeast.launcher.data.AppKey

/** An installed app the launcher can show and open. */
data class LaunchableApp(
    val key: AppKey,
    val label: String,
    /** True if the app declares a TV (leanback) launch activity. */
    val isTvApp: Boolean,
)

/**
 * Reads installed launchable apps from the system and opens them.
 * Everything here is local PackageManager IPC — no network.
 */
class AppCatalog(context: Context) {

    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val ownPackage = appContext.packageName

    /** Small in-memory icon cache; icons are re-decoded only after process death. */
    private val iconCache = LruCache<AppKey, ImageBitmap>(64)

    /**
     * All launchable apps, TV apps preferred. Apps that only have a phone launcher entry are
     * included too (some sideloaded apps), but a package's TV activity wins if it has both.
     * Call off the main thread.
     */
    fun loadInstalled(): List<LaunchableApp> {
        val tv = query(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val phone = query(Intent.CATEGORY_LAUNCHER)
        val byPackage = LinkedHashMap<String, LaunchableApp>()
        tv.forEach { byPackage.putIfAbsent(it.activityInfo.packageName, toApp(it, isTv = true)) }
        phone.forEach { byPackage.putIfAbsent(it.activityInfo.packageName, toApp(it, isTv = false)) }
        return byPackage.values
            .filter { it.key.packageName != ownPackage && !it.key.packageName.startsWith("$ownPackage.") }
            .sortedBy { it.label.lowercase() }
    }

    /** Decodes the app's icon at [sizePx]. Call off the main thread. Returns null if unavailable. */
    fun loadIcon(key: AppKey, sizePx: Int): ImageBitmap? {
        iconCache.get(key)?.let { return it }
        val bitmap = try {
            pm.getActivityIcon(ComponentName(key.packageName, key.activityName))
                .toBitmap(sizePx, sizePx)
                .asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        iconCache.put(key, bitmap)
        return bitmap
    }

    /** Opens the app. Returns false if it could not be started (e.g. just uninstalled). */
    fun launch(context: Context, app: LaunchableApp): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(if (app.isTvApp) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.key.packageName, app.key.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun query(category: String): List<ResolveInfo> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)

    private fun toApp(info: ResolveInfo, isTv: Boolean): LaunchableApp {
        val activity = info.activityInfo
        return LaunchableApp(
            key = AppKey(activity.packageName, activity.name),
            label = info.loadLabel(pm).toString().trim().ifEmpty { activity.packageName },
            isTvApp = isTv,
        )
    }
}
