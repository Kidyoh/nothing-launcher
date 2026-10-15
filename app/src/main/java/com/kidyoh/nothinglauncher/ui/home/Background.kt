package com.kidyoh.nothinglauncher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kidyoh.nothinglauncher.ui.theme.N

/**
 * The layered rounded-block wallpaper from the Nothing reference shots, plus a faint dot grid.
 * Geometry is computed once per size (drawWithCache) and the dots go out in a single drawPoints call.
 */
@Composable
fun NothingBackground(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().drawWithCache {
            val w = size.width
            val h = size.height
            val r = CornerRadius(w * 0.16f)
            val blocks = listOf(
                Triple(Offset(-w * 0.20f, h * 0.10f), Size(w * 0.75f, h * 0.36f), 1),
                Triple(Offset(w * 0.45f, -h * 0.05f), Size(w * 0.70f, h * 0.30f), 2),
                Triple(Offset(w * 0.30f, h * 0.38f), Size(w * 0.85f, h * 0.34f), 1),
                Triple(Offset(-w * 0.30f, h * 0.62f), Size(w * 0.80f, h * 0.30f), 2),
                Triple(Offset(w * 0.55f, h * 0.80f), Size(w * 0.60f, h * 0.30f), 1),
            )
            val step = w / 18f
            val dots = buildList {
                var y = step / 2
                while (y < h) {
                    var x = step / 2
                    while (x < w) { add(Offset(x, y)); x += step }
                    y += step
                }
            }
            val stroke = Stroke(1.2f)
            onDrawBehind {
                drawRect(N.Black)
                val edge = N.White.copy(alpha = 0.08f)
                blocks.forEach { (o, s, kind) ->
                    drawRoundRect(if (kind == 1) N.Block1 else N.Block2, o, s, r)
                    drawRoundRect(edge, o, s, r, style = stroke)
                }
                drawPoints(dots, PointMode.Points, N.White.copy(alpha = 0.06f), strokeWidth = 3.2f, cap = StrokeCap.Round)
            }
        },
    )
}

