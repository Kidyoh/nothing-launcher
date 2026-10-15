package com.kidyoh.nothinglauncher

import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.LaunchedEffect
import com.kidyoh.nothinglauncher.ui.theme.rememberIsDark
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidyoh.nothinglauncher.data.Prefs
import com.kidyoh.nothinglauncher.service.NotificationService
import com.kidyoh.nothinglauncher.service.ShadeAccessibilityService
import com.kidyoh.nothinglauncher.ui.components.DotText
import com.kidyoh.nothinglauncher.ui.theme.N
import com.kidyoh.nothinglauncher.ui.theme.NType
import com.kidyoh.nothinglauncher.ui.theme.NothingTheme

/** Walks through every permission the launcher needs to take over home, shade and quick settings. */
class SetupActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContent {
            NothingTheme {
                val dark = rememberIsDark()
                LaunchedEffect(dark) {
                    val bars = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                }
                Screen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++ // re-check grants when returning from system settings
    }

    @Composable
    private fun Screen() {
        @Suppress("UNUSED_VARIABLE") val t = tick
        val rm = getSystemService(RoleManager::class.java)
        val nm = getSystemService(NotificationManager::class.java)
        val isHome = rm.isRoleHeld(RoleManager.ROLE_HOME)
        val listener = NotificationService.isEnabled(this)
        val a11y = ShadeAccessibilityService.isEnabled(this)
        val dnd = nm.isNotificationPolicyAccessGranted
        val write = Settings.System.canWrite(this)
        val systemWallpaper by Prefs.systemWallpaper.collectAsStateWithLifecycle()
        val mono by Prefs.monoIcons.collectAsStateWithLifecycle()
        val shade by Prefs.shadeEnabled.collectAsStateWithLifecycle()

        Column(
            Modifier.fillMaxSize().background(N.Black).systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("NOTHING LAUNCHER", style = NType.label)
            DotText("SETUP", dot = 6.dp, color = N.White)
            Text("Grant each item so the launcher can replace your home screen, pull-down panel and quick settings.", style = NType.bodySmall)
            Spacer(Modifier.height(4.dp))

            Step("1", "Default home app", "Makes this your launcher.", isHome) {
                if (rm.isRoleAvailable(RoleManager.ROLE_HOME)) roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                else startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
            Step("2", "Nothing Panel (accessibility)", "Replaces the pull-down shade in every app. Find \"Nothing Panel\" under Installed/Downloaded apps and turn it on.", a11y) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            Step("3", "Notification access", "Shows your notifications and the music widget.", listener) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            Step("4", "Do Not Disturb access", "Lets the DND and silent tiles work.", dnd) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
            Step("5", "Modify system settings", "Brightness slider and auto-rotate tile.", write) {
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            }

            Spacer(Modifier.height(8.dp))
            Text("PREFERENCES", style = NType.label)
            val theme by Prefs.themeMode.collectAsStateWithLifecycle()
            ThemePicker(theme)
            Toggle("Replace system pull-down panel", shade) { Prefs.setShadeEnabled(it) }
            Toggle("Monochrome (Nothing) icons", mono) { Prefs.setMonoIcons(it) }
            Toggle("Use my own wallpaper", systemWallpaper) { Prefs.setSystemWallpaper(it) }

            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(56.dp).clip(CircleShape).background(N.Red)
                    .clickable { Prefs.setupDone = true; finish() },
                contentAlignment = Alignment.Center,
            ) { Text("DONE", style = NType.title.copy(color = N.OnRed)) }
        }
    }

    @Composable
    private fun Step(n: String, title: String, sub: String, done: Boolean, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(N.Surface)
                .clickable(enabled = !done, onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (done) N.White else N.Red),
                contentAlignment = Alignment.Center,
            ) { Text(if (done) "✓" else n, style = NType.title.copy(color = if (done) N.Black else N.OnRed)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = NType.title)
                Text(sub, style = NType.bodySmall)
            }
            Text(if (done) "ON" else "GRANT", style = NType.label.copy(color = if (done) N.Grey else N.Red))
        }
    }

    @Composable
    private fun ThemePicker(current: String) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(N.Surface).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(Prefs.THEME_DARK to "DARK", Prefs.THEME_LIGHT to "LIGHT", Prefs.THEME_SYSTEM to "SYSTEM").forEach { (mode, label) ->
                val selected = mode == current
                Box(
                    Modifier.weight(1f).height(44.dp).clip(CircleShape)
                        .background(if (selected) N.Red else N.Surface)
                        .clickable { Prefs.setThemeMode(mode) },
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = NType.label.copy(color = if (selected) N.OnRed else N.White)) }
            }
        }
    }

    @Composable
    private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(N.Surface).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = NType.body, modifier = Modifier.weight(1f))
            Switch(
                checked = value, onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = N.Red, checkedThumbColor = N.OnRed),
            )
        }
    }
}
