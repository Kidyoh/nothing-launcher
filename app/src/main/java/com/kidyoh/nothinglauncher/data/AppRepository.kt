package com.kidyoh.nothinglauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

data class AppInfo(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
) {
    /** Stable key used for favourites: "package/class#userSerial". */
    val key: String get() = "${component.flattenToString()}#${user.hashCode()}"
}

/** Loads every launchable activity (including work profile apps) and keeps the list fresh. */
object AppRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    private var started = false
    private var loadJob: Job? = null
    private lateinit var app: Context

    fun start(context: Context) {
        if (started) return
        started = true
        app = context.applicationContext
        val la = app.getSystemService(LauncherApps::class.java)
        la.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(p: String?, u: UserHandle?) = reload()
            override fun onPackageAdded(p: String?, u: UserHandle?) = reload()
            override fun onPackageChanged(p: String?, u: UserHandle?) = reload()
            override fun onPackagesAvailable(p: Array<out String>?, u: UserHandle?, r: Boolean) = reload()
            override fun onPackagesUnavailable(p: Array<out String>?, u: UserHandle?, r: Boolean) = reload()
        })
        // Re-render icons when the monochrome setting flips.
        scope.launch { Prefs.monoIcons.drop(1).collect { reload() } }
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch { _apps.value = load() }
    }

    private fun load(): List<AppInfo> {
        val la = app.getSystemService(LauncherApps::class.java)
        val um = app.getSystemService(UserManager::class.java)
        val size = (app.resources.displayMetrics.density * 64).toInt()
        val mono = Prefs.monoIcons.value
        val self = app.packageName
        return um.userProfiles.flatMap { user ->
            la.getActivityList(null, user).mapNotNull { info ->
                if (info.componentName.packageName == self) return@mapNotNull null
                runCatching {
                    AppInfo(
                        label = info.label.toString(),
                        component = info.componentName,
                        user = user,
                        icon = IconRenderer.render(info.getIcon(0), size, mono).asImageBitmap(),
                    )
                }.getOrNull()
            }
        }.sortedBy { it.label.lowercase() }
    }

    fun launch(context: Context, app: AppInfo, bounds: Rect? = null) {
        runCatching {
            context.getSystemService(LauncherApps::class.java)
                .startMainActivity(app.component, app.user, bounds, null)
        }
    }

    fun openAppInfo(context: Context, app: AppInfo) {
        runCatching {
            context.getSystemService(LauncherApps::class.java)
                .startAppDetailsActivity(app.component, app.user, null, null)
        }
    }

    fun uninstall(context: Context, app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:${app.component.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Intent.EXTRA_USER, app.user)
        runCatching { context.startActivity(intent) }
    }

    fun byKey(key: String): AppInfo? = _apps.value.firstOrNull { it.key == key }

    /** Picks sensible first-run favourites: phone, messages, camera, browser, etc. */
    fun defaultFavorites(context: Context, apps: List<AppInfo>): List<String> {
        val pm = context.packageManager
        val myUser = Process.myUserHandle()
        fun pkgFor(intent: Intent): String? =
            pm.resolveActivity(intent, 0)?.activityInfo?.packageName?.takeIf { it != "android" }

        val wanted = listOfNotNull(
            pkgFor(Intent(Intent.ACTION_DIAL)),
            pkgFor(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)),
            pkgFor(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)),
            pkgFor(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)),
            pkgFor(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)),
            pkgFor(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)),
            pkgFor(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS)),
            pkgFor(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)),
            pkgFor(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)),
            pkgFor(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS)),
            "com.android.settings",
            "com.google.android.youtube",
            "com.android.vending",
        ).distinct()
        val picked = wanted.mapNotNull { pkg ->
            apps.firstOrNull { it.component.packageName == pkg && it.user == myUser }?.key
        }.toMutableList()
        for (a in apps) {
            if (picked.size >= HOME_SLOTS) break
            if (a.key !in picked && a.user == myUser) picked.add(a.key)
        }
        return picked.take(HOME_SLOTS)
    }

    const val HOME_SLOTS = 10

}
