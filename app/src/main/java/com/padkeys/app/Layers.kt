package com.padkeys.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private fun noteName(idx: Int): String = "CDEFGAB"[idx % 7].toString() + (2 + idx / 7)

private const val MOVE = 0
private const val LOW = 1
private const val HIGH = 2

/** Layers editor: i-drag ang bar o ang handles para itakda ang saklaw ng bawat channel sa keyboard. */
@Composable
fun LayerEditor(state: PadKeysState, modifier: Modifier) {
    Panel(modifier) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SectionLabel("Layers")
                    Text("Drag the handles to set the key range of each channel.", color = C.Muted, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val big = RoundedCornerShape(12.dp)
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(big)
                            .background(Color(0x806EB4FF))
                            .border(1.dp, Color(0x9987BEFF), big)
                            .clickable(role = Role.Button) { state.layerMode = false },
                        contentAlignment = Alignment.Center,
                    ) { LayersIcon(Modifier.size(28.dp), Color.White) }
                    KeyboardToggle(state)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.PanelBorder))

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 12.dp),
            ) {
                // Ang lapad ng track ay kapareho ng lapad ng keyboard sa ibaba
                BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 124.dp, end = 64.dp)) {
                    val trackPx = constraints.maxWidth.toFloat()
                    val trackDp = maxWidth

                    Column(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().height(22.dp)) {
                            for (k in 0..5) {
                                Text(
                                    "C${2 + k}",
                                    color = C.Muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.offset(x = trackDp * (k * 7f / KEY_STEPS) - 8.dp),
                                )
                            }
                        }
                        state.channels.forEachIndexed { i, ch ->
                            LayerRow(state, i, ch, trackPx, trackDp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerRow(
    state: PadKeysState,
    index: Int,
    ch: ChannelState,
    trackPx: Float,
    trackDp: Dp,
) {
    val selected = state.layerSel == index
    val left = trackDp * (ch.lo.toFloat() / KEY_STEPS)
    val width = trackDp * ((ch.hi - ch.lo).toFloat() / KEY_STEPS)
    val right = left + width
    val barShape = RoundedCornerShape(8.dp)

    Box(Modifier.fillMaxWidth().height(54.dp)) {
        // Mga octave guide
        for (k in 0..5) {
            Box(
                Modifier
                    .offset(x = trackDp * (k * 7f / KEY_STEPS))
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0x17FFFFFF)),
            )
        }

        // Bar (drag = ilipat ang buong saklaw)
        Row(
            Modifier
                .offset(x = left, y = 6.dp)
                .width(width)
                .height(42.dp)
                .clip(barShape)
                .background(ch.color)
                .then(if (selected) Modifier.border(2.dp, Color(0xD9FFFFFF), barShape) else Modifier)
                .dragRange(trackPx, ch, MOVE) { state.layerSel = index },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            NoteChip(noteName(ch.lo))
            Text(
                ch.name.ifBlank { "Empty" },
                color = C.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            NoteChip(noteName(ch.hi - 1))
        }

        Handle(ch, trackPx, LOW, left, selected) { state.layerSel = index }
        Handle(ch, trackPx, HIGH, right, selected) { state.layerSel = index }
    }
}

@Composable
private fun NoteChip(text: String) {
    Box(
        Modifier
            .background(C.Track, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(text, color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun Handle(
    ch: ChannelState,
    trackPx: Float,
    mode: Int,
    x: Dp,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    // 44dp na lugar ng pindot; 22dp ang nakikitang bilog
    Box(
        Modifier
            .offset(x = x - 22.dp, y = 5.dp)
            .size(44.dp)
            .dragRange(trackPx, ch, mode, onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .then(if (selected) Modifier.border(4.dp, Color(0x47FFFFFF), CircleShape) else Modifier)
                .background(Color(0xFFE8ECF2), CircleShape)
                .padding(3.dp)
                .background(Color(0xFF4B5575), CircleShape),
        )
    }
}

private fun Modifier.dragRange(
    trackPx: Float,
    ch: ChannelState,
    mode: Int,
    onSelect: () -> Unit,
): Modifier = pointerInput(trackPx, mode, ch) {
    var startLo = 0
    var startHi = 0
    var acc = 0f
    detectDragGestures(
        onDragStart = {
            startLo = ch.lo
            startHi = ch.hi
            acc = 0f
            onSelect()
        },
    ) { change, drag ->
        change.consume()
        acc += drag.x
        val dk = (acc / trackPx * KEY_STEPS).roundToInt()
        when (mode) {
            MOVE -> {
                val w = startHi - startLo
                val lo = (startLo + dk).coerceIn(0, KEY_STEPS - w)
                ch.setRange(lo, lo + w)
            }
            LOW -> ch.setRange((startLo + dk).coerceIn(0, startHi - 1), startHi)
            else -> ch.setRange(startLo, (startHi + dk).coerceIn(startLo + 1, KEY_STEPS))
        }
    }
}
