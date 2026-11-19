package com.padkeys.app

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

private val PadNoteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/** Ambient Pads card: C..B keys, settings icon sa taas-kanan, vertical pad volume. */
@Composable
fun AmbientPanel(state: PadKeysState, modifier: Modifier) {
    var showSettings by remember { mutableStateOf(false) }

    Panel(modifier) {
        Row(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Kaliwa: label + 12 keys
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
                    SectionLabel("Ambient Pads")
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
                                    // Tap ulit sa aktibong key = fade out
                                    onClick = { state.dronePad = if (on) -1 else idx },
                                ) {
                                    Text(
                                        PadNoteNames[idx],
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

            // Kanan: settings icon + vertical volume + value
            Column(
                Modifier.width(52.dp).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { showSettings = !showSettings },
                    contentAlignment = Alignment.Center,
                ) {
                    SlidersIcon(Modifier.size(24.dp), C.Text)
                }
                VFader(
                    value = state.padVolume,
                    onValueChange = { state.padVolume = it },
                    color = C.Accent,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    capWidth = 44.dp,
                )
                Text(
                    (state.padVolume * 100).roundToInt().toString(),
                    color = C.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (showSettings) {
            Popup(
                popupPositionProvider = SidePopupPosition,
                onDismissRequest = { showSettings = false },
                properties = PopupProperties(focusable = true),
            ) {
                AmbientSettings(state, onClose = { showSettings = false })
            }
        }
    }
}

// Lalabas sa kanan ng Ambient Pads card, nakaayon sa ilalim nito.
private object SidePopupPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gap = 8
        val x = (anchorBounds.right + gap).coerceAtMost(windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val y = (anchorBounds.bottom - popupContentSize.height)
            .coerceAtMost(windowSize.height - popupContentSize.height)
            .coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

@Composable
private fun AmbientSettings(state: PadKeysState, onClose: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .width(400.dp)
            .background(C.Panel, shape)
            .border(1.dp, C.BtnBorder, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Pad Settings")
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = C.Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        SectionLabel("Sound")
        for (pair in PadPreset.entries.chunked(2)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (p in pair) {
                    val on = state.preset == p
                    Btn(
                        Modifier.weight(1f).height(52.dp),
                        selected = on,
                        base = C.BtnSmall,
                        onClick = { state.preset = p },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                p.label,
                                color = if (on) C.Ink else C.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                            Text(
                                p.blurb,
                                color = if (on) C.Ink else C.Muted,
                                fontSize = 10.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        SettingSlider("Tone", "Warm", "Bright", state.tone) { state.tone = it }
        SettingSlider("Brightness", "Soft", "Shimmer", state.brightness) { state.brightness = it }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    low: String,
    high: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(label)
            Spacer(Modifier.weight(1f))
            Text(
                (value * 100).roundToInt().toString(),
                color = C.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        HFader(value, onChange, C.Accent, Modifier.fillMaxWidth().height(28.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(low, color = C.Muted, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text(high, color = C.Muted, fontSize = 10.sp)
        }
    }
}
