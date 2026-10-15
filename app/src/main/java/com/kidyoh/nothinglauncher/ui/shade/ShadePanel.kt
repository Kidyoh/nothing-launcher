package com.kidyoh.nothinglauncher.ui.shade

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidyoh.nothinglauncher.data.rememberBattery
import com.kidyoh.nothinglauncher.data.rememberNow
import com.kidyoh.nothinglauncher.service.MediaInfo
import com.kidyoh.nothinglauncher.service.NotifItem
import com.kidyoh.nothinglauncher.service.NotificationService
import com.kidyoh.nothinglauncher.service.QuickSettings
import com.kidyoh.nothinglauncher.ui.components.DotText
import com.kidyoh.nothinglauncher.ui.theme.N
import com.kidyoh.nothinglauncher.ui.theme.NType
import java.text.SimpleDateFormat
import java.util.Locale

/** What the shade needs from its host (the home activity, or the system-wide overlay). */
interface ShadeHost {
    fun close()
    fun launch(intent: Intent)
    /** Runs an AccessibilityService global action. Returns false if the service is off. */
    fun globalAction(action: Int): Boolean
}

@Composable
fun ShadePanel(host: ShadeHost, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val qs by QuickSettings.state.collectAsStateWithLifecycle()
    val notifs by NotificationService.notifications.collectAsStateWithLifecycle()
    val listenerOn by NotificationService.connected.collectAsStateWithLifecycle()
    val media by NotificationService.media.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { QuickSettings.refresh(ctx) }

    fun run(intent: Intent?) { if (intent != null) host.launch(intent) }

    // Drag up anywhere on the non-scrolling chrome to close.
    val closeDrag = Modifier.pointerInput(Unit) {
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = { if (total < -80f) host.close() },
        ) { _, dy -> total += dy }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(N.Black.copy(alpha = 0.97f))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { ShadeHeader(host, closeDrag) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WideTile(Icons.Outlined.Wifi, "Internet", if (qs.wifi) "Wi-Fi on" else "Wi-Fi off", qs.wifi, Modifier.weight(1f)) {
                        run(QuickSettings.internetPanel)
                    }
                    WideTile(Icons.Outlined.Bluetooth, "Bluetooth", if (qs.bluetooth) "On" else "Off", qs.bluetooth, Modifier.weight(1f)) {
                        run(QuickSettings.bluetoothSettings)
                    }
                }
            }
            item {
                val ringerIcon = when (qs.ringer) {
                    AudioManager.RINGER_MODE_SILENT -> Icons.AutoMirrored.Outlined.VolumeOff
                    AudioManager.RINGER_MODE_VIBRATE -> Icons.Outlined.Vibration
                    else -> Icons.AutoMirrored.Outlined.VolumeUp
                }
                val ringerLabel = when (qs.ringer) {
                    AudioManager.RINGER_MODE_SILENT -> "Silent"
                    AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                    else -> "Sound"
                }
                val tiles = listOf<Triple<ImageVector, String, Pair<Boolean, () -> Unit>>>(
                    Triple(Icons.Outlined.FlashlightOn, "Torch", qs.torch to { QuickSettings.toggleTorch(ctx) }),
                    Triple(Icons.Outlined.DoNotDisturbOn, "DND", qs.dnd to { run(QuickSettings.toggleDnd(ctx)) }),
                    Triple(ringerIcon, ringerLabel, (qs.ringer != AudioManager.RINGER_MODE_NORMAL) to { run(QuickSettings.cycleRinger(ctx)) }),
                    Triple(Icons.Outlined.ScreenRotation, "Rotate", qs.autoRotate to { run(QuickSettings.toggleAutoRotate(ctx)) }),
                    Triple(Icons.Outlined.AirplanemodeActive, "Airplane", qs.airplane to { run(QuickSettings.airplaneSettings) }),
                    Triple(Icons.Outlined.LocationOn, "Location", qs.location to { run(QuickSettings.locationSettings) }),
                    Triple(Icons.Outlined.BatterySaver, "Saver", qs.batterySaver to { run(QuickSettings.batterySaverSettings) }),
                    Triple(Icons.Outlined.Screenshot, "Capture", false to {
                        host.close()
                        host.globalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                        Unit
                    }),
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tiles.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { (icon, label, s) -> RoundTile(icon, label, s.first, s.second) }
                        }
                    }
                }
            }
            item {
                BrightnessSlider(
                    value = qs.brightness,
                    auto = qs.autoBrightness,
                    onChange = { v -> run(QuickSettings.setBrightness(ctx, v)) },
                    onToggleAuto = { run(QuickSettings.toggleAutoBrightness(ctx)) },
                )
            }
            media?.let { m -> item { MediaCard(m) } }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("NOTIFICATIONS", style = NType.label, modifier = Modifier.weight(1f))
                    if (notifs.any { it.clearable }) {
                        Text(
                            "CLEAR ALL",
                            style = NType.label.copy(color = N.White),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(N.SurfaceHigh)
                                .clickable { NotificationService.dismissAll() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            if (!listenerOn) {
                item {
                    Text(
                        "Tap to allow notification access so they appear here",
                        style = NType.body,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(N.Red)
                            .clickable { host.launch(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                            .padding(18.dp),
                    )
                }
            } else if (notifs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                        DotText("0 NOTIFICATIONS", dot = 3.dp, color = N.Grey)
                    }
                }
            }
            items(notifs, key = { it.key }) { n -> NotificationCard(n, host) }
        }
        // Handle bar: drag up or tap to close.
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .then(closeDrag)
                .pointerInput(Unit) { detectTapGestures { host.close() } },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(48.dp, 5.dp).clip(CircleShape).background(N.Grey))
        }
    }
}

@Composable
private fun ShadeHeader(host: ShadeHost, closeDrag: Modifier) {
    val now by rememberNow()
    val battery by rememberBattery()
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
    val date = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now.time).uppercase()
    Row(closeDrag.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            DotText(time, dot = 6.dp, color = N.White)
            Spacer(Modifier.height(8.dp))
            Text("$date  ·  ${battery.level}%${if (battery.charging) " ⚡" else ""}", style = NType.bodySmall)
        }
        SmallCircle(Icons.Outlined.Settings) { host.launch(QuickSettings.settings) }
        Spacer(Modifier.width(10.dp))
        SmallCircle(Icons.Outlined.PowerSettingsNew, red = true) {
            if (!host.globalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)) host.launch(QuickSettings.settings)
        }
        Spacer(Modifier.width(10.dp))
        SmallCircle(Icons.Outlined.Lock) {
            host.close()
            host.globalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}

@Composable
private fun SmallCircle(icon: ImageVector, red: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(if (red) N.Red else N.SurfaceHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = N.White, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun WideTile(icon: ImageVector, title: String, sub: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(68.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(if (on) N.Red else N.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(if (on) N.White else N.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (on) N.Red else N.White, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = NType.title.copy(fontSize = 13.sp), maxLines = 1)
            Text(sub, style = NType.bodySmall.copy(color = if (on) N.White else N.Grey), maxLines = 1)
        }
    }
}

@Composable
private fun RoundTile(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(if (on) N.Red else N.Surface).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = N.White, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(label, style = NType.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BrightnessSlider(value: Float, auto: Boolean, onChange: (Float) -> Unit, onToggleAuto: () -> Unit) {
    var local by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { local = value }
    val change by rememberUpdatedState(onChange)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .weight(1f)
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(N.Surface)
                .pointerInput(Unit) {
                    detectTapGestures { o -> local = (o.x / size.width).coerceIn(0.02f, 1f); change(local) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change2, _ ->
                        local = (change2.position.x / size.width).coerceIn(0.02f, 1f)
                        change(local)
                    }
                },
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(local.coerceAtLeast(0.2f))
                    .clip(RoundedCornerShape(30.dp))
                    .background(N.White),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.BrightnessHigh, null, tint = N.Black, modifier = Modifier.padding(end = 18.dp).size(22.dp))
            }
        }
        Box(
            Modifier.size(60.dp).clip(CircleShape).background(if (auto) N.Red else N.Surface).clickable(onClick = onToggleAuto),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.BrightnessAuto, "Auto brightness", tint = N.White) }
    }
}

@Composable
fun MediaCard(m: MediaInfo, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(N.Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(N.SurfaceHigh)) {
            m.art?.let {
                Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }))
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(m.title, style = NType.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(m.artist, style = NType.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val tc = m.controller.transportControls
        MediaButton(Icons.Outlined.SkipPrevious) { tc.skipToPrevious() }
        MediaButton(if (m.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, red = true) {
            if (m.playing) tc.pause() else tc.play()
        }
        MediaButton(Icons.Outlined.SkipNext) { tc.skipToNext() }
    }
}

@Composable
private fun MediaButton(icon: ImageVector, red: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 4.dp).size(40.dp).clip(CircleShape).background(if (red) N.Red else N.SurfaceHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = N.White, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun NotificationCard(n: NotifItem, host: ShadeHost) {
    val ctx: Context = LocalContext.current
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        if (v != SwipeToDismissBoxValue.Settled && n.clearable) { NotificationService.dismiss(n.key); true } else false
    })
    SwipeToDismissBox(state = state, backgroundContent = {}, enableDismissFromStartToEnd = n.clearable, enableDismissFromEndToStart = n.clearable) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(N.Surface)
                .clickable {
                    n.contentIntent?.let { QuickSettings.send(ctx, it) }
                    host.close()
                }
                .padding(14.dp),
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(N.White), contentAlignment = Alignment.Center) {
                n.smallIcon?.let { Image(it, null, modifier = Modifier.size(18.dp), colorFilter = ColorFilter.tint(N.Black)) }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.appName.uppercase(), style = NType.label, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    Text(
                        "  ·  " + DateUtils.getRelativeTimeSpanString(n.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                        style = NType.label, maxLines = 1,
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (n.title.isNotEmpty()) Text(n.title, style = NType.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (n.text.isNotEmpty()) Text(n.text, style = NType.body.copy(color = N.Grey), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            n.largeIcon?.let {
                Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
            }
        }
    }
}
