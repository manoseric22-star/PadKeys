package com.padkeys.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val PadNoteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/** Ambient Pads card: C..B keys, settings icon sa taas-kanan, vertical pad volume. */
@Composable
fun AmbientPanel(state: PadKeysState, modifier: Modifier) {
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
                Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Ambient Pads")
                    // Ang napiling sound preset: nasa gitna, sa pagitan ng pangalan at ng settings icon
                    Box(Modifier.weight(1f).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                        Text(
                            state.preset.label,
                            color = C.AccentSoft,
                            fontFamily = Condensed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                        .clickable(role = Role.Button) { state.padSettingsOpen = !state.padSettingsOpen },
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
    }
}

/**
 * Pad Settings: nasa loob ng screen. Kasing-lapad ng 3 channel at kasing-taas ng Channels panel;
 * ang posisyon ay galing sa PadKeysScreen.
 */
@Composable
fun AmbientSettingsDialog(state: PadKeysState, left: Dp, width: Dp, top: Dp, bottom: Dp) {
    val close = { state.padSettingsOpen = false }
    BackHandler(onBack = close)

    Scrim(alpha = 0.45f) {
        val shape = RoundedCornerShape(12.dp)
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = left, top = top, bottom = bottom)
                .width(width)
                .fillMaxHeight()
                .background(C.Panel, shape)
                .border(1.dp, C.BtnBorder, shape)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Pad Settings")
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = close),
                    contentAlignment = Alignment.Center,
                ) { CloseIcon(Modifier.size(18.dp), C.Muted) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.PanelBorder))

            Text(
                "SOUND",
                color = C.Muted,
                fontFamily = Condensed,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
            )
            // Presets: lumalaki para punuin ang taas
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in PadPreset.entries.chunked(3)) {
                    Row(
                        Modifier.weight(1f).fillMaxWidth().heightIn(max = 64.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (p in row) {
                            val on = state.preset == p
                            Btn(
                                Modifier.weight(1f).fillMaxHeight(),
                                selected = on,
                                base = C.BtnSmall,
                                onClick = { state.preset = p },
                            ) {
                                Text(
                                    p.label,
                                    color = if (on) C.Ink else C.Text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.PanelBorder))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.weight(1f)) {
                    SettingSlider("Tone", "Warm", "Bright", state.tone) { state.tone = it }
                }
                Box(Modifier.weight(1f)) {
                    SettingSlider("Brightness", "Soft", "Shimmer", state.brightness) { state.brightness = it }
                }
            }
        }
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
            Text(
                label.uppercase(),
                color = C.Muted,
                fontFamily = Condensed,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                (value * 100).roundToInt().toString(),
                color = C.Text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        HFader(value, onChange, C.Accent, Modifier.fillMaxWidth().height(28.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(low, color = C.Muted, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text(high, color = C.Muted, fontSize = 9.sp)
        }
    }
}
