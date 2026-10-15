package com.kidyoh.nothinglauncher.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import com.kidyoh.nothinglauncher.data.Prefs
import com.kidyoh.nothinglauncher.ui.shade.ShadeHost
import com.kidyoh.nothinglauncher.ui.shade.ShadePanel
import com.kidyoh.nothinglauncher.ui.theme.NothingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Puts an invisible strip over the status bar. Swiping down on it (in any app) opens the
 * Nothing-style shade instead of the stock one. Uses TYPE_ACCESSIBILITY_OVERLAY, which needs no
 * "draw over other apps" permission and sits above the system status bar.
 */
class ShadeAccessibilityService : AccessibilityService(), ShadeHost {

    companion object {
        @Volatile var instance: ShadeAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = ComponentName(context, ShadeAccessibilityService::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }

    private lateinit var wm: WindowManager
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var trigger: View? = null
    private var shadeRoot: View? = null
    private var owner: OverlayOwner? = null
    private val visible = MutableTransitionState(false)

    override fun onServiceConnected() {
        instance = this
        Prefs.init(this)
        wm = getSystemService(WindowManager::class.java)
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch {
            Prefs.shadeEnabled.collect { enabled -> if (enabled) addTrigger() else removeTrigger() }
        }
    }

    /** Another app or the home screen came to the front (e.g. Home pressed): hide the panel. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (shadeRoot == null || event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != packageName && pkg != "com.android.systemui") close()
    }

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            visible.targetState = false
            removeShade()
        }
    }
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instance = null
        runCatching { unregisterReceiver(screenOff) }
        scope.cancel()
        removeTrigger()
        removeShade()
        super.onDestroy()
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        val h = if (id > 0) resources.getDimensionPixelSize(id) else 0
        return maxOf(h, (28 * resources.displayMetrics.density).toInt())
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addTrigger() {
        if (trigger != null) return
        val threshold = 24 * resources.displayMetrics.density
        val v = View(this)
        var downY = 0f
        var fired = false
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; fired = false }
                MotionEvent.ACTION_MOVE -> if (!fired && e.rawY - downY > threshold) { fired = true; showShade() }
                MotionEvent.ACTION_UP -> if (!fired && e.rawY - downY > threshold / 2) showShade()
            }
            true
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            statusBarHeight(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        runCatching { wm.addView(v, lp) }.onSuccess { trigger = v }
    }

    private fun removeTrigger() {
        trigger?.let { runCatching { wm.removeView(it) } }
        trigger = null
    }

    fun showShade() {
        if (shadeRoot != null) { visible.targetState = true; return }
        QuickSettings.refresh(this)
        val o = OverlayOwner()
        val root = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) close()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        val compose = ComposeView(this).apply {
            setContent {
                NothingTheme {
                    val host = remember { this@ShadeAccessibilityService }
                    AnimatedVisibility(
                        visibleState = visible,
                        enter = slideInVertically { -it / 3 } + fadeIn(),
                        exit = slideOutVertically { -it / 3 } + fadeOut(),
                    ) { ShadePanel(host) }
                    LaunchedEffect(visible.isIdle, visible.currentState) {
                        if (visible.isIdle && !visible.currentState) removeShade()
                    }
                }
            }
        }
        root.addView(compose)
        o.attach(root)
        o.resume()
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        visible.targetState = true
        runCatching { wm.addView(root, lp) }.onSuccess {
            shadeRoot = root
            owner = o
        }.onFailure { o.destroy() }
    }

    private fun removeShade() {
        shadeRoot?.let { runCatching { wm.removeView(it) } }
        shadeRoot = null
        owner?.destroy()
        owner = null
    }

    // ShadeHost
    override fun close() { visible.targetState = false }

    override fun launch(intent: Intent) {
        close()
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    override fun globalAction(action: Int): Boolean {
        // Let the panel animate away before screenshots / lock so it isn't captured.
        close()
        val delayMs = if (action == GLOBAL_ACTION_POWER_DIALOG) 0L else 450L
        main.postDelayed({ performGlobalAction(action) }, delayMs)
        return true
    }
}
