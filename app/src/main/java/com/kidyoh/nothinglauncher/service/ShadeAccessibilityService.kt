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
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import com.kidyoh.nothinglauncher.data.Prefs
import com.kidyoh.nothinglauncher.ui.components.SheetLayer
import com.kidyoh.nothinglauncher.ui.components.SheetState
import com.kidyoh.nothinglauncher.ui.shade.ShadeHost
import com.kidyoh.nothinglauncher.ui.shade.ShadePanel
import com.kidyoh.nothinglauncher.ui.theme.NothingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Puts an invisible strip over the status bar. Swiping down on it (in any app) pulls down the
 * Nothing-style shade instead of the stock one, tracking the finger. Uses TYPE_ACCESSIBILITY_OVERLAY,
 * which needs no "draw over other apps" permission and sits above the system status bar.
 *
 * The shade window is created once and kept composed but invisible and untouchable while closed,
 * so pulling it down never waits for the UI to be built.
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
    private var shadeParams: WindowManager.LayoutParams? = null
    private var owner: OverlayOwner? = null
    private var sheet: SheetState? = null
    private var shadeShown = false

    override fun onServiceConnected() {
        instance = this
        Prefs.init(this)
        wm = getSystemService(WindowManager::class.java)
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch {
            Prefs.shadeEnabled.collect { enabled ->
                if (enabled) { addShadeWindow(); addTrigger() } else { removeTrigger(); removeShadeWindow() }
            }
        }
    }

    /** Another app or the home screen came to the front (e.g. Home pressed): hide the panel. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!shadeShown || event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != packageName && pkg != "com.android.systemui") close()
    }

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            sheet?.snapClosed()
            setShadeShown(false)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instance = null
        runCatching { unregisterReceiver(screenOff) }
        scope.cancel()
        removeTrigger()
        removeShadeWindow()
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
        val slop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()
        val v = View(this)
        var downY = 0f
        var lastY = 0f
        var dragging = false
        var tracker: VelocityTracker? = null
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = e.rawY; lastY = e.rawY; dragging = false
                    tracker?.recycle()
                    tracker = VelocityTracker.obtain().also { it.addMovement(e) }
                }
                MotionEvent.ACTION_MOVE -> {
                    tracker?.addMovement(e)
                    if (!dragging && e.rawY - downY > slop) {
                        dragging = true
                        lastY = downY + slop
                        QuickSettings.refresh(this)
                        setShadeShown(true)
                    }
                    if (dragging) {
                        sheet?.dragBy(e.rawY - lastY)
                        lastY = e.rawY
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val t = tracker
                    if (dragging && t != null) {
                        t.addMovement(e)
                        t.computeCurrentVelocity(1000)
                        sheet?.settle(t.yVelocity)
                    } else if (e.actionMasked == MotionEvent.ACTION_UP && e.rawY - downY > slop / 2) {
                        showShade()
                    }
                    t?.recycle()
                    tracker = null
                }
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

    private fun addShadeWindow() {
        if (shadeRoot != null) return
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
                // The sheet needs a composition scope: its spring animations run on the frame clock.
                val composeScope = rememberCoroutineScope()
                val s = remember { SheetState(composeScope).also { sheet = it } }
                LaunchedEffect(s) {
                    snapshotFlow { s.progress.value == 0f && s.progress.targetValue == 0f && !s.progress.isRunning }
                        .distinctUntilChanged()
                        .collect { closed -> if (closed) setShadeShown(false) }
                }
                NothingTheme {
                    SheetLayer(s, fromTop = true) { ShadePanel(this@ShadeAccessibilityService, s) }
                }
            }
        }
        root.addView(compose)
        root.visibility = View.INVISIBLE
        o.attach(root)
        o.resume()
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            HIDDEN_FLAGS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        runCatching { wm.addView(root, lp) }.onSuccess {
            shadeRoot = root
            shadeParams = lp
            owner = o
        }.onFailure { o.destroy() }
    }

    private fun removeShadeWindow() {
        shadeRoot?.let { runCatching { wm.removeView(it) } }
        shadeRoot = null
        shadeParams = null
        sheet = null
        shadeShown = false
        owner?.destroy()
        owner = null
    }

    /** Visible + touchable + focusable (for Back) while open; invisible and pass-through while closed. */
    private fun setShadeShown(shown: Boolean) {
        val root = shadeRoot ?: return
        val lp = shadeParams ?: return
        if (shadeShown == shown) return
        shadeShown = shown
        lp.flags = if (shown) SHOWN_FLAGS else HIDDEN_FLAGS
        root.visibility = if (shown) View.VISIBLE else View.INVISIBLE
        runCatching { wm.updateViewLayout(root, lp) }
    }

    fun showShade() {
        if (shadeRoot == null) addShadeWindow()
        QuickSettings.refresh(this)
        setShadeShown(true)
        sheet?.open()
    }

    // ShadeHost
    override fun close() { sheet?.close() }

    override fun launch(intent: Intent) {
        close()
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    override fun globalAction(action: Int): Boolean {
        // Let the panel animate away before screenshots / lock so it isn't captured.
        close()
        val delayMs = if (action == GLOBAL_ACTION_POWER_DIALOG) 0L else 350L
        main.postDelayed({ performGlobalAction(action) }, delayMs)
        return true
    }
}

private const val SHOWN_FLAGS =
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

private const val HIDDEN_FLAGS = SHOWN_FLAGS or
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
