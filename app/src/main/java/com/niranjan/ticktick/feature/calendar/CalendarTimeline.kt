package com.niranjan.ticktick.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.Task
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay

private val GutterWidth = 46.dp
private val GridLineColor = Color(0x14202124)
private val NowLineColor = Color(0xFFE05B5B)

/** Scrollable 24-hour grid shared by the Day (one column) and Week (seven columns) views. */
@Composable
internal fun CalendarTimeline(
    days: List<CalendarDay>,
    today: LocalDate,
    clock: Clock,
    modifier: Modifier = Modifier,
    onOpenTask: (Task) -> Unit,
) {
    val compact = days.size > 1
    val hourHeight = if (compact) 56.dp else 64.dp
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    var now by remember { mutableStateOf(LocalTime.now(clock)) }
    LaunchedEffect(clock) {
        while (true) {
            now = LocalTime.now(clock)
            delay(60_000)
        }
    }
    LaunchedEffect(Unit) {
        val firstStart = days.flatMap { it.timedTasks }.mapNotNull { it.start }.minOrNull()
        val hour = ((firstStart?.hour ?: 8) - 1).coerceIn(0, 20)
        scroll.scrollTo(with(density) { (hourHeight * hour).roundToPx() })
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columnWidth = (maxWidth - GutterWidth) / days.size
        Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(Modifier.fillMaxWidth().height(hourHeight * 24)) {
                repeat(24) { hour ->
                    val y = hourHeight * hour
                    Box(Modifier.offset(y = y).padding(start = GutterWidth).fillMaxWidth().height(1.dp).background(GridLineColor))
                    if (hour > 0) Text(
                        hourLabel(hour), Modifier.offset(y = y - 7.dp).width(GutterWidth - 8.dp),
                        fontSize = 10.sp, color = TickTickColors.SecondaryText, textAlign = TextAlign.End, maxLines = 1,
                    )
                }
                days.forEachIndexed { index, day ->
                    val left = GutterWidth + columnWidth * index
                    if (index > 0) Box(Modifier.offset(x = left).width(1.dp).fillMaxHeight().background(GridLineColor))
                    positionTimed(day.timedTasks).forEach { positioned ->
                        val start = positioned.item.start ?: return@forEach
                        val startMinutes = start.toSecondOfDay() / 60f
                        val endMinutes = maxOf(visualEnd(positioned.item).toSecondOfDay() / 60f, startMinutes + 30f)
                        val width = (columnWidth - 4.dp) / positioned.columns
                        Box(
                            Modifier
                                .offset(x = left + 2.dp + width * positioned.column, y = hourHeight * (startMinutes / 60f))
                                .width(width - 1.dp)
                                .height(hourHeight * ((endMinutes - startMinutes) / 60f) - 2.dp),
                        ) { TimedBlock(positioned.item, compact) { onOpenTask(positioned.item.task) } }
                    }
                }
                val todayIndex = days.indexOfFirst { it.date == today }
                if (todayIndex >= 0) {
                    val y = hourHeight * (now.toSecondOfDay() / 3600f)
                    val left = GutterWidth + columnWidth * todayIndex
                    Box(Modifier.offset(x = left, y = y - 1.dp).width(columnWidth).height(2.dp).background(NowLineColor))
                    Box(Modifier.offset(x = left - 2.dp, y = y - 3.dp).size(6.dp).background(NowLineColor, CircleShape))
                }
            }
        }
    }
}

private fun hourLabel(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}
