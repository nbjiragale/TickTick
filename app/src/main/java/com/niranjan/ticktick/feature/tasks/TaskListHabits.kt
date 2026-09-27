package com.niranjan.ticktick.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.HabitSnapshot
import com.niranjan.ticktick.domain.model.TaskFilter
import com.niranjan.ticktick.domain.model.habitsFor
import com.niranjan.ticktick.feature.habits.HabitAvatar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class ListHabitOccurrence(val habit: Habit, val date: LocalDate, val completed: Boolean)

internal fun listHabitOccurrences(state: TasksUiState, snapshot: HabitSnapshot): List<ListHabitOccurrence> {
    if (state.presentation.layout != TaskLayout.List || state.filter != TaskFilter.Today || !snapshot.settings.showInTodayAndNext7Days) return emptyList()
    // Overdue is a task filter; habits do not accumulate overdue check-ins. Keep today's habit card visible there.
    val dates = if (state.homeSection == HomeTaskSection.Upcoming) (1L..7L).map(state.today::plusDays) else listOf(state.today)
    return dates.flatMap { date ->
        snapshot.habitsFor(date).map { habit -> ListHabitOccurrence(habit, date, date in snapshot.completedDates[habit.id].orEmpty()) }
            .filter { state.showCompleted || !it.completed }
    }
}

internal fun LazyListScope.taskListHabits(occurrences: List<ListHabitOccurrence>, today: LocalDate, onOpen: (Habit, LocalDate) -> Unit) {
    if (occurrences.isEmpty()) return
    item(key = "habit:header") {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Color.White)
            .padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Habit", Modifier.weight(1f), color = TickTickColors.DueTime, style = MaterialTheme.typography.titleMedium)
            Text(occurrences.size.toString(), color = TickTickColors.SecondaryText, style = MaterialTheme.typography.bodyMedium)
        }
    }
    itemsIndexed(occurrences, key = { _, item -> "habit:${item.habit.id}:${item.date}" }) { index, item ->
        val bottom = if (index == occurrences.lastIndex) 16.dp else 0.dp
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = bottom, bottomEnd = bottom)).background(Color.White)
            .clickable(role = Role.Button, onClickLabel = "Open ${item.habit.name} check-in for ${item.date}", onClick = { onOpen(item.habit, item.date) })
            .heightIn(min = 54.dp).padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            HabitAvatar(item.habit.icon, Modifier.size(30.dp))
            Text(item.habit.name, Modifier.weight(1f).padding(start = 4.dp, end = 8.dp), style = MaterialTheme.typography.bodyLarge,
                color = if (item.completed) TickTickColors.SecondaryText else TickTickColors.Text,
                textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Column(horizontalAlignment = Alignment.End) {
                Text(when (item.date) {
                    today -> "Today"
                    today.plusDays(1) -> "Tomorrow"
                    else -> item.date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
                }, color = TickTickColors.DueTime, style = MaterialTheme.typography.bodyMedium)
                if (item.completed) AppIcon(AppSymbol.Check, Modifier.size(12.dp), TickTickColors.Accent)
                else if (item.habit.reminders.isNotEmpty()) AppIcon(AppSymbol.Alarm, Modifier.padding(top = 2.dp).size(12.dp), TickTickColors.Checkbox)
            }
        }
    }
}
