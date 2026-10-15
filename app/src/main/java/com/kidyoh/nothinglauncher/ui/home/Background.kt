package com.kidyoh.nothinglauncher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kidyoh.nothinglauncher.ui.theme.N

/** The dark, layered rounded-block wallpaper from the Nothing reference shots, plus a faint dot grid. */
@Composable
fun NothingBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(N.Black)
        val w = size.width
        val h = size.height
        val blockA = Color(0xFF171717)
        val blockB = Color(0xFF1D1D1D)
        val edge = Color(0x14FFFFFF)
        val r = CornerRadius(w * 0.16f)
        fun block(x: Float, y: Float, bw: Float, bh: Float, c: Color) {
            drawRoundRect(c, Offset(x, y), Size(bw, bh), r)
            drawRoundRect(edge, Offset(x, y), Size(bw, bh), r, style = Stroke(1.2f))
        }
        block(-w * 0.20f, h * 0.10f, w * 0.75f, h * 0.36f, blockA)
        block(w * 0.45f, -h * 0.05f, w * 0.70f, h * 0.30f, blockB)
        block(w * 0.30f, h * 0.38f, w * 0.85f, h * 0.34f, blockA)
        block(-w * 0.30f, h * 0.62f, w * 0.80f, h * 0.30f, blockB)
        block(w * 0.55f, h * 0.80f, w * 0.60f, h * 0.30f, blockA)
        // Dot grid
        val step = w / 18f
        val dot = Color(0x0FFFFFFF)
        var y = step / 2
        while (y < h) {
            var x = step / 2
            while (x < w) {
                drawCircle(dot, 1.6f, Offset(x, y)); x += step
            }
            y += step
        }
    }
}
