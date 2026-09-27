package com.niranjan.ticktick.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Month grid (Sunday-start) with event previews and the selected day's agenda below. */
@Composable
internal fun CalendarMonthView(
    selected: LocalDate,
    today: LocalDate,
    data: CalendarData,
    onSelect: (LocalDate) -> Unit,
    onToggleTask: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onOpenHabit: (Habit, LocalDate) -> Unit,
) {
    val month = YearMonth.from(selected)
    val gridStart = sundayStart(month.atDay(1))
    val weeks = remember(month) { (ChronoUnit.DAYS.between(gridStart, sundayStart(month.atEndOfMonth())) / 7 + 1).toInt() }
    val days = remember(month, data) { (0 until weeks * 7).map { data.dayFor(gridStart.plusDays(it.toLong())) } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 10.sp, color = TickTickColors.SecondaryText)
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TickTickColors.Surface)
                .padding(2.dp).selectableGroup(),
        ) {
            repeat(weeks) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { index ->
                        MonthCell(days[week * 7 + index], month, selected, today, Modifier.weight(1f), onSelect)
                    }
                }
            }
        }
        val selectedDay = remember(selected, data) { data.dayFor(selected) }
        AgendaDayHeader(selected, today, Modifier.padding(top = 6.dp))
        Box(Modifier.weight(1f)) {
            if (selectedDay.isEmpty) CalendarEmptyState("Nothing on this day")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)) {
                agendaDay(selectedDay, onToggleTask, onOpenTask, onOpenHabit)
            }
        }
    }
}

@Composable
private fun MonthCell(
    day: CalendarDay,
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    modifier: Modifier,
    onSelect: (LocalDate) -> Unit,
) {
    val date = day.date
    val inMonth = YearMonth.from(date) == month
    val active = date == selected
    val description = "${date.format(FullDate)}, ${day.totalCount} item${if (day.totalCount == 1) "" else "s"}"
    Column(
        modifier.height(58.dp).padding(1.dp).clip(RoundedCornerShape(8.dp))
            .background(if (active) TickTickColors.TodayChip else Color.Transparent)
            .selectable(active, role = Role.Button) { onSelect(date) }
            .semantics { contentDescription = description }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(18.dp).clip(CircleShape).background(if (date == today) TickTickColors.Accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(), fontSize = 10.sp,
                color = when {
                    date == today -> Color.White
                    !inMonth -> TickTickColors.Checkbox
                    else -> TickTickColors.Text
                },
            )
        }
        val chips = day.agendaItems.take(2)
        chips.forEach { item ->
            Box(Modifier.padding(top = 1.dp).fillMaxWidth()) { MonthChip(item, muted = !inMonth) }
        }
        val extra = day.totalCount - chips.size
        if (extra > 0) Text("+$extra", fontSize = 8.sp, lineHeight = 9.sp, color = TickTickColors.SecondaryText)
    }
}

private val FullDate = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
