package com.kidyoh.nothinglauncher.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotifItem(
    val key: String,
    val pkg: String,
    val appName: String,
    val title: String,
    val text: String,
    val time: Long,
    val smallIcon: ImageBitmap?,
    val largeIcon: ImageBitmap?,
    val contentIntent: PendingIntent?,
    val clearable: Boolean,
)

data class MediaInfo(
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val playing: Boolean,
    val controller: MediaController,
)

/**
 * Mirrors the system's active notifications into a flow for the Nothing shade,
 * and (because it is an enabled listener) can also observe media sessions.
 */
class NotificationService : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<NotifItem>>(emptyList())
        val notifications: StateFlow<List<NotifItem>> = _notifications.asStateFlow()

        private val _media = MutableStateFlow<MediaInfo?>(null)
        val media: StateFlow<MediaInfo?> = _media.asStateFlow()

        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        @Volatile private var instance: NotificationService? = null

        fun dismiss(key: String) { runCatching { instance?.cancelNotification(key) } }
        fun dismissAll() { runCatching { instance?.cancelAllNotifications() } }

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(context, NotificationService::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }

    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishMedia()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishMedia()
        override fun onSessionDestroyed() = refreshSessions()
    }
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { refreshSessions() }

    override fun onListenerConnected() {
        instance = this
        _connected.value = true
        refreshAll()
        runCatching {
            sessionManager = getSystemService(MediaSessionManager::class.java).also {
                it.addOnActiveSessionsChangedListener(sessionsListener, ComponentName(this, NotificationService::class.java))
            }
            refreshSessions()
        }
    }

    override fun onListenerDisconnected() {
        instance = null
        _connected.value = false
        runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        activeController?.unregisterCallback(controllerCallback)
        _media.value = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refreshAll()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refreshAll()
    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) = refreshAll()

    private fun refreshAll() {
        val list = runCatching { activeNotifications }.getOrNull() ?: return
        _notifications.value = list
            .filter { !it.notification.isGroupSummaryWithChildren(list) }
            .filter { it.notification.category != Notification.CATEGORY_TRANSPORT || it.isClearable }
            .mapNotNull { toItem(it) }
            .sortedByDescending { it.time }
    }

    private fun Notification.isGroupSummaryWithChildren(all: Array<StatusBarNotification>): Boolean {
        if (flags and Notification.FLAG_GROUP_SUMMARY == 0) return false
        val group = group ?: return false
        return all.any { it.notification.group == group && it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
    }

    private fun toItem(sbn: StatusBarNotification): NotifItem? {
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return null
        val appName = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        val dp = resources.displayMetrics.density
        return NotifItem(
            key = sbn.key,
            pkg = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            time = sbn.postTime,
            smallIcon = n.smallIcon?.toImage(sbn.packageName, (20 * dp).toInt()),
            largeIcon = n.getLargeIcon()?.toImage(sbn.packageName, (44 * dp).toInt()),
            contentIntent = n.contentIntent,
            clearable = sbn.isClearable,
        )
    }

    private fun Icon.toImage(pkg: String, size: Int): ImageBitmap? = runCatching {
        val ctx = runCatching { createPackageContext(pkg, 0) }.getOrDefault(this@NotificationService)
        loadDrawable(ctx)?.toBitmap(size, size, Bitmap.Config.ARGB_8888)?.asImageBitmap()
    }.getOrNull()

    private fun refreshSessions() {
        val sm = sessionManager ?: return
        val sessions = runCatching { sm.getActiveSessions(ComponentName(this, NotificationService::class.java)) }
            .getOrDefault(emptyList())
        val pick = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
        if (pick?.sessionToken != activeController?.sessionToken) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = pick
            pick?.registerCallback(controllerCallback)
        }
        publishMedia()
    }

    private fun publishMedia() {
        val c = activeController
        val md = c?.metadata
        if (c == null || md == null) { _media.value = null; return }
        _media.value = MediaInfo(
            title = md.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = (md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM)).orEmpty(),
            art = (md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART))?.asImageBitmap(),
            playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            controller = c,
        )
    }
}
