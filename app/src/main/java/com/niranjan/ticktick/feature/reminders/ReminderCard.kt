package com.niranjan.ticktick.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.feature.taskeditor.color
import com.niranjan.ticktick.feature.taskeditor.schedule.ScheduleIcon
import com.niranjan.ticktick.feature.taskeditor.schedule.ScheduleInk
import com.niranjan.ticktick.feature.taskeditor.schedule.ScheduleLabelBlue
import com.niranjan.ticktick.feature.taskeditor.schedule.ScheduleMuted
import com.niranjan.ticktick.feature.taskeditor.schedule.ScheduleRed
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ReminderCard(task: Task, list: TaskList?, clock: Clock, error: String?, modifier: Modifier,
    onSnooze: () -> Unit, onComplete: () -> Unit, onClose: () -> Unit) {
    Column(modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(AppSymbol.Flag, Modifier.size(16.dp), task.priority.color())
            Text(task.reminderDueLabel(clock), Modifier.padding(start = 8.dp).widthIn(max = 140.dp),
                fontSize = 14.sp, color = ScheduleLabelBlue, maxLines = 2, overflow = TextOverflow.Ellipsis)
            AppIcon(if (list?.symbol == ListSymbol.Inbox) AppSymbol.Inbox else AppSymbol.List,
                Modifier.padding(start = 12.dp).size(15.dp), ScheduleMuted)
            Text(list?.name.orEmpty(), Modifier.padding(start = 6.dp).weight(1f), fontSize = 14.sp,
                color = ScheduleMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            ScheduleIcon(AppSymbol.Close, "Dismiss reminder", onClose, Modifier.offset(x = 12.dp).size(36.dp), ScheduleMuted)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(task.title, Modifier.padding(top = 4.dp), fontSize = 20.sp, lineHeight = 26.sp,
                fontWeight = FontWeight.Bold, color = ScheduleInk)
            if (task.description.isNotBlank()) Text(task.description, Modifier.padding(top = 10.dp),
                fontSize = 15.sp, lineHeight = 22.sp, color = Color(0xFF808282))
            task.checklist.forEach { item ->
                Text("- ${item.text}", Modifier.padding(top = 12.dp), fontSize = 15.sp, lineHeight = 20.sp,
                    color = if (item.completed) ScheduleMuted else Color(0xFF808282),
                    textDecoration = if (item.completed) TextDecoration.LineThrough else null)
            }
            error?.let { Text(it, Modifier.padding(top = 8.dp), fontSize = 13.sp, color = ScheduleRed) }
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReminderButton("Snooze", Color(0xFFF7F7F7), Color(0xFF818382), Modifier.weight(1f), onSnooze)
            ReminderButton("Complete", Color(0xFFEAF1FF), ScheduleLabelBlue, Modifier.weight(1f), onComplete)
        }
    }
}

@Composable
private fun ReminderButton(label: String, background: Color, ink: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.heightIn(min = 42.dp).clip(RoundedCornerShape(8.dp)).background(background)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink)
    }
}

private fun Task.reminderDueLabel(clock: Clock): String {
    val today = LocalDate.now(clock)
    val day = when (dueDate) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> dueDate?.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)).orEmpty()
    }
    return day + (dueTime?.format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH))?.let { ", $it" } ?: "")
}
