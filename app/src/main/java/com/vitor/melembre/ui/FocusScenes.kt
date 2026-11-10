package com.vitor.melembre.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Cenas ilustradas desenhadas em código, usadas como fundo (bem escurecido) do modo foco. */
object FocusScenes {
    val focus = listOf("office", "laptop", "night")
    val breaks = listOf("coffee", "phone", "water")
    val rest = listOf("plate", "window")
}

@Composable
fun FocusScene(id: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        when (id) {
            "office" -> office()
            "laptop" -> laptop()
            "night" -> night()
            "coffee" -> coffee()
            "phone" -> phone()
            "water" -> water()
            "plate" -> plate()
            "window" -> windowScene()
            else -> Unit
        }
    }
}

private fun DrawScope.box(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 0f) {
    drawRoundRect(c, Offset(size.width * x, size.height * y), Size(size.width * w, size.height * h), CornerRadius(r))
}

private fun DrawScope.circle(c: Color, x: Float, y: Float, r: Float) {
    drawCircle(c, size.height * r, Offset(size.width * x, size.height * y))
}

private fun DrawScope.office() {
    drawRect(Color(0xFF1A2030))
    box(Color(0xFF141A28), 0f, 0.74f, 1f, 0.26f)
    box(Color(0xFF2B3A5A), 0.52f, 0.22f, 0.3f, 0.4f, 12f)
    box(Color(0xFF4F6FA8), 0.535f, 0.26f, 0.27f, 0.32f, 6f)
    box(Color(0xFF2B3A5A), 0.645f, 0.62f, 0.05f, 0.1f)
    box(Color(0xFF34405E), 0.47f, 0.72f, 0.4f, 0.04f, 8f)
    circle(Color(0xFF10151F), 0.4f, 0.48f, 0.09f)
    val body = Path().apply {
        moveTo(size.width * 0.33f, size.height * 0.76f)
        cubicTo(size.width * 0.34f, size.height * 0.55f, size.width * 0.46f, size.height * 0.55f, size.width * 0.47f, size.height * 0.76f)
        close()
    }
    drawPath(body, Color(0xFF10151F))
}

private fun DrawScope.laptop() {
    drawRect(Color(0xFF1B1F2B))
    box(Color(0xFF12151E), 0f, 0.7f, 1f, 0.3f)
    box(Color(0xFF2E3550), 0.4f, 0.3f, 0.34f, 0.32f, 8f)
    box(Color(0xFF55608C), 0.415f, 0.34f, 0.31f, 0.24f, 4f)
    box(Color(0xFF3A4163), 0.36f, 0.62f, 0.42f, 0.05f, 6f)
    box(Color(0xFF3A3F55), 0.18f, 0.5f, 0.03f, 0.2f)
    drawPath(
        Path().apply {
            moveTo(size.width * 0.13f, size.height * 0.5f)
            lineTo(size.width * 0.26f, size.height * 0.5f)
            lineTo(size.width * 0.22f, size.height * 0.38f)
            lineTo(size.width * 0.17f, size.height * 0.38f)
            close()
        },
        Color(0xFF6A5E3A),
    )
}

private fun DrawScope.night() {
    drawRect(Color(0xFF111827))
    for (i in 0 until 9) {
        val h = 0.22f + (i * 37 % 7) * 0.05f
        box(Color(0xFF1C2740), 0.46f + i * 0.055f, 0.7f - h, 0.045f, h)
    }
    circle(Color(0xFFCBC8A8), 0.8f, 0.2f, 0.07f)
    box(Color(0xFF0C111C), 0f, 0.7f, 1f, 0.3f)
    box(Color(0xFF263048), 0.1f, 0.58f, 0.3f, 0.06f, 6f)
    box(Color(0xFF3B4568), 0.14f, 0.36f, 0.2f, 0.22f, 6f)
}

private fun DrawScope.coffee() {
    drawRect(Color(0xFF2A1F1A))
    box(Color(0xFF1D1511), 0f, 0.72f, 1f, 0.28f)
    box(Color(0xFFD9C3A5), 0.58f, 0.48f, 0.14f, 0.24f, 14f)
    drawArc(
        Color(0xFFD9C3A5), -90f, 180f, false,
        Offset(size.width * 0.69f, size.height * 0.52f), Size(size.width * 0.06f, size.height * 0.16f),
        style = Stroke(size.height * 0.03f),
    )
    box(Color(0xFF6B4630), 0.595f, 0.5f, 0.11f, 0.04f, 8f)
    box(Color(0xFFD9C3A5), 0.55f, 0.72f, 0.2f, 0.03f, 6f)
    for (dx in listOf(0.62f, 0.67f)) {
        val p = Path().apply {
            moveTo(size.width * dx, size.height * 0.42f)
            cubicTo(size.width * (dx - 0.02f), size.height * 0.34f, size.width * (dx + 0.02f), size.height * 0.3f, size.width * dx, size.height * 0.22f)
        }
        drawPath(p, Color(0xFF9C8B78), style = Stroke(size.height * 0.016f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.phone() {
    drawRect(Color(0xFF1E1B2B))
    box(Color(0xFF2A2640), 0.5f, 0.1f, 0.2f, 0.8f, 18f)
    box(Color(0xFF3C3560), 0.515f, 0.14f, 0.17f, 0.72f, 12f)
    circle(Color(0xFF8A5A9C), 0.6f, 0.3f, 0.07f)
    circle(Color(0xFF5A7A9C), 0.6f, 0.52f, 0.07f)
    circle(Color(0xFF9C7A5A), 0.6f, 0.74f, 0.07f)
    box(Color(0xFF241F38), 0.1f, 0.74f, 0.3f, 0.05f, 8f)
}

private fun DrawScope.water() {
    drawRect(Color(0xFF14232B))
    box(Color(0xFF0E1A20), 0f, 0.74f, 1f, 0.26f)
    drawPath(
        Path().apply {
            moveTo(size.width * 0.56f, size.height * 0.3f)
            lineTo(size.width * 0.72f, size.height * 0.3f)
            lineTo(size.width * 0.7f, size.height * 0.74f)
            lineTo(size.width * 0.58f, size.height * 0.74f)
            close()
        },
        Color(0xFF28465A),
    )
    drawPath(
        Path().apply {
            moveTo(size.width * 0.563f, size.height * 0.44f)
            lineTo(size.width * 0.717f, size.height * 0.44f)
            lineTo(size.width * 0.699f, size.height * 0.73f)
            lineTo(size.width * 0.581f, size.height * 0.73f)
            close()
        },
        Color(0xFF4A86A8),
    )
    circle(Color(0xFF4A86A8), 0.8f, 0.38f, 0.035f)
    circle(Color(0xFF4A86A8), 0.84f, 0.55f, 0.025f)
}

private fun DrawScope.plate() {
    drawRect(Color(0xFF1F2A24))
    box(Color(0xFF17201B), 0f, 0.7f, 1f, 0.3f)
    drawOval(Color(0xFFCFD4CC), Offset(size.width * 0.5f, size.height * 0.5f), Size(size.width * 0.3f, size.height * 0.34f))
    drawOval(Color(0xFF8A9A78), Offset(size.width * 0.55f, size.height * 0.56f), Size(size.width * 0.2f, size.height * 0.22f))
    box(Color(0xFFB8BCB4), 0.44f, 0.46f, 0.012f, 0.36f, 4f)
    box(Color(0xFFB8BCB4), 0.84f, 0.46f, 0.012f, 0.36f, 4f)
}

private fun DrawScope.windowScene() {
    drawRect(Color(0xFF1B241F))
    box(Color(0xFF2F4A5C), 0.5f, 0.12f, 0.32f, 0.62f, 8f)
    circle(Color(0xFFE3C77A), 0.72f, 0.3f, 0.07f)
    drawPath(
        Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.74f)
            lineTo(size.width * 0.58f, size.height * 0.5f)
            lineTo(size.width * 0.64f, size.height * 0.64f)
            lineTo(size.width * 0.7f, size.height * 0.44f)
            lineTo(size.width * 0.82f, size.height * 0.74f)
            close()
        },
        Color(0xFF26402F),
    )
    drawRoundRect(
        Color(0xFF3A4A42), Offset(size.width * 0.49f, size.height * 0.1f),
        Size(size.width * 0.34f, size.height * 0.66f), CornerRadius(12f), style = Stroke(size.height * 0.025f),
    )
}
