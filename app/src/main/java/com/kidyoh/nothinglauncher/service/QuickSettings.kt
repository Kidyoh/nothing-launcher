package com.kidyoh.nothinglauncher.service

import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QsState(
    val wifi: Boolean = false,
    val bluetooth: Boolean = false,
    val torch: Boolean = false,
    val dnd: Boolean = false,
    val autoRotate: Boolean = false,
    val airplane: Boolean = false,
    val location: Boolean = false,
    val batterySaver: Boolean = false,
    val ringer: Int = AudioManager.RINGER_MODE_NORMAL,
    val brightness: Float = 0.5f,
    val autoBrightness: Boolean = false,
    val canWriteSettings: Boolean = false,
    val hasDndAccess: Boolean = false,
)

/**
 * Reads and toggles everything the Nothing shade shows.
 * Android does not let normal apps flip Wi-Fi, Bluetooth, mobile data, airplane mode or location directly,
 * so those open the matching system panel — exactly what Pixel/Nothing tiles fall back to for third parties.
 */
object QuickSettings {
    private val _state = MutableStateFlow(QsState())
    val state: StateFlow<QsState> = _state.asStateFlow()
    private var torchCameraId: String? = null
    private var torchRegistered = false

    fun refresh(ctx: Context) {
        val cr = ctx.contentResolver
        val nm = ctx.getSystemService(NotificationManager::class.java)
        registerTorch(ctx)
        _state.value = _state.value.copy(
            wifi = runCatching { ctx.applicationContext.getSystemService(WifiManager::class.java).isWifiEnabled }.getOrDefault(false),
            bluetooth = runCatching { ctx.getSystemService(BluetoothManager::class.java).adapter?.isEnabled == true }.getOrDefault(false),
            dnd = nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL,
            autoRotate = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1,
            airplane = Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1,
            location = ctx.getSystemService(LocationManager::class.java).isLocationEnabled,
            batterySaver = ctx.getSystemService(PowerManager::class.java).isPowerSaveMode,
            ringer = ctx.getSystemService(AudioManager::class.java).ringerMode,
            brightness = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f,
            autoBrightness = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == 1,
            canWriteSettings = Settings.System.canWrite(ctx),
            hasDndAccess = nm.isNotificationPolicyAccessGranted,
        )
    }

    private fun registerTorch(ctx: Context) {
        if (torchRegistered) return
        torchRegistered = true
        val cm = ctx.applicationContext.getSystemService(CameraManager::class.java)
        torchCameraId = runCatching {
            cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        }.getOrNull()
        cm.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == torchCameraId) _state.value = _state.value.copy(torch = enabled)
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun toggleTorch(ctx: Context) {
        val id = torchCameraId ?: return
        runCatching { ctx.getSystemService(CameraManager::class.java).setTorchMode(id, !_state.value.torch) }
    }

    /** Returns an intent to show when the action needs the user to grant something first. */
    fun toggleDnd(ctx: Context): Intent? {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        nm.setInterruptionFilter(
            if (_state.value.dnd) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        )
        refresh(ctx)
        return null
    }

    fun cycleRinger(ctx: Context): Intent? {
        val am = ctx.getSystemService(AudioManager::class.java)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val next = when (am.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        if (next == AudioManager.RINGER_MODE_SILENT && !nm.isNotificationPolicyAccessGranted) {
            return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }
        runCatching { am.ringerMode = next }
        refresh(ctx)
        return null
    }

    fun toggleAutoRotate(ctx: Context): Intent? {
        if (!Settings.System.canWrite(ctx)) return writeSettingsIntent(ctx)
        Settings.System.putInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (_state.value.autoRotate) 0 else 1)
        refresh(ctx)
        return null
    }

    fun setBrightness(ctx: Context, value: Float): Intent? {
        if (!Settings.System.canWrite(ctx)) return writeSettingsIntent(ctx)
        val cr = ctx.contentResolver
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (value * 255).toInt().coerceIn(1, 255))
        _state.value = _state.value.copy(brightness = value, autoBrightness = false)
        return null
    }

    fun toggleAutoBrightness(ctx: Context): Intent? {
        if (!Settings.System.canWrite(ctx)) return writeSettingsIntent(ctx)
        Settings.System.putInt(
            ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            if (_state.value.autoBrightness) 0 else 1,
        )
        refresh(ctx)
        return null
    }

    fun writeSettingsIntent(ctx: Context) =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    val wifiPanel get() = Intent(Settings.Panel.ACTION_WIFI)
    val internetPanel get() = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    val bluetoothSettings get() = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    val airplaneSettings get() = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
    val locationSettings get() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    val batterySaverSettings get() = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    val volumePanel get() = Intent(Settings.Panel.ACTION_VOLUME)
    val settings get() = Intent(Settings.ACTION_SETTINGS)

    /** Fires a notification's PendingIntent, allowing it to start an activity from the overlay. */
    fun send(ctx: Context, pi: PendingIntent) {
        val opts = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= 34) {
            opts.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        }
        runCatching { pi.send(ctx, 0, null, null, null, null, opts.toBundle()) }
    }
}
