package com.niranjan.ticktick.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.Task
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Seven-day (Sunday-start) view with tappable day headers, all-day chips, and a shared timeline. */
@Composable
internal fun CalendarWeekView(
    selected: LocalDate,
    today: LocalDate,
    clock: Clock,
    data: CalendarData,
    onSelect: (LocalDate) -> Unit,
    onOpenTask: (Task) -> Unit,
    onOpenHabit: (Habit, LocalDate) -> Unit,
) {
    val start = sundayStart(selected)
    val days = remember(start, data) { (0L..6L).map { data.dayFor(start.plusDays(it)) } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            Spacer(Modifier.width(46.dp))
            days.forEach { day ->
                val date = day.date
                val active = date == selected
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .selectable(active, role = Role.Tab) { onSelect(date) }
                        .semantics { contentDescription = date.format(FullDate) }
                        .padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(date.format(Weekday), fontSize = 10.sp, color = TickTickColors.SecondaryText)
                    Box(
                        Modifier.padding(top = 2.dp).size(24.dp).clip(CircleShape)
                            .background(if (active) TickTickColors.Accent else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            date.dayOfMonth.toString(), fontSize = 12.sp,
                            color = if (active) Color.White else if (date == today) TickTickColors.Accent else TickTickColors.Text,
                        )
                    }
                }
            }
        }
        if (days.any { it.allDayItems.isNotEmpty() }) {
            Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp)) {
                Text(
                    "All-day", Modifier.width(46.dp).padding(end = 6.dp),
                    fontSize = 9.sp, color = TickTickColors.SecondaryText, textAlign = TextAlign.End,
                )
                days.forEach { day ->
                    Column(Modifier.weight(1f).padding(horizontal = 1.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        day.allDayItems.take(2).forEach { item -> WeekAllDayChip(item, itemAction(item, onOpenTask, onOpenHabit)) }
                        val extra = day.allDayItems.size - 2
                        if (extra > 0) Text(
                            "+$extra",
                            Modifier.clickable(role = Role.Button, onClickLabel = "Select day") { onSelect(day.date) }.padding(horizontal = 3.dp),
                            fontSize = 9.sp, color = TickTickColors.SecondaryText,
                        )
                    }
                }
            }
        }
        CalendarTimeline(days, today, clock, Modifier.weight(1f), onOpenTask)
    }
}

private val Weekday = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val FullDate = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
