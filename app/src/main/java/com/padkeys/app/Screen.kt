package com.padkeys.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun PadKeysScreen(state: PadKeysState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TopBar(state, Modifier.fillMaxWidth().height(56.dp))

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                Modifier.weight(0.27f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SetlistPanel(state, Modifier.weight(214f).fillMaxWidth())
                DronePanel(state, Modifier.weight(226f).fillMaxWidth())
            }
            Column(
                Modifier.weight(0.73f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChannelsPanel(state, Modifier.weight(1f).fillMaxWidth())
                ScenesBar(state, Modifier.fillMaxWidth().height(64.dp))
            }
        }

        BottomSection(state, Modifier.fillMaxWidth().height(176.dp))
    }
}

// ---------------------------------------------------------------- Top bar

@Composable
fun TopBar(state: PadKeysState, modifier: Modifier) {
    Panel(modifier) {
        Row(
            Modifier.fillMaxSize().padding(start = 20.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "PADKEYS",
                color = C.AccentSoft,
                fontFamily = Condensed,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = 1.8.sp,
            )
            Spacer(Modifier.weight(1f))
            SectionLabel("Master Volume")
            Spacer(Modifier.width(14.dp))
            HFader(state.master, { state.master = it }, C.Accent, Modifier.width(260.dp).height(28.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                (state.master * 100).roundToInt().toString(),
                Modifier.width(32.dp),
                color = C.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { /* TODO: buksan ang menu */ },
                contentAlignment = Alignment.Center,
            ) {
                HamburgerIcon(Modifier.size(24.dp), C.Text)
            }
        }
    }
}

// ---------------------------------------------------------------- Setlist & Patch

@Composable
fun SetlistPanel(state: PadKeysState, modifier: Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.selectedSong) {
        if (state.selectedSong in state.songs.indices) listState.animateScrollToItem(state.selectedSong)
    }

    Panel(modifier) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SectionLabel("Setlist & Patch", Modifier.height(24.dp))

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(state.songs) { i, song ->
                    SongRow(i + 1, song, selected = i == state.selectedSong) { state.selectedSong = i }
                }
            }

            Row(
                Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextAction("Prev") { state.prevSong() }
                val plusShape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(plusShape)
                        .border(2.dp, Color(0xFF5A6692), plusShape)
                        .clickable(role = Role.Button) { state.addSong() },
                    contentAlignment = Alignment.Center,
                ) {
                    PlusIcon(Modifier.size(22.dp), Color(0xFFDDE3FA))
                }
                TextAction("Next") { state.nextSong() }
            }
        }
    }
}

@Composable
private fun SongRow(number: Int, song: Song, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(if (selected) C.AccentDeep else C.Btn)
            .border(1.dp, if (selected) C.Accent else C.BtnBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            number.toString(),
            Modifier.width(16.dp),
            color = Color(0xFFAEBBCC),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Column {
            Text(song.title, color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(song.patch, color = Color(0xFFAEBBCC), fontSize = 11.sp)
        }
    }
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFFB9C2E6), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------- Drone pads

private val DroneNotes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

@Composable
fun DronePanel(state: PadKeysState, modifier: Modifier) {
    Panel(modifier) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Drone Pads")
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { /* TODO: settings ng drone pads */ },
                    contentAlignment = Alignment.Center,
                ) {
                    SlidersIcon(Modifier.size(24.dp), C.Text)
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (row in 0 until 3) {
                    Row(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (col in 0 until 4) {
                            val idx = row * 4 + col
                            val on = state.dronePad == idx
                            Btn(
                                Modifier.weight(1f).fillMaxHeight(),
                                selected = on,
                                onClick = { state.dronePad = if (on) -1 else idx },
                            ) {
                                Text(
                                    DroneNotes[idx],
                                    color = if (on) C.Ink else C.Text,
                                    fontFamily = Condensed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Channel sliders

@Composable
fun ChannelsPanel(state: PadKeysState, modifier: Modifier) {
    Panel(modifier) {
        Row(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.channels.forEach { ch ->
                ChannelStrip(ch, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
fun ChannelStrip(ch: ChannelState, modifier: Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .clip(shape)
            .background(C.Strip)
            .drawBehind { drawRect(ch.color, size = Size(size.width, 4.dp.toPx())) }
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.height(44.dp), verticalArrangement = Arrangement.Center) {
            Text(
                "CH ${ch.number}",
                color = ch.color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.9.sp,
            )
            Text(ch.name, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallToggle("M", ch.mute, ch.color, Modifier.weight(1f).height(44.dp)) { ch.mute = !ch.mute }
            SmallToggle("S", ch.solo, ch.color, Modifier.weight(1f).height(44.dp)) { ch.solo = !ch.solo }
            SmallToggle("FX", ch.fx, ch.color, Modifier.weight(1f).height(44.dp)) { ch.fx = !ch.fx }
        }

        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VFader(ch.volume, { ch.volume = it }, ch.color, Modifier.weight(1f).width(56.dp))
            Text(
                (ch.volume * 100).roundToInt().toString(),
                color = C.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ---------------------------------------------------------------- Scenes

@Composable
fun ScenesBar(state: PadKeysState, modifier: Modifier) {
    Panel(modifier) {
        Row(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("Scenes", Modifier.padding(end = 8.dp))

            state.scenes.forEachIndexed { i, _ ->
                Btn(
                    Modifier.width(56.dp).height(44.dp),
                    selected = state.scene == i,
                    onClick = { state.selectScene(i) },
                ) {
                    Text(
                        "${i + 1}",
                        color = if (state.scene == i) C.Ink else C.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Add | Save
            val pill = RoundedCornerShape(8.dp)
            Row(
                Modifier
                    .height(44.dp)
                    .clip(pill)
                    .background(C.Btn)
                    .border(1.dp, C.BtnBorder, pill),
            ) {
                PillAction("Add") { state.addScene() }
                Box(Modifier.width(1.dp).fillMaxHeight().background(C.BtnBorder))
                PillAction("Save") { state.saveScene() }
            }

            Spacer(Modifier.width(8.dp))

            // Layers
            val big = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .size(48.dp)
                    .clip(big)
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A3A5E), Color(0xFF17213A))))
                    .border(1.dp, Color(0xFF3A4C78), big)
                    .clickable(role = Role.Button) { /* TODO: layers editor */ },
                contentAlignment = Alignment.Center,
            ) {
                LayersIcon(Modifier.size(28.dp), Color.White)
            }

            // Keyboard
            Box(
                Modifier
                    .size(48.dp)
                    .clip(big)
                    .background(Color(0x806EB4FF))
                    .border(1.dp, Color(0x9987BEFF), big)
                    .clickable(role = Role.Button) { /* TODO: keyboard settings */ },
                contentAlignment = Alignment.Center,
            ) {
                PianoIcon(Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun PillAction(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .width(72.dp)
            .fillMaxHeight()
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------- Pitch / Mod / Layers preview / Keys

@Composable
fun BottomSection(state: PadKeysState, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Wheel("PITCH", state.pitch, { state.pitch = it }, springTo = 0.5f, modifier = Modifier.width(52.dp).fillMaxHeight())
        Wheel("MOD", state.mod, { state.mod = it }, springTo = null, modifier = Modifier.width(52.dp).fillMaxHeight())

        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LayerBars(state, Modifier.fillMaxWidth().height(38.dp))
            PianoKeyboard(state, Modifier.weight(1f).fillMaxWidth())
        }

        Column(
            Modifier.width(52.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(25, 61, 88).forEach { n ->
                Btn(
                    Modifier.weight(1f).fillMaxWidth(),
                    selected = state.keyCount == n,
                    onClick = { state.keyCount = n },
                ) {
                    Text(
                        n.toString(),
                        color = if (state.keyCount == n) C.Ink else C.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun Wheel(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    springTo: Float?,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .background(C.Track, shape)
            .border(1.dp, C.BtnBorder, shape)
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VFader(
            value,
            onChange,
            C.Accent,
            Modifier.weight(1f).fillMaxWidth(),
            capWidth = 38.dp,
            trackWidth = 4.dp,
            trackColor = Color(0xFF232B37),
            showFill = false,
            onRelease = springTo?.let { target -> { onChange(target) } },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = C.Muted,
            fontFamily = Condensed,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun LayerBars(state: PadKeysState, modifier: Modifier) {
    Canvas(modifier) {
        val barH = 6.dp.toPx()
        val gap = 2.dp.toPx()
        val total = state.channels.size * barH + (state.channels.size - 1) * gap
        val top = (size.height - total) / 2f
        state.channels.forEachIndexed { i, ch ->
            val y = top + i * (barH + gap)
            drawRoundRect(C.Strip, Offset(0f, y), Size(size.width, barH), CornerRadius(barH / 2f))
            drawRoundRect(
                ch.color,
                Offset(ch.rangeStart * size.width, y),
                Size((ch.rangeEnd - ch.rangeStart) * size.width, barH),
                CornerRadius(barH / 2f),
            )
        }
    }
}
