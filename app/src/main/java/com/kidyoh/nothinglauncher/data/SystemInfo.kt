package com.kidyoh.nothinglauncher.data

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar

data class Battery(val level: Int, val charging: Boolean, val tempC: Float, val status: String)

/** Live battery state from the sticky ACTION_BATTERY_CHANGED broadcast. */
@Composable
fun rememberBattery(): State<Battery> {
    val ctx = LocalContext.current
    return produceState(Battery(0, false, 0f, "unknown")) {
        fun parse(i: Intent?): Battery {
            i ?: return value
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            val status = when (st) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
                BatteryManager.BATTERY_STATUS_FULL -> "full"
                BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "discharging"
                else -> "unknown"
            }
            return Battery(level * 100 / scale, charging, i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f, status)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { value = parse(i) }
        }
        val sticky = ContextCompat.registerReceiver(
            ctx, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        value = parse(sticky)
        awaitDispose { ctx.unregisterReceiver(receiver) }
    }
}

/** Current time, ticking at the top of every minute. */
@Composable
fun rememberNow(): State<Calendar> = produceState(Calendar.getInstance()) {
    while (isActive) {
        value = Calendar.getInstance()
        delay(60_000L - System.currentTimeMillis() % 60_000L)
    }
}

/** Free RAM in bytes, refreshed every few seconds. */
@Composable
fun rememberFreeRam(): State<Long> {
    val ctx = LocalContext.current
    return produceState(0L) {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        while (isActive) {
            am.getMemoryInfo(info)
            value = info.availMem
            delay(5_000)
        }
    }
}

fun formatBytes(b: Long): String {
    val mb = b / 1024.0 / 1024.0
    return if (mb >= 1024) "%.2fGB".format(mb / 1024) else "%.2fMB".format(mb)
}

fun greeting(hour: Int) = when (hour) {
    in 5..11 -> "GOOD MORNING"
    in 12..16 -> "GOOD AFTERNOON"
    in 17..21 -> "GOOD EVENING"
    else -> "GOOD NIGHT"
}

