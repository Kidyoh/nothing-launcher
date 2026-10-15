package com.kidyoh.nothinglauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidyoh.nothinglauncher.data.Prefs

data class Palette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val fg: Color,
    val grey: Color,
    val block1: Color,
    val block2: Color,
)

private val DarkPalette = Palette(
    isDark = true,
    bg = Color(0xFF0E0E0E),
    surface = Color(0xFF1B1B1B),
    surfaceHigh = Color(0xFF262626),
    outline = Color(0xFF333333),
    fg = Color(0xFFF2F2F2),
    grey = Color(0xFF8A8A8A),
    block1 = Color(0xFF171717),
    block2 = Color(0xFF1D1D1D),
)

private val LightPalette = Palette(
    isDark = false,
    bg = Color(0xFFEDEDED),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFE0E0E0),
    outline = Color(0xFFC9C9C9),
    fg = Color(0xFF111111),
    grey = Color(0xFF6B6B6B),
    block1 = Color(0xFFE4E4E4),
    block2 = Color(0xFFF5F5F5),
)

/**
 * Nothing colours. The palette is snapshot state, so anything that reads these (composables,
 * Canvas draw lambdas) updates by itself when the theme switches.
 * Names are semantic for dark mode: in light mode "Black" is the light background and "White" the dark ink.
 */
object N {
    var palette by mutableStateOf(DarkPalette)
        private set

    val Black get() = palette.bg
    val Surface get() = palette.surface
    val SurfaceHigh get() = palette.surfaceHigh
    val Outline get() = palette.outline
    val White get() = palette.fg
    val Grey get() = palette.grey
    val Block1 get() = palette.block1
    val Block2 get() = palette.block2
    val Red = Color(0xFFD71921)
    /** Always-white ink for content drawn on red. */
    val OnRed = Color(0xFFFFFFFF)

    fun setDark(dark: Boolean) {
        val p = if (dark) DarkPalette else LightPalette
        if (palette != p) palette = p
    }
}

val Mono = FontFamily.Monospace

object NType {
    val label get() = TextStyle(fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.5.sp, color = N.Grey)
    val body get() = TextStyle(fontFamily = Mono, fontSize = 13.sp, color = N.White)
    val bodySmall get() = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = N.Grey)
    val title get() = TextStyle(fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = N.White)
}

/** Resolves the Dark / Light / System preference. */
@Composable
fun rememberIsDark(): Boolean {
    val mode by Prefs.themeMode.collectAsStateWithLifecycle()
    val system = isSystemInDarkTheme()
    return when (mode) {
        Prefs.THEME_LIGHT -> false
        Prefs.THEME_SYSTEM -> system
        else -> true
    }
}

@Composable
fun NothingTheme(content: @Composable () -> Unit) {
    val dark = rememberIsDark()
    // Written before any child reads the palette in this composition, so there is no extra frame.
    N.setDark(dark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = N.Red, onPrimary = N.OnRed, background = N.Black, surface = N.Surface,
            onSurface = N.White, surfaceVariant = N.SurfaceHigh, secondary = N.White,
            surfaceContainer = N.Surface,
        )
    } else {
        lightColorScheme(
            primary = N.Red, onPrimary = N.OnRed, background = N.Black, surface = N.Surface,
            onSurface = N.White, surfaceVariant = N.SurfaceHigh, secondary = N.White,
            surfaceContainer = N.Surface,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
