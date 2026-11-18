package com.padkeys.app

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val Bg = Color(0xFF0B0E13)
    val Panel = Color(0xFF151A22)
    val PanelBorder = Color(0xFF232B37)
    val Strip = Color(0xFF1B222C)
    val Btn = Color(0xFF1F2733)
    val BtnSmall = Color(0xFF232C39)
    val BtnBorder = Color(0xFF2F3A49)
    val Text = Color(0xFFE8ECF2)
    val Muted = Color(0xFF9AA7B8)
    val Accent = Color(0xFF3D8BFF)
    val AccentSoft = Color(0xFF5CA0FF)
    val AccentDeep = Color(0xFF1B3A66)
    val Track = Color(0xFF0E1116)
    val Cap = Color(0xFFE8ECF2)
    val Ink = Color(0xFF0B0E13)
    val Key = Color(0xFFF2F4F8)
    val KeyEdge = Color(0xFFB8C0CC)
    val BlackKey = Color(0xFF151A22)
}

val ChannelColors = listOf(
    Color(0xFF3D8BFF), // CH 1 asul
    Color(0xFFF2A93B), // CH 2 amber
    Color(0xFF3FD6A5), // CH 3 berde
    Color(0xFFE56B9A), // CH 4 pink
    Color(0xFFA98BFF), // CH 5 lila
    Color(0xFF38C8E8), // CH 6 cyan
    Color(0xFFC6E04A), // CH 7 lime
    Color(0xFFFF7A59), // CH 8 orange
)
val Condensed = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .background(C.Panel, shape)
            .border(1.dp, C.PanelBorder, shape),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier,
        color = C.Muted,
        fontFamily = Condensed,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 1.5.sp,
    )
}

@Composable
fun Btn(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectedColor: Color = C.Accent,
    base: Color = C.Btn,
    shape: Shape = RoundedCornerShape(8.dp),
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .background(if (selected) selectedColor else base)
            .border(1.dp, if (selected) selectedColor else C.BtnBorder, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun SmallToggle(
    label: String,
    on: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Btn(modifier, selected = on, selectedColor = color, base = C.BtnSmall, onClick = onClick) {
        Text(
            label,
            color = if (on) C.Ink else C.Text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

// Cap ng slider: may kulay na guhit sa gitna (pahalang sa vertical slider, patayo sa horizontal).
private fun DrawScope.drawCap(topLeft: Offset, capSize: Size, lineColor: Color, verticalLine: Boolean) {
    val radius = CornerRadius(6.dp.toPx())
    drawRoundRect(C.Cap, topLeft, capSize, radius)
    drawRoundRect(C.Track, topLeft, capSize, radius, style = Stroke(1.dp.toPx()))
    if (verticalLine) {
        val lw = 4.dp.toPx()
        val lh = capSize.height * 0.7f
        drawRoundRect(
            lineColor,
            Offset(topLeft.x + (capSize.width - lw) / 2f, topLeft.y + (capSize.height - lh) / 2f),
            Size(lw, lh),
            CornerRadius(lw / 2f),
        )
    } else {
        val lh = 4.dp.toPx()
        val lw = capSize.width * 0.7f
        drawRoundRect(
            lineColor,
            Offset(topLeft.x + (capSize.width - lw) / 2f, topLeft.y + (capSize.height - lh) / 2f),
            Size(lw, lh),
            CornerRadius(lh / 2f),
        )
    }
}

@Composable
fun VFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    capWidth: Dp = 56.dp,
    trackWidth: Dp = 10.dp,
    trackColor: Color = C.Track,
    showFill: Boolean = true,
    onRelease: (() -> Unit)? = null,
) {
    val onChange by rememberUpdatedState(onValueChange)
    val onUp by rememberUpdatedState(onRelease)

    Canvas(
        modifier.pointerInput(Unit) {
            val capH = 24.dp.toPx()
            fun toValue(y: Float): Float {
                val usable = (size.height - capH).coerceAtLeast(1f)
                return 1f - ((y - capH / 2f) / usable).coerceIn(0f, 1f)
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                onChange(toValue(down.position.y))
                down.consume()
                drag(down.id) { change ->
                    onChange(toValue(change.position.y))
                    change.consume()
                }
                onUp?.invoke()
            }
        },
    ) {
        val w = size.width
        val h = size.height
        val capW = capWidth.toPx()
        val capH = 24.dp.toPx()
        val trackW = trackWidth.toPx()
        val usable = (h - capH).coerceAtLeast(1f)
        val capTop = (1f - value.coerceIn(0f, 1f)) * usable
        val capMid = capTop + capH / 2f
        val cx = w / 2f

        drawRoundRect(trackColor, Offset(cx - trackW / 2f, 0f), Size(trackW, h), CornerRadius(trackW / 2f))
        if (showFill) {
            drawRoundRect(color, Offset(cx - trackW / 2f, capMid), Size(trackW, h - capMid), CornerRadius(trackW / 2f))
        }
        drawCap(Offset(cx - capW / 2f, capTop), Size(capW, capH), color, verticalLine = false)
    }
}

@Composable
fun HFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val onChange by rememberUpdatedState(onValueChange)

    Canvas(
        modifier.pointerInput(Unit) {
            val capW = 22.dp.toPx()
            fun toValue(x: Float): Float {
                val usable = (size.width - capW).coerceAtLeast(1f)
                return ((x - capW / 2f) / usable).coerceIn(0f, 1f)
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                onChange(toValue(down.position.x))
                down.consume()
                drag(down.id) { change ->
                    onChange(toValue(change.position.x))
                    change.consume()
                }
            }
        },
    ) {
        val w = size.width
        val h = size.height
        val capW = 22.dp.toPx()
        val capH = 28.dp.toPx()
        val trackH = 6.dp.toPx()
        val usable = (w - capW).coerceAtLeast(1f)
        val capLeft = value.coerceIn(0f, 1f) * usable
        val capMid = capLeft + capW / 2f
        val cy = h / 2f

        drawRoundRect(C.Track, Offset(0f, cy - trackH / 2f), Size(w, trackH), CornerRadius(trackH / 2f))
        drawRoundRect(color, Offset(0f, cy - trackH / 2f), Size(capMid, trackH), CornerRadius(trackH / 2f))
        drawCap(Offset(capLeft, cy - capH / 2f), Size(capW, capH), color, verticalLine = true)
    }
}
