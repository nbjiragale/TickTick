package com.niranjan.ticktick.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.Task
import java.time.Clock
import java.time.LocalDate

/** Hourly timeline for one date with an all-day area for holidays, date-only tasks, and habits. */
@Composable
internal fun CalendarDayView(
    date: LocalDate,
    today: LocalDate,
    clock: Clock,
    data: CalendarData,
    onOpenTask: (Task) -> Unit,
    onOpenHabit: (Habit, LocalDate) -> Unit,
) {
    val day = remember(date, data) { data.dayFor(date) }
    Column(Modifier.fillMaxSize()) {
        if (day.allDayItems.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 128.dp).verticalScroll(rememberScrollState()).padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                day.allDayItems.forEach { item -> AllDayChip(item, itemAction(item, onOpenTask, onOpenHabit)) }
            }
        }
        if (day.isEmpty) {
            Text(
                "Nothing scheduled on this day", Modifier.padding(vertical = 6.dp),
                fontSize = 12.sp, color = TickTickColors.SecondaryText,
            )
        }
        CalendarTimeline(listOf(day), today, clock, Modifier.weight(1f), onOpenTask)
    }
}
