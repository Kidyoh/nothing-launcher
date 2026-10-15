package com.kidyoh.nothinglauncher.ui.home

import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidyoh.nothinglauncher.data.AppInfo
import com.kidyoh.nothinglauncher.data.AppRepository
import com.kidyoh.nothinglauncher.data.Prefs
import com.kidyoh.nothinglauncher.data.greeting
import com.kidyoh.nothinglauncher.data.rememberBattery
import com.kidyoh.nothinglauncher.data.rememberNow
import com.kidyoh.nothinglauncher.service.NotificationService
import com.kidyoh.nothinglauncher.service.QuickSettings
import com.kidyoh.nothinglauncher.ui.components.AppCircle
import com.kidyoh.nothinglauncher.ui.components.DotText
import com.kidyoh.nothinglauncher.ui.theme.N
import com.kidyoh.nothinglauncher.ui.theme.NType
import java.util.Calendar

interface HomeActions {
    fun openDrawer(search: Boolean)
    /** Live drag on the home screen; positive = towards open. */
    fun dragDrawer(deltaPx: Float)
    fun settleDrawer(velocityPx: Float)
    fun dragShade(deltaPx: Float)
    fun settleShade(velocityPx: Float)
    fun openSettings()
    fun lockScreen()
    fun launch(intent: Intent)
}

@Composable
fun HomeScreen(actions: HomeActions) {
    val ctx = LocalContext.current
    val apps by AppRepository.apps.collectAsStateWithLifecycle()
    val favs by Prefs.favorites.collectAsStateWithLifecycle()
    val media by NotificationService.media.collectAsStateWithLifecycle()
    val qs by QuickSettings.state.collectAsStateWithLifecycle()
    val now by rememberNow()
    val battery by rememberBattery()
    var menu by remember { mutableStateOf(false) }
    var pickSlot by remember { mutableStateOf<Int?>(null) }
    val act by rememberUpdatedState(actions)

    fun appAt(slot: Int): AppInfo? = favs.getOrNull(slot)?.takeIf { it.isNotEmpty() }?.let { k -> apps.firstOrNull { it.key == k } }

    @Composable
    fun Slot(slot: Int, size: Dp) {
        val app = appAt(slot)
        AppCircle(
            app, size,
            onClick = { if (app != null) AppRepository.launch(ctx, app) else pickSlot = slot },
            onLongClick = { pickSlot = slot },
        )
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Swipe up drags the app drawer, swipe down drags the shade — both follow the finger.
                val tracker = VelocityTracker()
                var dir = 0
                detectVerticalDragGestures(
                    onDragStart = { dir = 0; tracker.resetTracking() },
                    onDragEnd = {
                        val v = tracker.calculateVelocity().y
                        if (dir < 0) act.settleDrawer(-v) else if (dir > 0) act.settleShade(v)
                    },
                    onDragCancel = { if (dir < 0) act.settleDrawer(0f) else if (dir > 0) act.settleShade(0f) },
                ) { change, dy ->
                    tracker.addPosition(change.uptimeMillis, change.position)
                    if (dir == 0) dir = if (dy < 0) -1 else 1
                    if (dir < 0) act.dragDrawer(-dy) else act.dragShade(dy)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { act.lockScreen() },
                    onLongPress = { menu = true },
                )
            }
            .systemBarsPadding(),
    ) {
        val pad = 22.dp
        val gap = 14.dp
        val byWidth = (maxWidth - pad * 2 - gap * 3) / 4
        val byHeight = (maxHeight - 300.dp) / 5
        val cell = min(byWidth, byHeight).coerceAtLeast(48.dp)
        val icon = cell * 0.82f
        val block = cell * 2 + gap
        val is24h = DateFormat.is24HourFormat(ctx)

        Column(
            Modifier.fillMaxSize().padding(horizontal = pad, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            // Header
            Column(Modifier.padding(top = 8.dp)) {
                Text("WELCOME BACK", style = NType.label)
                Spacer(Modifier.height(8.dp))
                DotText(greeting(now.get(Calendar.HOUR_OF_DAY)), dot = 3.5.dp, color = N.White)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Battery temp is ${battery.tempC.toInt()}°C, level is ${battery.level}%, and the status is ${battery.status}.",
                    style = NType.bodySmall.copy(color = N.White.copy(alpha = 0.75f)),
                )
            }
            // Pills
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                RamPill(Modifier.weight(1f).height(54.dp)) {
                    act.launch(Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                }
                DatePill(now, Modifier.weight(1f).height(54.dp)) {
                    act.launch(Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()))
                }
            }
            // Block 1: 4 apps + battery
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                IconGrid2x2(listOf(0, 1, 2, 3), cell, icon, gap) { Slot(it, icon) }
                BatteryWidget(battery, Modifier.size(block)) {
                    act.launch(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
                }
            }
            // Block 2: dot clock + (music | 2 apps + DND)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                DotClockWidget(now, block, is24h) { act.launch(Intent(AlarmClock.ACTION_SHOW_ALARMS)) }
                val m = media
                if (m != null) {
                    MediaWidget(m, Modifier.size(block))
                } else {
                    Column(Modifier.size(block), verticalArrangement = Arrangement.spacedBy(gap)) {
                        Row(Modifier.height(cell), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            listOf(4, 5).forEach { s ->
                                androidx.compose.foundation.layout.Box(Modifier.size(cell), contentAlignment = Alignment.Center) { Slot(s, icon) }
                            }
                        }
                        DndPill(qs.dnd, Modifier.fillMaxWidth().height(cell * 0.8f)) {
                            QuickSettings.toggleDnd(ctx)?.let { act.launch(it) }
                        }
                    }
                }
            }
            // Row of 4
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                (6..9).forEach { s ->
                    androidx.compose.foundation.layout.Box(Modifier.size(cell), contentAlignment = Alignment.Center) { Slot(s, icon) }
                }
            }
            Spacer(Modifier.weight(1f))
            SearchBar(Modifier.fillMaxWidth().height(58.dp)) { act.openDrawer(true) }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Wallpaper") }, onClick = {
                menu = false
                act.launch(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
            })
            DropdownMenuItem(text = { Text("Launcher settings") }, onClick = { menu = false; act.openSettings() })
            DropdownMenuItem(text = { Text("All apps") }, onClick = { menu = false; act.openDrawer(false) })
        }
    }

    pickSlot?.let { slot ->
        AppPicker(
            apps = apps,
            onPick = { Prefs.setFavorite(slot, it.key); pickSlot = null },
            onRemove = { Prefs.setFavorite(slot, ""); pickSlot = null },
            onDismiss = { pickSlot = null },
        )
    }
}

@Composable
private fun IconGrid2x2(slots: List<Int>, cell: Dp, icon: Dp, gap: Dp, content: @Composable (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        slots.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { s ->
                    androidx.compose.foundation.layout.Box(Modifier.size(cell), contentAlignment = Alignment.Center) { content(s) }
                }
            }
        }
    }
}

@Composable
fun AppPicker(apps: List<AppInfo>, onPick: (AppInfo) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(28.dp)).background(N.Surface).padding(vertical = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("CHOOSE APP", style = NType.label, modifier = Modifier.weight(1f))
                Text(
                    "REMOVE", style = NType.label.copy(color = N.OnRed),
                    modifier = Modifier.clip(CircleShape).background(N.Red).clickable(onClick = onRemove)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(apps, key = { it.key }) { a ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(a) }.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(a.icon, null, Modifier.size(36.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(a.label, style = NType.body)
                    }
                }
            }
        }
    }
}
