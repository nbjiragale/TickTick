package com.niranjan.ticktick.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.core.designsystem.TickTickDimensions
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.feature.habits.HabitAvatar
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Soft category palette; icons and labels keep color from being the only distinction. */
internal enum class CalendarCategory(val label: String, val container: Color, val ink: Color, val border: Color, val symbol: AppSymbol) {
    Task("Tasks", Color(0xFFDFE9FD), Color(0xFF2D5FC9), Color(0xFFC3D6F8), AppSymbol.Tasks),
    Habit("Habits", Color(0xFFDDF2E4), Color(0xFF2E7D4F), Color(0xFFBFE4CC), AppSymbol.Habits),
    Holiday("Holidays", Color(0xFFFBEED6), Color(0xFF96660F), Color(0xFFEDD9AE), AppSymbol.Flag),
}

internal val CalendarItem.category: CalendarCategory
    get() = when (this) {
        is CalendarItem.TaskItem -> CalendarCategory.Task
        is CalendarItem.HabitItem -> CalendarCategory.Habit
        is CalendarItem.HolidayItem -> CalendarCategory.Holiday
    }

private val BlockTimeFormat = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
private val HeaderDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)

internal fun LocalTime.blockLabel(): String = format(BlockTimeFormat)

internal fun CalendarItem.timeLabel(): String = when (this) {
    is CalendarItem.TaskItem -> when {
        start == null -> "All day"
        task.duration == null -> start.blockLabel()
        else -> "${start.blockLabel()} – ${end?.blockLabel()}"
    }
    is CalendarItem.HabitItem -> habit.reminders.minOrNull()?.blockLabel() ?: "Habit"
    is CalendarItem.HolidayItem -> "Holiday"
}

internal fun CalendarItem.a11yLabel(): String = when (this) {
    is CalendarItem.TaskItem -> "Task, $title, ${timeLabel()}" +
        (if (task.declined) ", marked Won't Do" else if (task.completed) ", completed" else "")
    is CalendarItem.HabitItem -> "Habit, $title" + if (checkedIn) ", checked in" else ""
    is CalendarItem.HolidayItem -> "Holiday, $title"
}

/** Resolves the tap action per category; holidays are display-only. */
internal fun itemAction(item: CalendarItem, onOpenTask: (Task) -> Unit, onOpenHabit: (Habit, LocalDate) -> Unit): (() -> Unit)? = when (item) {
    is CalendarItem.TaskItem -> ({ onOpenTask(item.task) })
    is CalendarItem.HabitItem -> ({ onOpenHabit(item.habit, item.date) })
    is CalendarItem.HolidayItem -> null
}

/** Category glyph: habits show their emoji, other categories their symbol. */
@Composable
internal fun ItemGlyph(item: CalendarItem, iconSize: Dp, emojiSize: TextUnit, tint: Color = item.category.ink) {
    if (item is CalendarItem.HabitItem) Text(item.habit.icon, fontSize = emojiSize, maxLines = 1)
    else AppIcon(item.category.symbol, Modifier.size(iconSize), tint)
}

@Composable
internal fun AgendaTaskRow(item: CalendarItem.TaskItem, onToggle: (Task) -> Unit, onOpen: (Task) -> Unit) {
    val task = item.task
    val palette = CalendarCategory.Task
    val shape = RoundedCornerShape(TickTickDimensions.CardRadius)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(palette.container).border(1.dp, palette.border, shape)
            .clickable(onClickLabel = "Edit ${task.title}") { onOpen(task) }
            .heightIn(min = 48.dp).padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp, 48.dp)
                .toggleable(!task.isActive, enabled = item.actionable, role = Role.Checkbox, onValueChange = { onToggle(task) })
                .semantics {
                    contentDescription = if (!item.actionable) "Occurrence preview for ${task.title}. Open the task to edit the series."
                    else if (task.declined) "Restore ${task.title}, marked Won't Do"
                    else if (task.completed) "Mark ${task.title} incomplete" else "Complete ${task.title}"
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (task.completed) palette.ink else Color.Transparent)
                    .border(1.4.dp, if (task.completed) palette.ink else palette.ink.copy(alpha = .5f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (task.completed) AppIcon(AppSymbol.Check, Modifier.size(13.dp), Color.White)
                if (task.declined) AppIcon(AppSymbol.Close, Modifier.size(12.dp), palette.ink)
            }
        }
        AppIcon(palette.symbol, Modifier.size(13.dp), palette.ink)
        Text(
            task.title, Modifier.weight(1f).padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.done) TickTickColors.SecondaryText else TickTickColors.Text,
            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Text(item.timeLabel(), Modifier.padding(start = 8.dp), fontSize = 11.sp, color = palette.ink, maxLines = 1)
    }
}

@Composable
internal fun AgendaHabitRow(item: CalendarItem.HabitItem, onOpen: (Habit, LocalDate) -> Unit) {
    val palette = CalendarCategory.Habit
    val shape = RoundedCornerShape(TickTickDimensions.CardRadius)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(palette.container).border(1.dp, palette.border, shape)
            .clickable(role = Role.Button, onClickLabel = "Open check-in") { onOpen(item.habit, item.date) }
            .heightIn(min = 48.dp).padding(horizontal = 10.dp)
            .semantics { contentDescription = item.a11yLabel() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabitAvatar(item.habit.icon, Modifier.size(26.dp))
        Text(
            item.habit.name, Modifier.weight(1f).padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.checkedIn) TickTickColors.SecondaryText else TickTickColors.Text,
            textDecoration = if (item.checkedIn) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        if (item.checkedIn) AppIcon(AppSymbol.Check, Modifier.padding(start = 6.dp).size(12.dp), palette.ink)
        Text(item.timeLabel(), Modifier.padding(start = 8.dp), fontSize = 11.sp, color = palette.ink, maxLines = 1)
    }
}

@Composable
internal fun AgendaHolidayRow(item: CalendarItem.HolidayItem) {
    val palette = CalendarCategory.Holiday
    val shape = RoundedCornerShape(TickTickDimensions.CardRadius)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(palette.container).border(1.dp, palette.border, shape)
            .heightIn(min = 44.dp).padding(horizontal = 12.dp)
            .semantics { contentDescription = item.a11yLabel() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(palette.symbol, Modifier.size(14.dp), palette.ink)
        Text(
            item.holiday.name, Modifier.weight(1f).padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = TickTickColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Text("Holiday", fontSize = 11.sp, color = palette.ink)
    }
}

/** Emits one day's agenda rows with stable keys; shared by List view and the Month day agenda. */
internal fun LazyListScope.agendaDay(day: CalendarDay, onToggleTask: (Task) -> Unit, onOpenTask: (Task) -> Unit, onOpenHabit: (Habit, LocalDate) -> Unit) {
    items(day.holidays, key = { "holiday:${day.date}:${it.holiday.id}" }) { AgendaHolidayRow(it) }
    items(day.timedTasks, key = { "timed:${day.date}:${it.key}" }) { AgendaTaskRow(it, onToggleTask, onOpenTask) }
    items(day.allDayTasks, key = { "allday:${day.date}:${it.key}" }) { AgendaTaskRow(it, onToggleTask, onOpenTask) }
    items(day.habits, key = { "habit:${day.date}:${it.habit.id}" }) { AgendaHabitRow(it, onOpenHabit) }
}

@Composable
internal fun AgendaDayHeader(date: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    val suffix = when (date) {
        today -> " · Today"
        today.plusDays(1) -> " · Tomorrow"
        today.minusDays(1) -> " · Yesterday"
        else -> ""
    }
    val year = if (date.year == today.year) "" else ", ${date.year}"
    Text(
        date.format(HeaderDateFormat) + year + suffix, modifier.padding(top = 10.dp, bottom = 2.dp),
        fontSize = 13.sp, fontWeight = FontWeight.Bold,
        color = if (date == today) TickTickColors.Accent else TickTickColors.SecondaryText,
    )
}

/** Full-width all-day entry used by the Day view. */
@Composable
internal fun AllDayChip(item: CalendarItem, onClick: (() -> Unit)?) {
    val palette = item.category
    val shape = RoundedCornerShape(8.dp)
    val struck = item is CalendarItem.TaskItem && item.done || item is CalendarItem.HabitItem && item.checkedIn
    Row(
        Modifier.fillMaxWidth().clip(shape).background(palette.container).border(1.dp, palette.border, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 30.dp).padding(horizontal = 8.dp)
            .semantics { contentDescription = item.a11yLabel() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemGlyph(item, 12.dp, 11.sp)
        Text(
            item.title, Modifier.weight(1f).padding(start = 6.dp), fontSize = 12.sp,
            color = if (struck) TickTickColors.SecondaryText else TickTickColors.Text,
            textDecoration = if (struck) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(item.timeLabel(), fontSize = 10.sp, color = palette.ink, maxLines = 1)
    }
}

/** Tiny all-day chip inside a Week view column. */
@Composable
internal fun WeekAllDayChip(item: CalendarItem, onClick: (() -> Unit)?) {
    val palette = item.category
    val shape = RoundedCornerShape(4.dp)
    Row(
        Modifier.fillMaxWidth().height(16.dp).clip(shape).background(palette.container)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 3.dp)
            .semantics { contentDescription = item.a11yLabel() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemGlyph(item, 8.dp, 8.sp)
        Text(
            item.title, Modifier.padding(start = 2.dp), fontSize = 8.sp, lineHeight = 9.sp,
            color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mini event preview inside a Month grid cell. */
@Composable
internal fun MonthChip(item: CalendarItem, muted: Boolean) {
    val palette = item.category
    Row(
        Modifier.alpha(if (muted) .55f else 1f).fillMaxWidth().height(14.dp)
            .clip(RoundedCornerShape(3.dp)).background(palette.container).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(palette.symbol, Modifier.size(8.dp), palette.ink)
        Text(
            item.title, Modifier.padding(start = 2.dp), fontSize = 8.sp, lineHeight = 9.sp,
            color = palette.ink, maxLines = 1, overflow = TextOverflow.Clip,
        )
    }
}

/** Timeline block for a timed task in the Day and Week views. */
@Composable
internal fun TimedBlock(item: CalendarItem.TaskItem, compact: Boolean, onClick: () -> Unit) {
    val palette = CalendarCategory.Task
    val shape = RoundedCornerShape(6.dp)
    Column(
        Modifier.fillMaxSize().clip(shape).background(palette.container).border(1.dp, palette.border, shape)
            .clickable(onClickLabel = "Edit ${item.task.title}", onClick = onClick)
            .padding(horizontal = if (compact) 3.dp else 8.dp, vertical = if (compact) 2.dp else 4.dp)
            .semantics { contentDescription = item.a11yLabel() },
    ) {
        if (compact) {
            AppIcon(palette.symbol, Modifier.size(8.dp), palette.ink)
            Text(
                item.task.title, fontSize = 9.sp, lineHeight = 10.sp,
                color = if (item.done) TickTickColors.SecondaryText else TickTickColors.Text,
                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 4, overflow = TextOverflow.Ellipsis,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(palette.symbol, Modifier.size(11.dp), palette.ink)
                Text(item.timeLabel(), Modifier.padding(start = 4.dp), fontSize = 10.sp, color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                item.task.title, Modifier.padding(top = 1.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = if (item.done) TickTickColors.SecondaryText else TickTickColors.Text,
                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun CalendarLegend(showHolidays: Boolean, holidaySource: String) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)
            .semantics { contentDescription = "Calendar legend" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LegendChip(CalendarCategory.Task, CalendarCategory.Task.label)
        LegendChip(CalendarCategory.Habit, CalendarCategory.Habit.label)
        if (showHolidays) LegendChip(CalendarCategory.Holiday, "Holidays · $holidaySource")
    }
}

@Composable
private fun LegendChip(palette: CalendarCategory, label: String) {
    Row(
        Modifier.clip(CircleShape).background(palette.container).border(1.dp, palette.border, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(palette.symbol, Modifier.size(10.dp), palette.ink)
        Text(label, Modifier.padding(start = 4.dp), fontSize = 10.sp, color = palette.ink, maxLines = 1)
    }
}

@Composable
internal fun CalendarViewSelector(selected: CalendarViewMode, onSelect: (CalendarViewMode) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TickTickColors.Surface).padding(3.dp).selectableGroup()) {
        CalendarViewMode.entries.forEach { mode ->
            val active = mode == selected
            Box(
                Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (active) TickTickColors.TodayChip else Color.Transparent)
                    .selectable(active, role = Role.Tab) { onSelect(mode) }
                    .semantics { contentDescription = "${mode.label} view" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mode.label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = if (active) TickTickColors.Accent else TickTickColors.SecondaryText,
                )
            }
        }
    }
}

@Composable
internal fun CalendarEmptyState(message: String, modifier: Modifier = Modifier) {
    Text(
        message, modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        color = TickTickColors.SecondaryText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
    )
}
