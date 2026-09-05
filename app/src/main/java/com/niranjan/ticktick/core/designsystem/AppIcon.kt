package com.niranjan.ticktick.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppSymbol {
    Menu, More, Suggestion, Plus, Search, Bell, Settings, Inbox, Calendar,
    Tasks, Focus, Habits, Work, Home, List, Manage, AddList, Check, Close, Crown,
}

/** Small local vector set. Replace individual glyphs when original icon assets are available. */
@Composable
fun AppIcon(
    symbol: AppSymbol,
    modifier: Modifier = Modifier,
    tint: Color = TickTickColors.Text,
    calendarDay: String = "5",
) {
    Box(modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                val stroke = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = tint, width: Float = 1.6f) {
                    drawLine(color, Offset(x1, y1), Offset(x2, y2), width, StrokeCap.Round)
                }
                fun path(vararg points: Pair<Float, Float>, close: Boolean = false): Path = Path().apply {
                    points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.first, point.second) else lineTo(point.first, point.second)
                    }
                    if (close) close()
                }
                when (symbol) {
                    AppSymbol.Menu -> {
                        line(3f, 4.5f, 21f, 4.5f)
                        line(3f, 12f, 21f, 12f)
                        line(3f, 19.5f, 21f, 19.5f)
                    }
                    AppSymbol.More -> listOf(5f, 12f, 19f).forEach { drawCircle(tint, 1.3f, Offset(12f, it)) }
                    AppSymbol.Plus, AppSymbol.AddList -> {
                        if (symbol == AppSymbol.AddList) drawRoundRect(tint, Offset(3f, 3f), Size(18f, 18f), CornerRadius(4f), style = stroke)
                        val width = if (symbol == AppSymbol.Plus) 1.1f else 1.6f
                        line(12f, 7f, 12f, 17f, width = width)
                        line(7f, 12f, 17f, 12f, width = width)
                    }
                    AppSymbol.Search -> {
                        drawCircle(tint, 7.3f, Offset(10.2f, 10.2f), style = stroke)
                        line(15.8f, 15.8f, 21f, 21f)
                    }
                    AppSymbol.Bell -> {
                        val bell = Path().apply {
                            moveTo(5f, 17f); lineTo(6.5f, 15f); lineTo(6.5f, 9f)
                            cubicTo(6.5f, 1.5f, 17.5f, 1.5f, 17.5f, 9f)
                            lineTo(17.5f, 15f); lineTo(19f, 17f); close()
                        }
                        drawPath(bell, tint, style = stroke)
                        line(10f, 20f, 14f, 20f)
                        line(12f, 2f, 12f, 3f)
                    }
                    AppSymbol.Settings, AppSymbol.Habits -> {
                        val hex = path(12f to 1.5f, 20.8f to 6.5f, 20.8f to 17.5f, 12f to 22.5f, 3.2f to 17.5f, 3.2f to 6.5f, close = true)
                        if (symbol == AppSymbol.Habits) {
                            drawPath(hex, tint)
                            drawCircle(Color.White, 3.4f, Offset(12f, 12f))
                        } else {
                            drawPath(hex, tint, style = stroke)
                            drawCircle(tint, 3.2f, Offset(12f, 12f), style = stroke)
                        }
                    }
                    AppSymbol.Tasks -> {
                        drawRoundRect(tint, Offset(1f, 1f), Size(22f, 22f), CornerRadius(5f))
                        drawPath(path(6f to 12.5f, 10.3f to 17f, 17.8f to 6.5f), Color.White, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    AppSymbol.Calendar -> {
                        drawRoundRect(tint, Offset(1f, 3f), Size(22f, 21f), CornerRadius(5f))
                        line(6f, 1f, 6f, 6f, width = 3f)
                        line(18f, 1f, 18f, 6f, width = 3f)
                    }
                    AppSymbol.Focus -> {
                        val pin = Path().apply {
                            moveTo(12f, 23f)
                            cubicTo(9f, 22f, 1f, 14f, 1f, 10f)
                            cubicTo(1f, -3f, 23f, -3f, 23f, 10f)
                            cubicTo(23f, 14f, 15f, 22f, 12f, 23f)
                            close()
                        }
                        drawPath(pin, tint)
                        line(12f, 5f, 12f, 11f, Color.White, 1.9f)
                        line(12f, 11f, 15f, 11f, Color.White, 1.9f)
                    }
                    AppSymbol.Inbox -> {
                        drawRoundRect(tint, Offset(2f, 3f), Size(20f, 19f), CornerRadius(4f))
                        drawPath(path(5f to 6f, 19f to 6f, 19f to 12f, 15f to 12f, 14f to 16f, 10f to 16f, 9f to 12f, 5f to 12f, close = true), Color.White)
                    }
                    AppSymbol.Work -> {
                        drawRoundRect(tint, Offset(8f, 3f), Size(8f, 5f), CornerRadius(2f), style = stroke)
                        drawRoundRect(tint, Offset(2f, 6f), Size(20f, 16f), CornerRadius(2f))
                        line(3f, 13f, 21f, 13f, Color.White.copy(alpha = .45f), .8f)
                        line(12f, 12f, 12f, 15f, Color.White, 1.2f)
                    }
                    AppSymbol.Home -> {
                        drawPath(path(2f to 11f, 12f to 2f, 22f to 11f), tint, style = stroke)
                        drawPath(path(5f to 10f, 5f to 22f, 19f to 22f, 19f to 10f), tint, style = stroke)
                        drawPath(path(10f to 22f, 10f to 15f, 14f to 15f, 14f to 22f), tint, style = stroke)
                    }
                    AppSymbol.List -> listOf(6f, 12f, 18f).forEach {
                        drawCircle(tint, 1.2f, Offset(4f, it)); line(9f, it, 21f, it)
                    }
                    AppSymbol.Manage -> {
                        line(3f, 5f, 21f, 5f); line(3f, 12f, 10f, 12f); line(3f, 19f, 9f, 19f)
                        drawCircle(tint, 3.3f, Offset(17f, 16f), style = stroke)
                        listOf(0f, 90f, 180f, 270f).forEach { degrees ->
                            val angle = Math.toRadians(degrees.toDouble())
                            val dx = kotlin.math.cos(angle).toFloat(); val dy = kotlin.math.sin(angle).toFloat()
                            line(17f + dx * 4f, 16f + dy * 4f, 17f + dx * 5f, 16f + dy * 5f)
                        }
                    }
                    AppSymbol.Check -> drawPath(path(4f to 12f, 9f to 17f, 20f to 5f), tint, style = stroke)
                    AppSymbol.Close -> { line(6f, 6f, 18f, 18f); line(6f, 18f, 18f, 6f) }
                    AppSymbol.Crown -> {
                        drawPath(path(4f to 7f, 8f to 11f, 12f to 5f, 16f to 11f, 20f to 7f, 18f to 18f, 6f to 18f, close = true), tint)
                    }
                    AppSymbol.Suggestion -> {
                        val bulb = Path().apply {
                            moveTo(9f, 18f); lineTo(9f, 16f)
                            cubicTo(9f, 14f, 6f, 13f, 6f, 9f)
                            cubicTo(6f, 1f, 18f, 1f, 18f, 9f)
                            cubicTo(18f, 13f, 15f, 14f, 15f, 16f)
                            lineTo(15f, 18f); close()
                        }
                        drawPath(bulb, tint, style = stroke)
                        line(10f, 21f, 14f, 21f)
                        line(12f, 0f, 12f, 1f); line(2f, 9f, 3f, 9f); line(21f, 9f, 22f, 9f)
                        line(4f, 2f, 5f, 3f); line(20f, 2f, 21f, 1f)
                        line(21f, 0f, 21f, 4f); line(19f, 2f, 23f, 2f)
                    }
                }
            }
        }
        if (symbol == AppSymbol.Calendar) {
            Text(calendarDay, Modifier.offset(y = 3.dp), color = Color.White, fontSize = 13.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
