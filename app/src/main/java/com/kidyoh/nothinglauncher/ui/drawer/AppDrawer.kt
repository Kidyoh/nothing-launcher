package com.kidyoh.nothinglauncher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidyoh.nothinglauncher.data.AppInfo
import com.kidyoh.nothinglauncher.data.AppRepository
import com.kidyoh.nothinglauncher.data.Prefs
import com.kidyoh.nothinglauncher.ui.components.AppCircle
import com.kidyoh.nothinglauncher.ui.components.DotText
import com.kidyoh.nothinglauncher.ui.theme.N
import com.kidyoh.nothinglauncher.ui.theme.NType

@Composable
fun AppDrawer(focusSearch: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val apps by AppRepository.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    LaunchedEffect(focusSearch) { if (focusSearch) runCatching { focus.requestFocus() } }

    fun open(a: AppInfo) {
        keyboard?.hide()
        AppRepository.launch(ctx, a)
        onClose()
    }

    val gridState = rememberLazyGridState()
    // Pull down past the top of the list to close the drawer.
    val closeOnPull = remember {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0) {
                    pulled += available.y
                    if (pulled > 220f) { pulled = 0f; onClose() }
                } else if (consumed.y != 0f) pulled = 0f
                return Offset.Zero
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(N.Black.copy(alpha = 0.96f))
            .systemBarsPadding()
            .imePadding(),
    ) {
        Column(
            Modifier.fillMaxWidth().pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total > 80f) onClose() }) { _, dy -> total += dy }
            }.padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(CircleShape).background(N.Grey))
            Spacer(Modifier.height(16.dp))
            DotText("APPS ${apps.size}", dot = 3.dp, color = N.White)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().height(54.dp).clip(CircleShape).background(N.White).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = N.Black)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search apps", style = NType.body.copy(color = N.Grey))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = NType.body.copy(color = N.Black),
                        cursorBrush = SolidColor(N.Red),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { filtered.firstOrNull()?.let { open(it) } }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            modifier = Modifier.weight(1f).nestedScroll(closeOnPull),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(filtered, key = { it.key }) { a -> DrawerItem(a, onOpen = { open(a) }) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerItem(a: AppInfo, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = { menu = true }).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppCircle(a, 58.dp, onClick = onOpen, onLongClick = { menu = true })
            Spacer(Modifier.height(8.dp))
            Text(
                a.label, style = NType.bodySmall.copy(color = N.White), maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Add to home") }, onClick = {
                menu = false
                if (!Prefs.addFavorite(a.key)) {
                    android.widget.Toast.makeText(ctx, "Home is full — long-press a home icon to replace it", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
            DropdownMenuItem(text = { Text("App info") }, onClick = { menu = false; AppRepository.openAppInfo(ctx, a) })
            DropdownMenuItem(text = { Text("Uninstall") }, onClick = { menu = false; AppRepository.uninstall(ctx, a) })
        }
    }
}
