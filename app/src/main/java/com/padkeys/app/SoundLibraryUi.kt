package com.padkeys.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Gold = Color(0xFFF2C24B)
private const val TAB_FAV = 0
private const val TAB_SF = 1
private const val TAB_SM = 2

private sealed interface LibRow {
    class Header(val title: String, val count: Int) : LibRow
    class Item(val item: SoundItem) : LibRow
}

/** Lumalabas kapag tinap ang pangalan ng channel (Empty). */
@Composable
fun SoundLibraryDialog(state: PadKeysState) {
    val chIndex = state.channelPopup
    val ch = state.channels.getOrNull(chIndex) ?: return
    val lib = state.library
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by remember(chIndex) { mutableStateOf(TAB_SF) }
    var query by remember(chIndex) { mutableStateOf("") }
    var playing by remember(chIndex) { mutableStateOf("") }
    var confirmDelCh by remember(chIndex) { mutableStateOf(false) }
    var menuItem by remember(chIndex) { mutableStateOf<SoundItem?>(null) }

    val close = { state.channelPopup = -1 }
    BackHandler { if (menuItem != null) menuItem = null else close() }

    // Import gamit ang file picker ng Android
    val sfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        importAll(scope, ctx, uris) { c, u -> lib.importSoundfont(c, u) }
    }
    val smPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        importAll(scope, ctx, uris) { c, u -> lib.importSample(c, u) }
    }

    val all = lib.allItems()
    val favCount = all.count { it.id in lib.favorites }
    val sfCount = all.count { it.isSoundfont }
    val smCount = all.count { !it.isSoundfont }

    val q = query.trim().lowercase()
    var source = when (tab) {
        TAB_FAV -> all.filter { it.id in lib.favorites }
        TAB_SF -> all.filter { it.isSoundfont }
        else -> all.filter { !it.isSoundfont }
    }
    if (q.isNotEmpty()) source = source.filter { it.name.lowercase().contains(q) }

    val rows = ArrayList<LibRow>()
    if (tab == TAB_SF) {
        var last: String? = null
        for (it in source) {
            if (it.group != last) {
                last = it.group
                rows.add(LibRow.Header(it.group, source.count { x -> x.group == it.group }))
            }
            rows.add(LibRow.Item(it))
        }
    } else {
        source.forEach { rows.add(LibRow.Item(it)) }
    }

    Scrim {
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 920.dp)
                .height(640.dp)
                .clip(shape)
                .background(C.Panel)
                .border(1.dp, C.BtnBorder, shape),
        ) {
            // ---- Name
            Row(
                Modifier.fillMaxWidth().height(72.dp).padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(ch.color, CircleShape))
                    Text("CH ${ch.number}", color = ch.color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                }
                PopupLabel("Name")
                TextBox(
                    ch.name,
                    { ch.name = it; ch.instrumentId = null },
                    Modifier.widthIn(max = 360.dp).weight(1f).height(44.dp),
                    placeholder = "Empty",
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = close),
                    contentAlignment = Alignment.Center,
                ) { CloseIcon(Modifier.size(22.dp), C.Muted) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.PanelBorder))

            // ---- Sound Library + Import
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sound Library", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (tab != TAB_FAV) {
                    val sh = RoundedCornerShape(10.dp)
                    Row(
                        Modifier
                            .height(44.dp)
                            .clip(sh)
                            .background(C.AccentDeep)
                            .border(1.dp, C.Accent, sh)
                            .clickable(role = Role.Button) {
                                if (tab == TAB_SF) sfPicker.launch(arrayOf("*/*")) else smPicker.launch(arrayOf("audio/*"))
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ImportIcon(Modifier.size(18.dp), C.Text)
                        Text(
                            if (tab == TAB_SF) "Import Soundfonts" else "Import Audio (MP3, WAV)",
                            color = C.Text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // ---- Search + Delete channel
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextBox(
                    query,
                    { query = it },
                    Modifier.weight(1f).height(48.dp),
                    placeholder = "Search instruments",
                    leading = { SearchIcon(Modifier.size(20.dp), C.Muted) },
                )
                val canDel = state.channels.size > 1
                val dShape = RoundedCornerShape(10.dp)
                Row(
                    Modifier
                        .height(48.dp)
                        .widthIn(min = 48.dp)
                        .alpha(if (canDel) 1f else 0.4f)
                        .clip(dShape)
                        .background(if (confirmDelCh) DangerRed else Color.Transparent)
                        .border(1.dp, if (confirmDelCh) DangerRed else DangerSoft.copy(alpha = 0.45f), dShape)
                        .clickable(enabled = canDel, role = Role.Button) {
                            if (!confirmDelCh) confirmDelCh = true else state.deleteChannel(chIndex)
                        }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    TrashIcon(Modifier.size(20.dp), if (confirmDelCh) Color.White else DangerSoft)
                    if (confirmDelCh) {
                        Text("Delete channel?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.PanelBorder))

            // ---- Dalawang column
            Row(Modifier.weight(1f).fillMaxWidth()) {
                // Unang column: Favorites / Soundfonts / Sampler
                Column(
                    Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF12171E))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TabButton("Favorites", favCount, tab == TAB_FAV, { StarIcon(Modifier.size(20.dp), it, false) }) {
                        tab = TAB_FAV; query = ""; playing = ""
                    }
                    TabButton("Soundfonts", sfCount, tab == TAB_SF, { SoundfontIcon(Modifier.size(20.dp), it) }) {
                        tab = TAB_SF; query = ""; playing = ""
                    }
                    TabButton("Sampler", smCount, tab == TAB_SM, { WaveIcon(Modifier.size(20.dp), it) }) {
                        tab = TAB_SM; query = ""; playing = ""
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(C.PanelBorder))

                // Pangalawang column
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterStart) {
                        PopupLabel(
                            when (tab) {
                                TAB_FAV -> "Favorites"
                                TAB_SF -> "Soundfonts"
                                else -> "Sampler"
                            },
                        )
                    }
                    if (rows.isEmpty()) {
                        Column(
                            Modifier.fillMaxSize().padding(bottom = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                when {
                                    q.isNotEmpty() -> "No results for \"${query.trim()}\""
                                    tab == TAB_FAV -> "No favorites yet"
                                    tab == TAB_SF -> "No soundfonts imported"
                                    else -> "No samples imported"
                                },
                                color = C.Text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                when {
                                    q.isNotEmpty() -> "Try a different name."
                                    tab == TAB_FAV -> "Tap the star on any instrument or sample to add it here."
                                    tab == TAB_SF -> "Tap Import Soundfonts to add .sf2 files."
                                    else -> "Tap Import Audio to add MP3 or WAV files."
                                },
                                color = C.Muted,
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(
                                rows,
                                key = { r ->
                                    when (r) {
                                        is LibRow.Header -> "h:${r.title}"
                                        is LibRow.Item -> r.item.id
                                    }
                                },
                                span = { r ->
                                    if (r is LibRow.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                                },
                            ) { r ->
                                when (r) {
                                    is LibRow.Header -> GroupHeader(r.title, r.count)
                                    is LibRow.Item -> {
                                        val it = r.item
                                        ItemRow(
                                            item = it,
                                            meta = if (tab == TAB_SF) "Instrument" else it.meta,
                                            selected = ch.instrumentId == it.id,
                                            favorite = it.id in lib.favorites,
                                            playing = playing == it.id,
                                            onPick = { ch.name = it.name; ch.instrumentId = it.id },
                                            onLongPress = { if (tab != TAB_FAV) menuItem = it },
                                            onFavorite = { lib.toggleFavorite(it.id) },
                                            onPlay = { playing = if (playing == it.id) "" else it.id },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- Delete instrument (long press)
        menuItem?.let { m ->
            Scrim(alpha = 0.55f) {
                val dShape = RoundedCornerShape(16.dp)
                Column(
                    Modifier
                        .width(340.dp)
                        .clip(dShape)
                        .background(C.Strip)
                        .border(1.dp, C.BtnBorder, dShape)
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier.size(48.dp).background(DangerRed.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { TrashIcon(Modifier.size(24.dp), DangerSoft) }
                    Spacer(Modifier.height(2.dp))
                    Text("Delete instrument?", color = C.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(m.name, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (m.isSoundfont) m.group else "${m.badge} · ${m.meta}",
                        color = C.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val bShape = RoundedCornerShape(10.dp)
                        Btn(Modifier.weight(1f).height(46.dp), shape = bShape, base = C.BtnSmall, onClick = { menuItem = null }) {
                            Text("Cancel", color = C.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Btn(
                            Modifier.weight(1f).height(46.dp),
                            selected = true,
                            selectedColor = DangerRed,
                            shape = bShape,
                            onClick = {
                                if (ch.instrumentId == m.id) ch.instrumentId = null
                                if (playing == m.id) playing = ""
                                lib.delete(m)
                                menuItem = null
                            },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TrashIcon(Modifier.size(18.dp), Color.White)
                                Text("Delete", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun importAll(
    scope: kotlinx.coroutines.CoroutineScope,
    ctx: Context,
    uris: List<Uri>,
    one: (Context, Uri) -> Unit,
) {
    if (uris.isEmpty()) return
    val app = ctx.applicationContext
    scope.launch {
        withContext(Dispatchers.IO) {
            for (u in uris) {
                try {
                    one(app, u)
                } catch (_: Exception) {
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    count: Int,
    selected: Boolean,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val fg = if (selected) C.Text else Color(0xFFB9C2D0)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (selected) C.AccentDeep else Color.Transparent)
            .border(1.dp, if (selected) C.Accent else Color.Transparent, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon(fg)
        Text(label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
        Box(
            Modifier
                .widthIn(min = 24.dp)
                .background(Color(0x14FFFFFF), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) { Text(count.toString(), color = fg, fontSize = 12.sp) }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 2.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FileIcon(Modifier.size(16.dp), C.Muted)
        Text(title, color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text(if (count == 1) "$count instrument" else "$count instruments", color = C.Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ItemRow(
    item: SoundItem,
    meta: String,
    selected: Boolean,
    favorite: Boolean,
    playing: Boolean,
    onPick: () -> Unit,
    onLongPress: () -> Unit,
    onFavorite: () -> Unit,
    onPlay: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (selected) C.AccentDeep else C.Strip, shape)
            .border(1.dp, if (selected) C.Accent else C.PanelBorder, shape)
            .padding(start = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Preview (▶). Visual lang muna; ikokonekta sa sound engine sa susunod.
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (playing) C.Accent else Color.Transparent)
                .border(1.dp, if (playing) C.Accent else C.BtnBorder, CircleShape)
                .clickable(role = Role.Button, onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            if (playing) PauseIcon(Modifier.size(18.dp), C.Ink) else PlayIcon(Modifier.size(18.dp), C.Text)
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(item.id) {
                    detectTapGestures(onTap = { onPick() }, onLongPress = { onLongPress() })
                },
            verticalArrangement = Arrangement.Center,
        ) {
            Text(item.name, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, color = C.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        if (selected) CheckIcon(Modifier.size(18.dp), Color(0xFF8FBBFF))
        Box(
            Modifier.background(C.PanelBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
        ) { Text(item.badge, color = Color(0xFFB9C2D0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
        Box(
            Modifier.size(44.dp).clickable(role = Role.Button, onClick = onFavorite),
            contentAlignment = Alignment.Center,
        ) { StarIcon(Modifier.size(22.dp), if (favorite) Gold else C.Muted, favorite) }
    }
}
