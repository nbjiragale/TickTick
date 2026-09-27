package com.niranjan.ticktick.feature.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niranjan.ticktick.domain.repository.UiStateRepository
import com.niranjan.ticktick.domain.repository.taskWriteResult
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.HabitSnapshot
import com.niranjan.ticktick.domain.model.HolidaySnapshot
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.feature.tasks.TasksUiState
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CalendarViewMode(val label: String) { List("List"), Day("Day"), Week("Week"), Month("Month") }

@Composable
fun CalendarScreen(
    state: TasksUiState,
    habitSnapshot: HabitSnapshot,
    holidaySnapshot: HolidaySnapshot,
    clock: Clock,
    preferences: UiStateRepository,
    onToggleTask: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onOpenHabit: (Habit, LocalDate) -> Unit,
    onToggleShowCompleted: () -> Unit,
) {
    val today = state.today
    val stored by preferences.snapshot.collectAsStateWithLifecycle()
    val options = stored.preferences["calendar"]?.let(::JSONObject) ?: JSONObject()
    val viewName = options.optString("view", CalendarViewMode.Month.name)
    val view = CalendarViewMode.entries.firstOrNull { it.name == viewName } ?: CalendarViewMode.Month
    val selectedEpochDay = if (options.optBoolean("followToday", true)) today.toEpochDay() else options.optLong("date", today.toEpochDay())
    val selected = LocalDate.ofEpochDay(selectedEpochDay)
    val scope = rememberCoroutineScope()
    var writeError by remember { mutableStateOf<String?>(null) }
    fun update(transform: (JSONObject) -> JSONObject) {
        scope.launch { taskWriteResult { preferences.updatePreference("calendar") { transform(it?.let(::JSONObject) ?: JSONObject()).toString() } }
            .onFailure { writeError = it.message ?: "Couldn't save Calendar settings." } }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val data = remember(state.snapshot.tasks, state.snapshot.occurrenceHistory, habitSnapshot, holidaySnapshot, state.calendarShowCompleted) {
        CalendarData(state.snapshot.tasks, habitSnapshot, holidaySnapshot, state.calendarShowCompleted, state.snapshot.occurrenceHistory)
    }

    fun select(date: LocalDate) {
        update { it.put("date", date.toEpochDay()).put("followToday", date == today) }
    }
    fun shift(direction: Long) = select(
        when (view) {
            CalendarViewMode.Day -> selected.plusDays(direction)
            CalendarViewMode.Week -> selected.plusWeeks(direction)
            else -> YearMonth.from(selected).plusMonths(direction).let { it.atDay(minOf(selected.dayOfMonth, it.lengthOfMonth())) }
        },
    )

    val unit = when (view) {
        CalendarViewMode.Day -> "day"
        CalendarViewMode.Week -> "week"
        else -> "month"
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        CalendarViewSelector(view) { mode -> update { it.put("view", mode.name) } }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            NavChevron("Previous $unit", flip = true) { shift(-1) }
            Text(
                periodLabel(view, selected, today), Modifier.weight(1f), textAlign = TextAlign.Center,
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TickTickColors.Text,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            NavChevron("Next $unit", flip = false) { shift(1) }
            Box(
                Modifier.padding(start = 2.dp).clip(CircleShape)
                    .border(1.dp, TickTickColors.Accent.copy(alpha = .45f), CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Go to today") { select(today) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) { Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TickTickColors.Accent) }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp).semantics { contentDescription = "Calendar options" }) {
                    AppIcon(AppSymbol.More, Modifier.size(20.dp), TickTickColors.SecondaryText)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (state.calendarShowCompleted) "Hide Completed" else "Show Completed") },
                        onClick = {
                            onToggleShowCompleted()
                            menuOpen = false
                        },
                    )
                }
            }
        }
        CalendarLegend(holidaySnapshot.holidays.isNotEmpty(), holidaySnapshot.sourceLabel)
        Box(Modifier.weight(1f)) {
            when (view) {
                CalendarViewMode.List -> CalendarListView(selected, today, data, onToggleTask, onOpenTask, onOpenHabit)
                CalendarViewMode.Day -> CalendarDayView(selected, today, clock, data, onOpenTask, onOpenHabit)
                CalendarViewMode.Week -> CalendarWeekView(selected, today, clock, data, ::select, onOpenTask, onOpenHabit)
                CalendarViewMode.Month -> CalendarMonthView(selected, today, data, ::select, onToggleTask, onOpenTask, onOpenHabit)
            }
        }
    }
    writeError?.let { message -> androidx.compose.material3.AlertDialog(onDismissRequest = { writeError = null },
        text = { Text(message) }, confirmButton = { androidx.compose.material3.TextButton(onClick = { writeError = null }) { Text("OK") } }) }
}

@Composable
private fun NavChevron(label: String, flip: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(AppSymbol.Chevron, Modifier.size(16.dp).rotate(if (flip) 180f else 0f), TickTickColors.SecondaryText)
    }
}

private fun periodLabel(view: CalendarViewMode, selected: LocalDate, today: LocalDate): String = when (view) {
    CalendarViewMode.Day ->
        selected.format(DateTimeFormatter.ofPattern(if (selected.year == today.year) "EEE, MMM d" else "EEE, MMM d, yyyy", Locale.ENGLISH))
    CalendarViewMode.Week -> {
        val start = sundayStart(selected)
        val end = start.plusDays(6)
        val startText = start.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
        val endText = end.format(DateTimeFormatter.ofPattern(if (start.month == end.month) "d" else "MMM d", Locale.ENGLISH))
        val year = if (end.year == today.year) "" else ", ${end.year}"
        "$startText – $endText$year"
    }
    else ->
        YearMonth.from(selected).format(DateTimeFormatter.ofPattern(if (selected.year == today.year) "MMMM" else "MMMM yyyy", Locale.ENGLISH))
}
