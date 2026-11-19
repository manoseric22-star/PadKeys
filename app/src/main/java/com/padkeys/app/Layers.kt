package com.padkeys.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.remember
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


/** Pangalan ng note, hal. 60 -> C4, 42 -> F#2 */
private fun noteName(note: Int): String {
    val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    return names[note % 12] + (note / 12 - 1)
}

private const val MOVE = 0
private const val LOW = 1
private const val HIGH = 2

/** Layers editor: tap ang strip para piliin, i-drag ang bar o ang handles para itakda ang saklaw sa keyboard. */
@Composable
fun LayerEditor(state: PadKeysState, modifier: Modifier) {
    // Parehong layout ng virtual keys sa ibaba, kaya laging tugma (25 / 61 / 88)
    val layout = remember(state.keyCount) { KeyLayout.forKeyCount(state.keyCount) }
    val steps = layout.whites.size

    Panel(modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SectionLabel("Layers")
                    Text(
                        "Tap a strip to select it, then drag its handles to set the key range.",
                        color = C.Muted,
                        fontSize = 12.sp,
                    )
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
                        // Ruler: C1..C8 sa posisyon ng bawat C sa keyboard
                        Box(Modifier.fillMaxWidth().height(22.dp)) {
                            layout.whites.forEachIndexed { i, n ->
                                if (n % 12 == 0) {
                                    Text(
                                        "C${n / 12 - 1}",
                                        color = C.Muted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.offset(x = trackDp * (i.toFloat() / steps)),
                                    )
                                }
                            }
                        }
                        state.channels.forEachIndexed { i, ch ->
                            LayerRow(state, layout, i, ch, trackPx, trackDp)
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
    layout: KeyLayout,
    index: Int,
    ch: ChannelState,
    trackPx: Float,
    trackDp: Dp,
) {
    val steps = layout.whites.size
    val selected = state.layerSel == index
    val (iLo, iHi) = layout.whiteSpan(ch.lo, ch.hi)
    val left = trackDp * (iLo.toFloat() / steps)
    val width = trackDp * ((iHi - iLo + 1).toFloat() / steps)
    val right = left + width
    val barShape = RoundedCornerShape(8.dp)
    val select = { state.layerSel = index }

    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .pointerInput(index) { detectTapGestures(onTap = { select() }) },
    ) {
        // Guhit sa bawat C
        layout.whites.forEachIndexed { i, n ->
            if (n % 12 == 0) {
                Box(
                    Modifier
                        .offset(x = trackDp * (i.toFloat() / steps))
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0x17FFFFFF)),
                )
            }
        }

        // Bar: tap = piliin, drag = ilipat ang buong saklaw
        Row(
            Modifier
                .offset(x = left, y = 6.dp)
                .width(width)
                .height(42.dp)
                .clip(barShape)
                .background(ch.color)
                .then(if (selected) Modifier.border(2.dp, Color(0xD9FFFFFF), barShape) else Modifier)
                .pointerInput(index) { detectTapGestures(onTap = { select() }) }
                .dragRange(trackPx, layout, ch, MOVE, select),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            NoteChip(noteName(layout.whites[iLo]))
            Text(
                ch.name.ifBlank { "Empty" },
                color = C.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            NoteChip(noteName(layout.whites[iHi]))
        }

        Handle(trackPx, layout, ch, LOW, left, selected, select)
        Handle(trackPx, layout, ch, HIGH, right, selected, select)
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
    trackPx: Float,
    layout: KeyLayout,
    ch: ChannelState,
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
            .dragRange(trackPx, layout, ch, mode, onSelect),
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

/** Ang saklaw ay nasa puting key. Kapag nasa dulo ng keyboard, ginagawang 0/127 para sumunod sa 25/61/88. */
private fun ChannelState.setSpan(layout: KeyLayout, iLo: Int, iHi: Int) {
    val last = layout.whites.lastIndex
    lo = if (iLo <= 0) 0 else layout.whites[iLo]
    hi = if (iHi >= last) 127 else layout.whites[iHi]
}

private fun Modifier.dragRange(
    trackPx: Float,
    layout: KeyLayout,
    ch: ChannelState,
    mode: Int,
    onSelect: () -> Unit,
): Modifier = pointerInput(trackPx, mode, ch, layout) {
    val steps = layout.whites.size
    var startLo = 0
    var startHi = 0
    var acc = 0f
    detectDragGestures(
        onDragStart = {
            val span = layout.whiteSpan(ch.lo, ch.hi)
            startLo = span.first
            startHi = span.second
            acc = 0f
            onSelect()
        },
    ) { change, drag ->
        change.consume()
        acc += drag.x
        val dk = (acc / trackPx * steps).roundToInt()
        when (mode) {
            MOVE -> {
                val w = startHi - startLo
                val lo = (startLo + dk).coerceIn(0, steps - 1 - w)
                ch.setSpan(layout, lo, lo + w)
            }
            LOW -> ch.setSpan(layout, (startLo + dk).coerceIn(0, startHi), startHi)
            else -> ch.setSpan(layout, startLo, (startHi + dk).coerceIn(startLo, steps - 1))
        }
    }
}
