package com.kidyoh.nothinglauncher

import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidyoh.nothinglauncher.data.AppRepository
import com.kidyoh.nothinglauncher.data.Prefs
import com.kidyoh.nothinglauncher.service.QuickSettings
import com.kidyoh.nothinglauncher.service.ShadeAccessibilityService
import com.kidyoh.nothinglauncher.ui.components.SheetLayer
import com.kidyoh.nothinglauncher.ui.components.SheetState
import com.kidyoh.nothinglauncher.ui.drawer.AppDrawer
import com.kidyoh.nothinglauncher.ui.home.HomeActions
import com.kidyoh.nothinglauncher.ui.home.HomeScreen
import com.kidyoh.nothinglauncher.ui.home.NothingBackground
import com.kidyoh.nothinglauncher.ui.shade.ShadeHost
import com.kidyoh.nothinglauncher.ui.shade.ShadePanel
import com.kidyoh.nothinglauncher.ui.theme.NothingTheme
import com.kidyoh.nothinglauncher.ui.theme.rememberIsDark

class MainActivity : ComponentActivity(), HomeActions, ShadeHost {

    private lateinit var drawer: SheetState
    private lateinit var shade: SheetState
    private var searchRequest by mutableIntStateOf(0)

    // After the "set as default home app" dialog, continue with the rest of the setup once.
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!Prefs.setupDone) openSettings()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        AppRepository.start(this)

        if (savedInstanceState == null) {
            if (!isDefaultLauncher()) requestDefaultLauncher()
            else if (!Prefs.setupDone) openSettings()
        }

        setContent {
            val scope = rememberCoroutineScope()
            drawer = remember { SheetState(scope) }
            shade = remember { SheetState(scope) }
            NothingTheme {
                val dark = rememberIsDark()
                LaunchedEffect(dark) {
                    val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                }
                val apps by AppRepository.apps.collectAsStateWithLifecycle()
                val systemWallpaper by Prefs.systemWallpaper.collectAsStateWithLifecycle()
                // Seed the home screen with sensible apps on first run.
                LaunchedEffect(apps) {
                    if (apps.isNotEmpty() && !Prefs.favoritesInitialized) {
                        Prefs.setFavorites(AppRepository.defaultFavorites(this@MainActivity, apps))
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    if (!systemWallpaper) NothingBackground()
                    HomeScreen(this@MainActivity)
                    // Both panels stay composed so opening them never has to build the UI first.
                    SheetLayer(drawer, fromTop = false) { AppDrawer(drawer, searchRequest) }
                    SheetLayer(shade, fromTop = true) { ShadePanel(this@MainActivity, shade) }
                }
                BackHandler(enabled = drawer.isOpen || shade.isOpen) { closeOverlays() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        QuickSettings.refresh(this)
    }

    /** Pressing Home while already home closes the drawer/shade, like every stock launcher. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action) closeOverlays()
    }

    /** Leaving home (launching an app) resets the panels instantly, so returning shows home. */
    override fun onStop() {
        super.onStop()
        if (::drawer.isInitialized) { drawer.snapClosed(); shade.snapClosed() }
    }

    private fun closeOverlays() {
        if (!::drawer.isInitialized) return
        drawer.close()
        shade.close()
    }

    private fun isDefaultLauncher(): Boolean {
        val rm = getSystemService(RoleManager::class.java)
        return rm.isRoleAvailable(RoleManager.ROLE_HOME) && rm.isRoleHeld(RoleManager.ROLE_HOME)
    }

    /** Shows Android's "Set Nothing Launcher as your default home app?" dialog. */
    fun requestDefaultLauncher() {
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            runCatching { roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)) }
                .onFailure { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        } else {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    // HomeActions
    override fun openDrawer(search: Boolean) {
        if (search) searchRequest++
        shade.close()
        drawer.open()
    }
    override fun dragDrawer(deltaPx: Float) = drawer.dragBy(deltaPx)
    override fun settleDrawer(velocityPx: Float) = drawer.settle(velocityPx)
    override fun dragShade(deltaPx: Float) {
        if (shade.value == 0f) QuickSettings.refresh(this)
        shade.dragBy(deltaPx)
    }
    override fun settleShade(velocityPx: Float) = shade.settle(velocityPx)
    override fun openSettings() { startActivity(Intent(this, SetupActivity::class.java)) }
    override fun lockScreen() {
        if (!globalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) {
            android.widget.Toast.makeText(this, "Enable the Nothing Panel accessibility service to double-tap to lock", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // HomeActions + ShadeHost
    override fun launch(intent: Intent) {
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    // ShadeHost
    override fun close() = shade.close()
    override fun globalAction(action: Int): Boolean = ShadeAccessibilityService.instance?.globalAction(action) ?: false
}
