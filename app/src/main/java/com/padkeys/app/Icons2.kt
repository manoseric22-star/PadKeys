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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

// Maliliit na icon na iginuhit (24x24 na grid) para sa mga popup.

private fun poly(k: Float, close: Boolean, vararg p: Float): Path {
    val path = Path()
    path.moveTo(p[0] * k, p[1] * k)
    var i = 2
    while (i < p.size) {
        path.lineTo(p[i] * k, p[i + 1] * k)
        i += 2
    }
    if (close) path.close()
    return path
}

@Composable
private fun Ico(
    modifier: Modifier,
    strokeW: Float = 2f,
    draw: DrawScope.(k: Float, st: Stroke) -> Unit,
) {
    Canvas(modifier) {
        val k = size.minDimension / 24f
        draw(k, Stroke(strokeW * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CloseIcon(modifier: Modifier, color: Color) = Ico(modifier, 2.2f) { k, st ->
    drawLine(color, Offset(6 * k, 6 * k), Offset(18 * k, 18 * k), st.width, StrokeCap.Round)
    drawLine(color, Offset(18 * k, 6 * k), Offset(6 * k, 18 * k), st.width, StrokeCap.Round)
}

@Composable
fun MinusIcon(modifier: Modifier, color: Color) = Ico(modifier, 2.2f) { k, st ->
    drawLine(color, Offset(5 * k, 12 * k), Offset(19 * k, 12 * k), st.width, StrokeCap.Round)
}

@Composable
fun CheckIcon(modifier: Modifier, color: Color) = Ico(modifier, 2.6f) { k, st ->
    drawPath(poly(k, false, 5f, 12.5f, 9.5f, 17f, 19f, 7.5f), color, style = st)
}

@Composable
fun ChevronIcon(modifier: Modifier, color: Color) = Ico(modifier, 2f) { k, st ->
    drawPath(poly(k, false, 9f, 5f, 16f, 12f, 9f, 19f), color, style = st)
}

@Composable
fun PlayIcon(modifier: Modifier, color: Color) = Ico(modifier) { k, _ ->
    drawPath(poly(k, true, 8f, 5f, 19f, 12f, 8f, 19f), color, style = Fill)
}

@Composable
fun PauseIcon(modifier: Modifier, color: Color) = Ico(modifier) { k, _ ->
    drawRect(color, Offset(7 * k, 5 * k), Size(4 * k, 14 * k))
    drawRect(color, Offset(13 * k, 5 * k), Size(4 * k, 14 * k))
}

@Composable
fun StarIcon(modifier: Modifier, color: Color, filled: Boolean) = Ico(modifier, 2f) { k, st ->
    val p = poly(
        k, true,
        12f, 3.5f, 14.7f, 9.1f, 20.8f, 10f, 16.4f, 14.3f, 17.4f, 20.4f,
        12f, 17.5f, 6.6f, 20.4f, 7.6f, 14.3f, 3.2f, 10f, 9.3f, 9.1f,
    )
    if (filled) drawPath(p, color, style = Fill)
    drawPath(p, color, style = st)
}

@Composable
fun TrashIcon(modifier: Modifier, color: Color) = Ico(modifier, 2f) { k, st ->
    drawLine(color, Offset(4 * k, 7 * k), Offset(20 * k, 7 * k), st.width, StrokeCap.Round)
    drawPath(poly(k, false, 9f, 7f, 9f, 4f, 15f, 4f, 15f, 7f), color, style = st)
    drawPath(poly(k, false, 6f, 7f, 7f, 20f, 17f, 20f, 18f, 7f), color, style = st)
    drawLine(color, Offset(10 * k, 11 * k), Offset(10 * k, 17 * k), st.width, StrokeCap.Round)
    drawLine(color, Offset(14 * k, 11 * k), Offset(14 * k, 17 * k), st.width, StrokeCap.Round)
}

@Composable
fun SearchIcon(modifier: Modifier, color: Color) = Ico(modifier, 2f) { k, st ->
    drawCircle(color, radius = 6.5f * k, center = Offset(11 * k, 11 * k), style = st)
    drawLine(color, Offset(16 * k, 16 * k), Offset(20.5f * k, 20.5f * k), st.width, StrokeCap.Round)
}

@Composable
fun ImportIcon(modifier: Modifier, color: Color) = Ico(modifier, 2.2f) { k, st ->
    drawLine(color, Offset(12 * k, 4 * k), Offset(12 * k, 15 * k), st.width, StrokeCap.Round)
    drawPath(poly(k, false, 7.5f, 10.5f, 12f, 15f, 16.5f, 10.5f), color, style = st)
    drawLine(color, Offset(5 * k, 19 * k), Offset(19 * k, 19 * k), st.width, StrokeCap.Round)
}

@Composable
fun PencilIcon(modifier: Modifier, color: Color) = Ico(modifier, 2f) { k, st ->
    drawPath(poly(k, true, 4f, 20f, 8f, 20f, 19f, 9f, 15f, 5f, 4f, 16f), color, style = st)
    drawLine(color, Offset(13.5f * k, 6.5f * k), Offset(17.5f * k, 10.5f * k), st.width, StrokeCap.Round)
}

@Composable
fun FileIcon(modifier: Modifier, color: Color) = Ico(modifier, 2f) { k, st ->
    drawPath(poly(k, true, 6f, 3.5f, 14f, 3.5f, 18f, 7.5f, 18f, 20.5f, 6f, 20.5f), color, style = st)
    drawPath(poly(k, false, 14f, 3.5f, 14f, 7.5f, 18f, 7.5f), color, style = st)
}

@Composable
fun SoundfontIcon(modifier: Modifier, color: Color) = Ico(modifier, 2f) { k, st ->
    drawRoundRect(color, Offset(3 * k, 5 * k), Size(18 * k, 14 * k), CornerRadius(2.5f * k), style = st)
    drawLine(color, Offset(8.5f * k, 5 * k), Offset(8.5f * k, 19 * k), st.width)
    drawLine(color, Offset(15.5f * k, 5 * k), Offset(15.5f * k, 19 * k), st.width)
}

@Composable
fun WaveIcon(modifier: Modifier, color: Color) = Ico(modifier, 2f) { k, st ->
    drawLine(color, Offset(3 * k, 12 * k), Offset(5 * k, 12 * k), st.width, StrokeCap.Round)
    drawLine(color, Offset(7.5f * k, 8 * k), Offset(7.5f * k, 16 * k), st.width, StrokeCap.Round)
    drawLine(color, Offset(12 * k, 4.5f * k), Offset(12 * k, 19.5f * k), st.width, StrokeCap.Round)
    drawLine(color, Offset(16.5f * k, 8.5f * k), Offset(16.5f * k, 15.5f * k), st.width, StrokeCap.Round)
    drawLine(color, Offset(21 * k, 11 * k), Offset(21 * k, 13 * k), st.width, StrokeCap.Round)
}
