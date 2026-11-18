package com.padkeys.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

// Lahat ng icon ay iginuhit sa 24x24 na grid at nag-i-scale sa laki ng Modifier.

@Composable
fun HamburgerIcon(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val s = size.width / 24f
        listOf(7f, 12f, 17f).forEach { y ->
            drawLine(color, Offset(4f * s, y * s), Offset(20f * s, y * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun PlusIcon(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val s = size.width / 24f
        drawLine(color, Offset(12f * s, 5f * s), Offset(12f * s, 19f * s), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
        drawLine(color, Offset(5f * s, 12f * s), Offset(19f * s, 12f * s), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
    }
}

@Composable
fun SlidersIcon(modifier: Modifier, color: Color, background: Color = C.Panel) {
    Canvas(modifier) {
        val s = size.width / 24f
        listOf(6f, 12f, 18f).forEach { y ->
            drawLine(color, Offset(4f * s, y * s), Offset(20f * s, y * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
        }
        listOf(Offset(9f, 6f), Offset(15f, 12f), Offset(8f, 18f)).forEach { p ->
            val c = Offset(p.x * s, p.y * s)
            drawCircle(background, radius = 2.4f * s, center = c)
            drawCircle(color, radius = 2.4f * s, center = c, style = Stroke(2f * s))
        }
    }
}

@Composable
fun LayersIcon(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val s = size.width / 24f
        val stroke = Stroke(1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val top = Path().apply {
            moveTo(12f * s, 3.5f * s)
            lineTo(20.5f * s, 7.5f * s)
            lineTo(12f * s, 11.5f * s)
            lineTo(3.5f * s, 7.5f * s)
            close()
        }
        val mid = Path().apply {
            moveTo(3.5f * s, 12f * s)
            lineTo(12f * s, 16f * s)
            lineTo(20.5f * s, 12f * s)
        }
        val bottom = Path().apply {
            moveTo(3.5f * s, 16.5f * s)
            lineTo(12f * s, 20.5f * s)
            lineTo(20.5f * s, 16.5f * s)
        }
        drawPath(top, color, style = stroke)
        drawPath(mid, color, style = stroke)
        drawPath(bottom, color, style = stroke)
    }
}

@Composable
fun PianoIcon(modifier: Modifier) {
    Canvas(modifier) {
        val s = size.width / 24f
        val ink = C.Track
        drawRoundRect(Color(0xFFEEF4FF), Offset(2.5f * s, 5.5f * s), Size(19f * s, 13f * s), CornerRadius(3f * s))
        drawRoundRect(ink, Offset(2.5f * s, 5.5f * s), Size(19f * s, 13f * s), CornerRadius(3f * s), style = Stroke(1.6f * s))
        listOf(7.25f, 12f, 16.75f).forEach { x ->
            drawLine(ink, Offset(x * s, 13f * s), Offset(x * s, 18.5f * s), strokeWidth = 1f * s)
        }
        listOf(5.75f, 10.5f, 15.25f).forEach { x ->
            drawRoundRect(ink, Offset(x * s, 5.5f * s), Size(3f * s, 7.5f * s), CornerRadius(1f * s))
        }
    }
}
