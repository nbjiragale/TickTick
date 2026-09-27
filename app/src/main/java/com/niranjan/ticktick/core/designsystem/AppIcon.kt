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
    Menu, More, Suggestion, Plus, Search, Bell, Settings, Inbox, InboxOutline, Calendar,
    Tasks, Focus, Habits, Work, Home, List, Manage, AddList, Check, Close, Crown,
    Date, Flag, Tag, Move, MoreHorizontal, Microphone, Send, Image, Template,
    Note, Expand, Back, Chevrons, Alarm, Attachment, Clock, Repeat, Keyboard, Chevron,
    Camera, Folder, Scan, Trash, PlanDone, PlanToday, PlanLater, PlanDecline,
    Snooze, Sunrise, Moon, Pencil, HabitStats, HabitBook, HabitSettings, Reorder, Share, Archive,
    Kanban, Background, Sort, Filter, SectionMove, Pin, Duplicate, Undo, DateClear,
    Sun, FocusStats, Music, Pause, Play, Stop, TimerRing,
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
                    AppSymbol.Filter -> {
                        line(2f, 4f, 22f, 4f); line(2f, 10f, 12f, 10f); line(2f, 16f, 9f, 16f)
                        drawPath(path(13f to 9f, 22f to 9f, 18.5f to 14f, 18.5f to 21f, 16f to 20f, 16f to 14f, 13f to 9f), tint, style = stroke)
                    }
                    AppSymbol.Kanban -> {
                        listOf(3f to 14f, 10f to 19f, 17f to 11f).forEach { (x, height) ->
                            drawRoundRect(tint, Offset(x, 3f), Size(4f, height), CornerRadius(1f), style = stroke)
                        }
                    }
                    AppSymbol.Background -> {
                        drawRoundRect(tint, Offset(3f, 2f), Size(17f, 8f), CornerRadius(2f), style = stroke)
                        drawPath(path(21f to 6f, 23f to 6f, 23f to 13f, 12f to 13f, 12f to 17f), tint, style = stroke)
                        drawRoundRect(tint, Offset(10f, 17f), Size(4f, 6f), CornerRadius(1f), style = stroke)
                    }
                    AppSymbol.Sort -> {
                        line(6f, 3f, 6f, 22f); line(2f, 7f, 6f, 3f); line(6f, 3f, 10f, 7f)
                        line(18f, 2f, 18f, 21f); line(14f, 17f, 18f, 21f); line(18f, 21f, 22f, 17f)
                    }
                    AppSymbol.Snooze -> {
                        drawArc(tint, 35f, 300f, false, Offset(3f, 3f), Size(18f, 18f), style = stroke)
                        drawPath(path(17f to 9f, 21f to 12f, 24f to 8f), tint, style = stroke)
                        line(12f, 6f, 12f, 12f); line(12f, 12f, 16f, 12f)
                    }
                    AppSymbol.Sunrise -> {
                        drawArc(tint, 180f, 180f, false, Offset(5f, 10f), Size(14f, 14f), style = stroke)
                        line(2f, 21f, 22f, 21f); line(12f, 2f, 12f, 8f)
                        drawPath(path(8f to 6f, 12f to 2f, 16f to 6f), tint, style = stroke)
                        line(3f, 9f, 5f, 11f); line(19f, 11f, 21f, 9f)
                        line(1f, 16f, 3f, 16f); line(21f, 16f, 23f, 16f)
                    }
                    AppSymbol.Moon -> {
                        drawPath(Path().apply {
                            moveTo(11f, 2f); cubicTo(0f, 2f, 0f, 22f, 12f, 22f)
                            cubicTo(18f, 22f, 22f, 18f, 22f, 13f)
                            cubicTo(10f, 18f, 6f, 11f, 11f, 2f)
                        }, tint, style = stroke)
                        line(19f, 1f, 19f, 7f); line(16f, 4f, 22f, 4f)
                    }
                    AppSymbol.Pencil -> {
                        drawPath(path(6f to 16f, 15f to 2f, 21f to 6f, 12f to 20f, 5f to 21f, 6f to 16f), tint, style = stroke)
                        line(13f, 5f, 19f, 9f); line(6f, 16f, 12f, 20f); line(2f, 24f, 23f, 24f)
                    }
                    AppSymbol.PlanDone, AppSymbol.PlanDecline, AppSymbol.PlanToday -> {
                        drawRoundRect(tint, Offset(3f, 3f), Size(18f, 18f), CornerRadius(4f), style = stroke)
                        if (symbol == AppSymbol.PlanDecline) {
                            line(8f, 8f, 16f, 16f); line(16f, 8f, 8f, 16f)
                        }
                        if (symbol == AppSymbol.PlanToday) {
                            line(8f, 1f, 8f, 5f); line(16f, 1f, 16f, 5f)
                        }
                    }
                    AppSymbol.PlanLater -> {
                        drawRoundRect(tint, Offset(3f, 6f), Size(15f, 15f), CornerRadius(2f), style = stroke)
                        drawArc(tint, -90f, 180f, false, Offset(15f, 9f), Size(8f, 9f), style = stroke)
                        line(2f, 24f, 21f, 24f); line(7f, 1f, 7f, 2f); line(13f, 2f, 13f, 3f)
                    }
                    AppSymbol.Camera -> {
                        drawRoundRect(tint, Offset(3f, 6f), Size(18f, 16f), CornerRadius(3f), style = stroke)
                        line(8f, 2f, 16f, 2f)
                        drawCircle(tint, 4f, Offset(12f, 14f), style = stroke)
                        drawCircle(tint, .9f, Offset(17f, 9f))
                    }
                    AppSymbol.Folder -> {
                        drawPath(Path().apply {
                            moveTo(3f, 6f); quadraticTo(3f, 3f, 6f, 3f); lineTo(10f, 3f)
                            lineTo(13f, 6f); lineTo(19f, 6f); quadraticTo(22f, 6f, 22f, 9f)
                            lineTo(22f, 19f); quadraticTo(22f, 21f, 19f, 21f); lineTo(6f, 21f)
                            quadraticTo(3f, 21f, 3f, 18f); close()
                        }, tint, style = stroke)
                        line(3f, 10f, 22f, 10f)
                    }
                    AppSymbol.Scan -> {
                        drawRoundRect(tint, Offset(7f, 4f), Size(10f, 16f), CornerRadius(1.5f), style = stroke)
                        drawPath(path(6f to 2f, 2f to 2f, 2f to 6f), tint, style = stroke)
                        drawPath(path(18f to 2f, 22f to 2f, 22f to 6f), tint, style = stroke)
                        drawPath(path(2f to 18f, 2f to 22f, 6f to 22f), tint, style = stroke)
                        drawPath(path(22f to 18f, 22f to 22f, 18f to 22f), tint, style = stroke)
                        listOf(8f, 11f, 14f, 17f).forEach { line(10f, it, 14f, it, width = 1f) }
                    }
                    AppSymbol.Trash -> {
                        line(3f, 6f, 21f, 6f); line(9f, 2f, 15f, 2f)
                        drawPath(path(5f to 6f, 6f to 22f, 18f to 22f, 19f to 6f), tint, style = stroke)
                        line(10f, 10f, 10f, 18f); line(14f, 10f, 14f, 18f)
                    }
                    AppSymbol.Clock, AppSymbol.Focus -> {
                        drawCircle(tint, 9f, Offset(12f, 12f), style = stroke)
                        line(12f, 6f, 12f, 12f); line(12f, 12f, 16f, 12f)
                    }
                    AppSymbol.Chevron -> drawPath(path(9f to 5f, 15f to 12f, 9f to 19f), tint, style = stroke)
                    AppSymbol.Repeat -> {
                        drawArc(tint, 200f, 150f, false, Offset(4f, 4f), Size(16f, 16f), style = stroke)
                        drawArc(tint, 20f, 150f, false, Offset(4f, 4f), Size(16f, 16f), style = stroke)
                        drawPath(path(15f to 2f, 19f to 5f, 16f to 9f), tint, style = stroke)
                        drawPath(path(9f to 22f, 5f to 19f, 8f to 15f), tint, style = stroke)
                    }
                    AppSymbol.Keyboard -> {
                        drawRoundRect(tint, Offset(2f, 5f), Size(20f, 14f), CornerRadius(2f), style = stroke)
                        listOf(8f, 11f).forEach { y -> listOf(6f, 10f, 14f, 18f).forEach { x -> drawCircle(tint, .65f, Offset(x, y)) } }
                        line(7f, 15f, 17f, 15f, width = 1f)
                    }
                    AppSymbol.Back -> {
                        line(20f, 12f, 3f, 12f)
                        drawPath(path(11f to 4f, 3f to 12f, 11f to 20f), tint, style = stroke)
                    }
                    AppSymbol.Chevrons -> {
                        drawPath(path(8f to 9f, 12f to 5f, 16f to 9f), tint, style = stroke)
                        drawPath(path(8f to 15f, 12f to 19f, 16f to 15f), tint, style = stroke)
                    }
                    AppSymbol.Date -> {
                        drawRoundRect(tint, Offset(2f, 3f), Size(20f, 19f), CornerRadius(4f), style = stroke)
                        line(3f, 8f, 21f, 8f)
                        for (x in listOf(7f, 12f, 17f)) for (y in listOf(12f, 16f)) drawCircle(tint, .8f, Offset(x, y))
                    }
                    AppSymbol.Flag -> {
                        line(3f, 2f, 3f, 23f)
                        drawPath(path(3f to 3f, 21f to 3f, 17f to 9f, 21f to 15f, 3f to 15f), tint, style = stroke)
                    }
                    AppSymbol.Tag -> {
                        drawPath(path(3f to 2f, 11f to 2f, 22f to 13f, 22f to 16f, 16f to 22f, 13f to 22f, 2f to 11f, 2f to 3f, close = true), tint, style = stroke)
                        drawCircle(tint, 1.3f, Offset(7f, 7f))
                    }
                    AppSymbol.Move -> {
                        drawRoundRect(tint, Offset(2f, 6f), Size(20f, 16f), CornerRadius(4f), style = stroke)
                        line(5f, 2f, 19f, 2f)
                        line(7f, 14f, 17f, 14f)
                        drawPath(path(13f to 10f, 17f to 14f, 13f to 18f), tint, style = stroke)
                    }
                    AppSymbol.MoreHorizontal -> listOf(4f, 12f, 20f).forEach { drawCircle(tint, 1.7f, Offset(it, 12f)) }
                    AppSymbol.Microphone -> {
                        drawRoundRect(tint, Offset(8f, 1f), Size(8f, 15f), CornerRadius(4f), style = stroke)
                        drawPath(Path().apply { moveTo(3f, 11f); cubicTo(3f, 24f, 21f, 24f, 21f, 11f) }, tint, style = stroke)
                        line(12f, 21f, 12f, 24f)
                    }
                    AppSymbol.Send -> drawPath(path(2f to 3f, 23f to 12f, 2f to 21f, 5f to 14f, 15f to 12f, 5f to 10f, close = true), tint)
                    AppSymbol.Image -> {
                        drawRoundRect(tint, Offset(2f, 3f), Size(20f, 18f), CornerRadius(4f), style = stroke)
                        drawCircle(tint, 1.7f, Offset(8f, 8f))
                        drawPath(path(3f to 17f, 9f to 12f, 13f to 15f, 18f to 10f, 21f to 12f), tint, style = stroke)
                    }
                    AppSymbol.Template, AppSymbol.Note -> {
                        drawPath(Path().apply { moveTo(5f, 2f); lineTo(15f, 2f); lineTo(21f, 8f); lineTo(21f, 20f); quadraticTo(21f, 22f, 18f, 22f); lineTo(5f, 22f); quadraticTo(2f, 22f, 2f, 19f); lineTo(2f, 5f); quadraticTo(2f, 2f, 5f, 2f) }, tint, style = stroke)
                        if (symbol == AppSymbol.Template) { line(7f, 9f, 16f, 9f); line(11.5f, 9f, 11.5f, 18f) }
                        else drawPath(path(7f to 3f, 7f to 12f, 11f to 9f, 15f to 12f, 15f to 3f), tint, style = stroke)
                    }
                    AppSymbol.Expand -> {
                        drawPath(path(14f to 3f, 21f to 3f, 21f to 10f), tint, style = stroke)
                        line(21f, 3f, 14f, 10f)
                        drawPath(path(3f to 14f, 3f to 21f, 10f to 21f), tint, style = stroke)
                        line(3f, 21f, 10f, 14f)
                    }
                    AppSymbol.Alarm -> {
                        drawCircle(tint, 7.5f, Offset(12f, 13f), style = stroke)
                        drawPath(path(12f to 8f, 12f to 13f, 16f to 13f), tint, style = stroke)
                        drawPath(path(2f to 7f, 3f to 3f, 7f to 2f), tint, style = stroke)
                        drawPath(path(17f to 2f, 21f to 3f, 22f to 7f), tint, style = stroke)
                        line(6f, 20f, 4f, 23f); line(18f, 20f, 20f, 23f)
                    }
                    AppSymbol.Attachment -> {
                        drawPath(Path().apply {
                            moveTo(16f, 6f); lineTo(7f, 15f); cubicTo(4f, 18f, 8f, 21f, 11f, 18f)
                            lineTo(20f, 9f); cubicTo(25f, 4f, 18f, -1f, 14f, 3f)
                            lineTo(3f, 14f); cubicTo(-3f, 21f, 6f, 29f, 13f, 22f); lineTo(22f, 13f)
                        }, tint, style = stroke)
                    }
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
                    AppSymbol.Settings -> {
                        val hex = path(12f to 1.5f, 20.8f to 6.5f, 20.8f to 17.5f, 12f to 22.5f, 3.2f to 17.5f, 3.2f to 6.5f, close = true)
                        drawPath(hex, tint, style = stroke)
                        drawCircle(tint, 3.2f, Offset(12f, 12f), style = stroke)
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
                    AppSymbol.Habits -> {
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
                    AppSymbol.InboxOutline -> {
                        drawPath(Path().apply {
                            moveTo(3f, 11f); lineTo(7f, 4f); lineTo(17f, 4f); lineTo(21f, 11f)
                            lineTo(22f, 18f); quadraticTo(22f, 20f, 20f, 20f)
                            lineTo(4f, 20f); quadraticTo(2f, 20f, 2f, 18f); close()
                        }, tint, style = stroke)
                        drawPath(path(3f to 11f, 8f to 11f, 10f to 15f, 14f to 15f, 16f to 11f, 21f to 11f), tint, style = stroke)
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
                    AppSymbol.Share -> {
                        drawPath(path(10f to 4f, 5f to 4f, 3f to 6f, 3f to 20f, 5f to 22f, 19f to 22f, 21f to 20f, 21f to 14f), tint, style = stroke)
                        line(11f, 13f, 22f, 2f)
                        drawPath(path(15f to 2f, 22f to 2f, 22f to 9f), tint, style = stroke)
                    }
                    AppSymbol.Archive -> {
                        drawPath(path(5f to 8f, 2f to 15f, 2f to 21f, 22f to 21f, 22f to 15f, 19f to 8f), tint, style = stroke)
                        drawPath(path(3f to 15f, 8f to 15f, 10f to 18f, 14f to 18f, 16f to 15f, 21f to 15f), tint, style = stroke)
                        line(12f, 2f, 12f, 13f)
                        drawPath(path(7f to 8f, 12f to 13f, 17f to 8f), tint, style = stroke)
                    }
                    AppSymbol.Reorder -> {
                        line(4f, 6f, 20f, 6f, width = 1f)
                        line(4f, 12f, 20f, 12f, width = 1f)
                        line(4f, 18f, 20f, 18f, width = 1f)
                        line(12f, 4f, 12f, 6f, width = 1f)
                        line(12f, 18f, 12f, 20f, width = 1f)
                    }
                    AppSymbol.HabitStats -> {
                        drawCircle(tint, 9f, Offset(12f, 12f), style = stroke)
                        drawPath(path(3f to 12f, 12f to 12f, 12f to 3f), tint, style = stroke)
                        line(15f, 4f, 15f, 10f)
                    }
                    AppSymbol.HabitBook -> {
                        drawRoundRect(tint, Offset(4f, 3f), Size(16f, 19f), CornerRadius(4f), style = stroke)
                        line(8f, 4f, 8f, 21f); line(11f, 6f, 14f, 6f)
                    }
                    AppSymbol.HabitSettings -> {
                        listOf(5f, 12f, 19f).forEachIndexed { index, y ->
                            line(3f, y, 21f, y)
                            val x = if (index == 1) 15f else 7f
                            drawCircle(Color.White, 3f, Offset(x, y))
                            drawCircle(tint, 2f, Offset(x, y))
                        }
                    }
                    AppSymbol.Sun -> {
                        drawCircle(tint, 5f, Offset(12f, 12f), style = stroke)
                        repeat(8) { index ->
                            val angle = Math.toRadians(index * 45.0)
                            line(12f + (kotlin.math.cos(angle) * 8).toFloat(), 12f + (kotlin.math.sin(angle) * 8).toFloat(),
                                12f + (kotlin.math.cos(angle) * 11).toFloat(), 12f + (kotlin.math.sin(angle) * 11).toFloat())
                        }
                    }
                    AppSymbol.FocusStats -> {
                        drawCircle(tint, 9f, Offset(12f, 12f), style = stroke)
                        drawPath(path(3f to 12f, 12f to 12f, 12f to 3f), tint, style = stroke)
                        line(15f, 4f, 15f, 10f)
                    }
                    AppSymbol.TimerRing -> {
                        drawCircle(tint, 10f, Offset(12f, 12f), style = Stroke(2.4f))
                        drawCircle(tint, 5f, Offset(12f, 12f))
                    }
                    AppSymbol.Pause -> {
                        drawRoundRect(tint, Offset(5f, 3f), Size(5f, 18f), CornerRadius(1.4f))
                        drawRoundRect(tint, Offset(15f, 3f), Size(5f, 18f), CornerRadius(1.4f))
                    }
                    AppSymbol.Play -> drawPath(path(7f to 3f, 21f to 12f, 7f to 21f, close = true), tint)
                    AppSymbol.Stop -> drawRoundRect(tint, Offset(5f, 5f), Size(14f, 14f), CornerRadius(3f))
                    AppSymbol.Music -> {
                        drawPath(path(8f to 18f, 8f to 6f, 20f to 3f, 20f to 15f), tint, style = stroke)
                        line(8f, 10f, 20f, 7f)
                        drawOval(tint, Offset(2f, 16f), Size(6f, 5f))
                        drawOval(tint, Offset(14f, 13f), Size(6f, 5f))
                    }
                    AppSymbol.SectionMove -> {
                        drawPath(path(8f to 3f, 4f to 3f, 4f to 9f), tint, style = stroke)
                        drawPath(path(16f to 3f, 20f to 3f, 20f to 9f), tint, style = stroke)
                        drawPath(path(4f to 15f, 4f to 21f, 8f to 21f), tint, style = stroke)
                        drawPath(path(20f to 15f, 20f to 21f, 16f to 21f), tint, style = stroke)
                        line(7f, 12f, 17f, 12f)
                        drawPath(path(13f to 8f, 17f to 12f, 13f to 16f), tint, style = stroke)
                    }
                    AppSymbol.Pin -> {
                        line(5f, 3f, 19f, 3f)
                        drawPath(path(12f to 6f, 4f to 13f, 9f to 13f, 9f to 21f, 15f to 21f, 15f to 13f, 20f to 13f, 12f to 6f), tint, style = stroke)
                    }
                    AppSymbol.Duplicate -> {
                        drawRoundRect(tint, Offset(3f, 7f), Size(14f, 14f), CornerRadius(4f), style = stroke)
                        drawPath(path(8f to 3f, 17f to 3f, 21f to 7f, 21f to 16f), tint, style = stroke)
                    }
                    AppSymbol.Undo -> {
                        drawPath(path(9f to 4f, 4f to 9f, 9f to 14f), tint, style = stroke)
                        val curve = Path().apply { moveTo(4f, 9f); lineTo(15f, 9f); cubicTo(24f, 9f, 24f, 21f, 14f, 21f); lineTo(8f, 21f) }
                        drawPath(curve, tint, style = stroke)
                    }
                    AppSymbol.DateClear -> {
                        drawRoundRect(tint, Offset(3f, 3f), Size(18f, 18f), CornerRadius(4f), style = stroke)
                        line(3f, 7f, 21f, 7f); line(9f, 11f, 15f, 17f); line(15f, 11f, 9f, 17f)
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
        if (symbol == AppSymbol.PlanToday) {
            Text(calendarDay, Modifier.offset(y = 2.dp), color = tint, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
