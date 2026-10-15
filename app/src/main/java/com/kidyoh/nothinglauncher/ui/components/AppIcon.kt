package com.kidyoh.nothinglauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidyoh.nothinglauncher.data.AppInfo
import com.kidyoh.nothinglauncher.ui.theme.N

/** Round white app icon; an empty slot shows a dashed-looking "+" placeholder. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCircle(app: AppInfo?, size: Dp, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(if (app == null) Modifier.border(1.dp, N.Outline, CircleShape).background(N.Black.copy(alpha = 0.4f)) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        if (app != null) {
            Image(app.icon, app.label, modifier = Modifier.size(size))
        } else {
            Icon(Icons.Outlined.Add, "Add app", tint = N.Grey, modifier = Modifier.size(size * 0.35f))
        }
    }
}
