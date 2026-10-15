package com.kidyoh.nothinglauncher

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidyoh.nothinglauncher.data.AppRepository
import com.kidyoh.nothinglauncher.data.Prefs
import com.kidyoh.nothinglauncher.service.QuickSettings
import com.kidyoh.nothinglauncher.service.ShadeAccessibilityService
import com.kidyoh.nothinglauncher.ui.drawer.AppDrawer
import com.kidyoh.nothinglauncher.ui.home.HomeActions
import com.kidyoh.nothinglauncher.ui.home.HomeScreen
import com.kidyoh.nothinglauncher.ui.home.NothingBackground
import com.kidyoh.nothinglauncher.ui.shade.ShadeHost
import com.kidyoh.nothinglauncher.ui.shade.ShadePanel
import com.kidyoh.nothinglauncher.ui.theme.NothingTheme

class MainActivity : ComponentActivity(), HomeActions, ShadeHost {

    private var drawerOpen by mutableStateOf(false)
    private var drawerSearch by mutableStateOf(false)
    private var shadeOpen by mutableStateOf(false)

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
            NothingTheme {
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
                    AnimatedVisibility(
                        visible = drawerOpen,
                        enter = slideInVertically { it / 3 } + fadeIn(),
                        exit = slideOutVertically { it / 3 } + fadeOut(),
                    ) { AppDrawer(drawerSearch, onClose = { drawerOpen = false }) }
                    AnimatedVisibility(
                        visible = shadeOpen,
                        enter = slideInVertically { -it / 3 } + fadeIn(),
                        exit = slideOutVertically { -it / 3 } + fadeOut(),
                    ) { ShadePanel(this@MainActivity) }
                }
                BackHandler(enabled = drawerOpen || shadeOpen) { closeOverlays() }
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

    private fun closeOverlays() {
        drawerOpen = false
        shadeOpen = false
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
    override fun openDrawer(search: Boolean) { drawerSearch = search; shadeOpen = false; drawerOpen = true }
    override fun openShade() { QuickSettings.refresh(this); drawerOpen = false; shadeOpen = true }
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
    override fun close() { shadeOpen = false }
    override fun globalAction(action: Int): Boolean = ShadeAccessibilityService.instance?.globalAction(action) ?: false
}
