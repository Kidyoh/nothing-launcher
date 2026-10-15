package com.kidyoh.nothinglauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A 5x7 dot-matrix font, drawn dot by dot, in the spirit of Nothing's NDot typeface.
 * Each glyph is 7 rows of 5 bits (MSB = leftmost column).
 */
object DotFont {
    private val glyphs: Map<Char, IntArray> = mapOf(
        '0' to rows("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
        '1' to rows("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
        '2' to rows("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
        '3' to rows("11111", "00010", "00100", "00010", "00001", "10001", "01110"),
        '4' to rows("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
        '5' to rows("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
        '6' to rows("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
        '7' to rows("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
        '8' to rows("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
        '9' to rows("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
        'A' to rows("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
        'B' to rows("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
        'C' to rows("01110", "10001", "10000", "10000", "10000", "10001", "01110"),
        'D' to rows("11100", "10010", "10001", "10001", "10001", "10010", "11100"),
        'E' to rows("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
        'F' to rows("11111", "10000", "10000", "11110", "10000", "10000", "10000"),
        'G' to rows("01110", "10001", "10000", "10111", "10001", "10001", "01111"),
        'H' to rows("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
        'I' to rows("01110", "00100", "00100", "00100", "00100", "00100", "01110"),
        'J' to rows("00111", "00010", "00010", "00010", "00010", "10010", "01100"),
        'K' to rows("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
        'L' to rows("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
        'M' to rows("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
        'N' to rows("10001", "10001", "11001", "10101", "10011", "10001", "10001"),
        'O' to rows("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
        'P' to rows("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
        'Q' to rows("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
        'R' to rows("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
        'S' to rows("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
        'T' to rows("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
        'U' to rows("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
        'V' to rows("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
        'W' to rows("10001", "10001", "10001", "10101", "10101", "10101", "01010"),
        'X' to rows("10001", "10001", "01010", "00100", "01010", "10001", "10001"),
        'Y' to rows("10001", "10001", "10001", "01010", "00100", "00100", "00100"),
        'Z' to rows("11111", "00001", "00010", "00100", "01000", "10000", "11111"),
        ':' to rows("00000", "00100", "00100", "00000", "00100", "00100", "00000"),
        '%' to rows("11000", "11001", "00010", "00100", "01000", "10011", "00011"),
        '.' to rows("00000", "00000", "00000", "00000", "00000", "01100", "01100"),
        '-' to rows("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
        '/' to rows("00000", "00001", "00010", "00100", "01000", "10000", "00000"),
        '°' to rows("01100", "10010", "10010", "01100", "00000", "00000", "00000"),
        ' ' to rows("00000", "00000", "00000", "00000", "00000", "00000", "00000"),
    )

    private fun rows(vararg r: String) = IntArray(7) { r[it].toInt(2) }

    fun glyph(c: Char): IntArray = glyphs[c.uppercaseChar()] ?: glyphs[' ']!!

    /** Width in dot columns of a string, with one blank column between glyphs. */
    fun columns(text: String) = if (text.isEmpty()) 0 else text.length * 6 - 1
}

/**
 * Renders [text] as a dot matrix. [dot] is the pitch of one dot cell.
 * [ghost] draws the unlit dots faintly, like a real LED matrix.
 */
@Composable
fun DotText(
    text: String,
    modifier: Modifier = Modifier,
    dot: Dp = 4.dp,
    color: Color = Color.White,
    ghost: Color = Color.Transparent,
) {
    val cols = DotFont.columns(text)
    val glyphs = remember(text) { text.map { DotFont.glyph(it) } }
    val pitchPx = with(LocalDensity.current) { dot.toPx() }
    Canvas(modifier.size(dot * cols.coerceAtLeast(1), dot * 7)) {
        val r = pitchPx * 0.40f
        glyphs.forEachIndexed { gi, g ->
            val x0 = gi * 6 * pitchPx
            for (row in 0 until 7) {
                val bits = g[row]
                for (col in 0 until 5) {
                    val on = (bits shr (4 - col)) and 1 == 1
                    val c = if (on) color else ghost
                    if (c.alpha == 0f) continue
                    drawCircle(
                        color = c,
                        radius = r,
                        center = Offset(x0 + col * pitchPx + pitchPx / 2, row * pitchPx + pitchPx / 2),
                    )
                }
            }
        }
    }
}
