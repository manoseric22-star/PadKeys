package com.padkeys.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private val WHITE_PITCH_CLASSES = setOf(0, 2, 4, 5, 7, 9, 11)

private fun isWhiteKey(note: Int) = note % 12 in WHITE_PITCH_CLASSES

private const val BLACK_KEY_HEIGHT = 0.62f
private const val BLACK_KEY_WIDTH = 0.6f // kaugnay ng lapad ng puting key

/** Ang hanay ng mga key (MIDI note numbers). 60 = Middle C. */
class KeyLayout(val first: Int, val last: Int) {
    val whites: List<Int> = (first..last).filter { isWhiteKey(it) }
    val blacks: List<Int> = (first..last).filter { !isWhiteKey(it) }
    private val whiteIndex: Map<Int, Int> = whites.withIndex().associate { it.value to it.index }

    /** Posisyon (bilang ng puting key mula sa kaliwa) kung saan nakaupo ang black key. */
    fun boundaryOfBlack(note: Int): Int = (whiteIndex[note - 1] ?: 0) + 1

    fun noteAt(x: Float, y: Float, w: Float, h: Float): Int? {
        if (x < 0f || x >= w || y < 0f || y > h) return null
        val whiteW = w / whites.size
        if (y < h * BLACK_KEY_HEIGHT) {
            val b = (x / whiteW).roundToInt()
            if (abs(x - b * whiteW) < whiteW * BLACK_KEY_WIDTH / 2f) {
                val idx = b - 1
                if (idx in whites.indices) {
                    val n = whites[idx] + 1
                    if (!isWhiteKey(n) && n <= last) return n
                }
            }
        }
        val i = (x / whiteW).toInt().coerceIn(0, whites.size - 1)
        return whites[i]
    }

    companion object {
        fun forKeyCount(count: Int): KeyLayout = when (count) {
            88 -> KeyLayout(21, 108) // A0 - C8
            25 -> KeyLayout(48, 72)  // C3 - C5
            else -> KeyLayout(36, 96) // 61 keys: C2 - C7
        }
    }
}

@Composable
fun PianoKeyboard(state: PadKeysState, modifier: Modifier = Modifier) {
    val layout = remember(state.keyCount) { KeyLayout.forKeyCount(state.keyCount) }
    val shape = RoundedCornerShape(10.dp)

    LaunchedEffect(layout) { state.pressed = emptySet() }

    Box(
        modifier
            .background(C.Track, shape)
            .border(1.dp, C.BtnBorder, shape)
            .padding(4.dp),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(layout) {
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    awaitPointerEventScope {
                        val active = HashMap<PointerId, Int>()
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                for (ch in event.changes) {
                                    if (ch.pressed) {
                                        val n = layout.noteAt(ch.position.x, ch.position.y, w, h)
                                        val prev = active[ch.id]
                                        if (n != prev) {
                                            if (prev != null) {
                                                state.noteOff(prev)
                                                active.remove(ch.id)
                                            }
                                            if (n != null) {
                                                active[ch.id] = n
                                                state.noteOn(n)
                                            }
                                        }
                                        ch.consume()
                                    } else {
                                        active.remove(ch.id)?.let { state.noteOff(it) }
                                    }
                                }
                            }
                        } finally {
                            active.values.forEach { state.noteOff(it) }
                        }
                    }
                },
        ) {
            val pressed = state.pressed
            val w = size.width
            val h = size.height
            val whiteW = w / layout.whites.size
            val edge = 1.dp.toPx()

            layout.whites.forEachIndexed { i, n ->
                drawRect(
                    if (n in pressed) C.AccentSoft else C.Key,
                    Offset(i * whiteW, 0f),
                    Size(whiteW, h),
                )
                drawLine(C.KeyEdge, Offset((i + 1) * whiteW, 0f), Offset((i + 1) * whiteW, h), strokeWidth = edge)
            }

            val blackW = whiteW * BLACK_KEY_WIDTH
            val blackH = h * BLACK_KEY_HEIGHT
            val corner = CornerRadius(3.dp.toPx())
            layout.blacks.forEach { n ->
                val cx = layout.boundaryOfBlack(n) * whiteW
                val topLeft = Offset(cx - blackW / 2f, 0f)
                drawRoundRect(if (n in pressed) C.Accent else C.BlackKey, topLeft, Size(blackW, blackH), corner)
                drawRoundRect(C.BtnBorder, topLeft, Size(blackW, blackH), corner, style = Stroke(edge))
            }

            // Layers editor: kulayan ang saklaw ng napiling channel
            if (state.layerMode) {
                state.channels.getOrNull(state.layerSel)?.let { ch ->
                    drawRect(
                        ch.color.copy(alpha = 0.4f),
                        Offset(ch.lo.toFloat() / KEY_STEPS * w, 0f),
                        Size((ch.hi - ch.lo).toFloat() / KEY_STEPS * w, h),
                    )
                }
            }
        }
    }
}
