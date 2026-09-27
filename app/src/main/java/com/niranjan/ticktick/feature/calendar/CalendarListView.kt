package com.niranjan.ticktick.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.Task
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Agenda for the selected date's month, grouped by date; empty dates are omitted. */
@Composable
internal fun CalendarListView(
    selected: LocalDate,
    today: LocalDate,
    data: CalendarData,
    onToggleTask: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onOpenHabit: (Habit, LocalDate) -> Unit,
) {
    val month = YearMonth.from(selected)
    val days = remember(month, data) {
        (1..month.lengthOfMonth()).map { data.dayFor(month.atDay(it)) }.filter { !it.isEmpty }
    }
    if (days.isEmpty()) {
        CalendarEmptyState("Nothing scheduled in ${month.format(MonthTitle)}")
        return
    }
    val listState = rememberLazyListState()
    val headerIndices = remember(days) {
        val indices = mutableListOf<Int>()
        var accumulated = 0
        days.forEach { day ->
            indices += accumulated
            accumulated += 1 + day.agendaItems.size
        }
        indices
    }
    LaunchedEffect(month) {
        val target = days.indexOfFirst { it.date >= selected }
        if (target > 0) listState.scrollToItem(headerIndices[target])
    }
    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        days.forEach { day ->
            item(key = "header:${day.date}") { AgendaDayHeader(day.date, today) }
            agendaDay(day, onToggleTask, onOpenTask, onOpenHabit)
        }
    }
}

private val MonthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
