package com.kidyoh.nothinglauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object N {
    val Black = Color(0xFF0E0E0E)
    val Surface = Color(0xFF1B1B1B)
    val SurfaceHigh = Color(0xFF262626)
    val Outline = Color(0xFF333333)
    val White = Color(0xFFF2F2F2)
    val Grey = Color(0xFF8A8A8A)
    val Red = Color(0xFFD71921)
}

val Mono = FontFamily.Monospace

object NType {
    val label = TextStyle(fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.5.sp, color = N.Grey)
    val body = TextStyle(fontFamily = Mono, fontSize = 13.sp, color = N.White)
    val bodySmall = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = N.Grey)
    val title = TextStyle(fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = N.White)
}

@Composable
fun NothingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = N.Red,
            onPrimary = N.White,
            background = N.Black,
            surface = N.Surface,
            onSurface = N.White,
            surfaceVariant = N.SurfaceHigh,
            secondary = N.White,
        ),
        content = content,
    )
}
