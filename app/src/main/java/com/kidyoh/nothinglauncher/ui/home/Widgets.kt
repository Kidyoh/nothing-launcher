package com.kidyoh.nothinglauncher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidyoh.nothinglauncher.data.Battery
import com.kidyoh.nothinglauncher.data.formatBytes
import com.kidyoh.nothinglauncher.data.rememberFreeRam
import com.kidyoh.nothinglauncher.service.MediaInfo
import com.kidyoh.nothinglauncher.ui.components.DotText
import com.kidyoh.nothinglauncher.ui.theme.N
import com.kidyoh.nothinglauncher.ui.theme.NType
import java.util.Calendar

/** Red pill showing free memory, with a small dot glyph (top-left pill in the reference). */
@Composable
fun RamPill(modifier: Modifier, onClick: () -> Unit) {
    val ram by rememberFreeRam()
    Row(
        modifier.clip(CircleShape).background(N.Red).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val step = size.width / 4
            for (i in 0 until 4) for (j in 0 until 4) {
                drawCircle(N.OnRed, step * 0.3f, Offset(step / 2 + i * step, step / 2 + j * step))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(formatBytes(ram), style = NType.body.copy(fontSize = 14.sp, color = N.OnRed), maxLines = 1)
    }
}

@Composable
fun DatePill(now: Calendar, modifier: Modifier, onClick: () -> Unit) {
    val months = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUNE", "JULY", "AUG", "SEPT", "OCT", "NOV", "DEC")
    Box(modifier.clip(CircleShape).background(N.White).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(
            "${now.get(Calendar.DAY_OF_MONTH)} ${months[now.get(Calendar.MONTH)]} ${now.get(Calendar.YEAR)}",
            style = NType.body.copy(color = N.Black, fontSize = 14.sp),
            maxLines = 1,
        )
    }
}

/** 2x2 battery widget: segmented battery like the second reference, dot-matrix percentage. */
@Composable
fun BatteryWidget(b: Battery, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(32.dp)).background(N.Surface).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BATTERY", style = NType.label.copy(color = N.White), modifier = Modifier.weight(1f))
            if (b.charging) Box(Modifier.size(8.dp).clip(CircleShape).background(N.Red))
        }
        Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val bodyW = size.width * 0.92f
            val stroke = 2.dp.toPx()
            drawRoundRect(
                N.White, size = androidx.compose.ui.geometry.Size(bodyW, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
            )
            drawRoundRect(
                N.White, topLeft = Offset(bodyW + 2.dp.toPx(), size.height * 0.3f),
                size = androidx.compose.ui.geometry.Size(size.width - bodyW - 2.dp.toPx(), size.height * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )
            val segments = 14
            val pad = 4.dp.toPx()
            val segW = (bodyW - pad * 2) / segments
            val lit = (b.level * segments + 50) / 100
            for (i in 0 until segments) {
                drawRect(
                    if (i < lit) (if (b.level <= 15) N.Red else N.White) else N.SurfaceHigh,
                    topLeft = Offset(pad + i * segW + segW * 0.15f, pad),
                    size = androidx.compose.ui.geometry.Size(segW * 0.7f, size.height - pad * 2),
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            DotText("${b.level}", dot = 4.dp, color = N.White)
            Spacer(Modifier.width(4.dp))
            Text("%", style = NType.body)
        }
        Text(
            b.status.uppercase() + if (b.tempC > 0) " · ${b.tempC.toInt()}°C" else "",
            style = NType.label.copy(color = N.Red, fontSize = 9.sp),
            maxLines = 1,
        )
    }
}

/** Big red square dot-matrix clock (07 / 59). */
@Composable
fun DotClockWidget(now: Calendar, size: Dp, is24h: Boolean, onClick: () -> Unit) {
    val h = now.get(if (is24h) Calendar.HOUR_OF_DAY else Calendar.HOUR).let { if (!is24h && it == 0) 12 else it }
    val m = now.get(Calendar.MINUTE)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(32.dp)).background(N.Red).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val dot = size / 24
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DotText("%02d".format(h), dot = dot, color = N.OnRed, ghost = N.OnRed.copy(alpha = 0.12f))
            Spacer(Modifier.height(dot * 2))
            DotText("%02d".format(m), dot = dot, color = N.OnRed, ghost = N.OnRed.copy(alpha = 0.12f))
        }
    }
}

@Composable
fun DndPill(on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(CircleShape).background(if (on) N.Red else N.Surface).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(N.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.DoNotDisturbOn, null, tint = if (on) N.Red else N.Black, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("Do Not Disturb", style = NType.body.copy(fontSize = 12.sp, color = if (on) N.OnRed else N.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Album-art square with play/next, styled like the black & white music tile in the reference. */
@Composable
fun MediaWidget(m: MediaInfo, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(32.dp)).background(N.White).padding(5.dp)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(N.SurfaceHigh)) {
            m.art?.let {
                Image(
                    it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                )
            }
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(N.Black.copy(alpha = 0.55f)).padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(m.title, style = NType.title.copy(fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.artist, style = NType.bodySmall.copy(fontSize = 10.sp), maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    val tc = m.controller.transportControls
                    Icon(
                        if (m.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, tint = N.OnRed,
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(N.Red)
                            .clickable { if (m.playing) tc.pause() else tc.play() }.padding(4.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.SkipNext, null, tint = N.White,
                        modifier = Modifier.size(26.dp).clip(CircleShape).clickable { tc.skipToNext() }.padding(3.dp),
                    )
                }
            }
        }
    }
}

/** White search bar with the red "SEARCH" button, bottom of the reference home screen. */
@Composable
fun SearchBar(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(CircleShape).background(N.White).clickable(onClick = onClick).padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, tint = N.Black)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clip(CircleShape).background(N.Black).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(N.Red))
            Spacer(Modifier.width(8.dp))
            Text("SEARCH", style = NType.label.copy(color = N.White))
        }
    }
}
