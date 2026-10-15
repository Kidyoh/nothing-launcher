package com.kidyoh.nothinglauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * A full-screen panel (app drawer / shade) whose position tracks the finger 1:1 and then
 * springs open or closed using the release velocity. progress: 0 = hidden, 1 = fully open.
 */
class SheetState(private val scope: CoroutineScope) {
    val progress = Animatable(0f)
    /** Travel distance in px for progress 0 → 1; set from the panel's measured height. */
    var heightPx = 2000f

    val value: Float get() = progress.value
    val isOpen: Boolean get() = progress.targetValue > 0.5f
    val isVisible: Boolean get() = progress.value > 0f || progress.targetValue > 0f

    /** Moves the panel by [deltaPx]; positive means "more open". */
    fun dragBy(deltaPx: Float) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            progress.snapTo((progress.value + deltaPx / heightPx).coerceIn(0f, 1f))
        }
    }

    /** Finishes a drag. [velocityPx] is px/s, positive means "towards open". */
    fun settle(velocityPx: Float) {
        val v = velocityPx / heightPx
        val target = when {
            v > 1.0f -> 1f
            v < -1.0f -> 0f
            progress.value > 0.4f -> 1f
            else -> 0f
        }
        animateTo(target, v)
    }

    fun open() = animateTo(1f, 0f)
    fun close() = animateTo(0f, 0f)
    fun snapClosed() { scope.launch(start = CoroutineStart.UNDISPATCHED) { progress.snapTo(0f) } }

    private fun animateTo(target: Float, velocity: Float) {
        scope.launch {
            progress.animateTo(target, spring(dampingRatio = 1f, stiffness = 900f), initialVelocity = velocity)
        }
    }

    /**
     * Lets a scrolling list inside the panel keep dragging the panel once it hits its edge.
     * [openDirection] is +1 when dragging down opens the panel (shade), -1 when dragging up does (drawer).
     */
    fun nestedScroll(openDirection: Int) = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Panel partly dragged: finish moving it before the list scrolls again.
            val toOpen = available.y * openDirection
            if (source == NestedScrollSource.UserInput && progress.value < 1f && toOpen > 0f) {
                dragBy(toOpen)
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val toOpen = available.y * openDirection
            if (source == NestedScrollSource.UserInput && toOpen < 0f) {
                dragBy(toOpen)
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (progress.value < 1f) {
                settle(available.y * openDirection)
                return available
            }
            return Velocity.Zero
        }
    }
}

/** Drag handle behaviour for the panel's own chrome (header, grab bar). */
fun Modifier.sheetDrag(sheet: SheetState, openDirection: Int): Modifier = pointerInput(sheet) {
    val tracker = VelocityTracker()
    detectVerticalDragGestures(
        onDragStart = { tracker.resetTracking() },
        onDragEnd = { sheet.settle(tracker.calculateVelocity().y * openDirection) },
        onDragCancel = { sheet.settle(0f) },
    ) { change, dy ->
        tracker.addPosition(change.uptimeMillis, change.position)
        sheet.dragBy(dy * openDirection)
    }
}

/**
 * Hosts a panel and slides it in from the top ([fromTop]) or bottom. Movement happens in the
 * graphics layer only, so dragging never recomposes or re-measures the panel content.
 * While hidden it is not drawn and sits off-screen, so it can't catch touches.
 */
@Composable
fun SheetLayer(sheet: SheetState, fromTop: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { if (it.height > 0) sheet.heightPx = it.height.toFloat() }
            .graphicsLayer {
                val p = sheet.progress.value
                val h = sheet.heightPx
                translationY = when {
                    p <= 0f -> if (fromTop) -h else h // fully off-screen: never intercepts touches
                    fromTop -> -(1f - p) * h
                    else -> (1f - p) * h * 0.6f
                }
                alpha = if (p <= 0f) 0f else if (fromTop) 1f else (0.4f + p * 0.6f).coerceAtMost(1f)
            },
    ) { content() }
}
