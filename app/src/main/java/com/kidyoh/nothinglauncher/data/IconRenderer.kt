package com.kidyoh.nothinglauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * Turns any app icon into a Nothing-style round icon.
 * In monochrome mode it prefers the app's themed (monochrome) layer drawn black on a white disc,
 * and falls back to a desaturated icon.
 */
object IconRenderer {
    private const val WHITE = 0xFFF2F2F2.toInt()
    private const val BLACK = 0xFF0E0E0E.toInt()

    fun render(d: Drawable, size: Int, mono: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = size.toFloat()
        val circle = Path().apply { addCircle(s / 2, s / 2, s / 2, Path.Direction.CW) }
        c.clipPath(circle)
        val grey = if (mono) ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) else null
        val bleed = size / 4 // adaptive layers are 108dp with a 72dp safe zone

        if (d is AdaptiveIconDrawable) {
            val monoLayer = if (mono && Build.VERSION.SDK_INT >= 33) d.monochrome else null
            if (monoLayer != null) {
                c.drawColor(WHITE)
                monoLayer.mutate().apply {
                    setTint(BLACK)
                    setBounds(-bleed, -bleed, size + bleed, size + bleed)
                    draw(c)
                }
            } else {
                c.drawColor(WHITE)
                listOf(d.background, d.foreground).forEach { layer ->
                    layer ?: return@forEach
                    layer.mutate().apply {
                        colorFilter = grey
                        setBounds(-bleed, -bleed, size + bleed, size + bleed)
                        draw(c)
                    }
                }
            }
        } else {
            c.drawPaint(Paint().apply { color = WHITE })
            val inset = (size * 0.18f).toInt()
            d.mutate().apply {
                colorFilter = grey
                setBounds(inset, inset, size - inset, size - inset)
                draw(c)
            }
        }
        return bmp
    }
}
