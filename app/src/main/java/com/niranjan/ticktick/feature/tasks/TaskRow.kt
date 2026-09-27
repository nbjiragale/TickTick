package com.niranjan.ticktick.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.core.designsystem.TickTickDimensions
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskPriority
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskRow(task: Task, today: LocalDate, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier, grouped: Boolean = false,
    onDateClick: (() -> Unit)? = null, showDetails: Boolean = false, listLabel: String = "",
    selecting: Boolean = false, selected: Boolean = false, onLongPress: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(TickTickDimensions.CardRadius)
    val priorityColor = when (task.priority) {
        TaskPriority.High -> Color(0xFFE35D66)
        TaskPriority.Medium -> Color(0xFFE1BA39)
        TaskPriority.Low -> TickTickColors.DueTime
        TaskPriority.None -> TickTickColors.Checkbox
    }
    Row(
        modifier.fillMaxWidth()
            .then(if (grouped) Modifier else Modifier.shadow(2.dp, shape, ambientColor = Color(0xFF17243A).copy(alpha = .12f), spotColor = Color(0xFF17243A).copy(alpha = .08f)).clip(shape))
            .background(TickTickColors.Surface)
            .combinedClickable(onClickLabel = if (selecting) "Select ${task.title}" else "Edit ${task.title}", onClick = onOpen,
                onLongClickLabel = "Select task", onLongClick = onLongPress)
            .heightIn(min = TickTickDimensions.TaskHeight)
            .padding(start = if (grouped && showDetails) 5.dp else if (grouped) 6.dp else 8.dp, end = if (showDetails) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp, 44.dp)
                .then(if (showDetails) Modifier.align(Alignment.Top) else Modifier)
                .toggleable(if (selecting) selected else !task.isActive, role = Role.Checkbox, onValueChange = { onToggle() })
                .semantics { contentDescription = if (selecting) "Select ${task.title}" else if (task.declined) "Restore ${task.title}, marked Won't Do" else if (task.completed) "Mark ${task.title} incomplete" else "Complete ${task.title}" },
            contentAlignment = Alignment.Center,
        ) {
            if (selecting) {
                Box(Modifier.size(19.dp).clip(CircleShape)
                    .background(if (selected) TickTickColors.Accent else Color.Transparent)
                    .border(1.5.dp, if (selected) TickTickColors.Accent else if (task.isNote) Color(0xFFD5B32D) else priorityColor, CircleShape),
                    contentAlignment = Alignment.Center) {
                    if (selected) AppIcon(AppSymbol.Check, Modifier.size(13.dp), Color.White)
                }
            } else if (showDetails && task.isNote && task.isActive) {
                DetailNoteIcon()
            } else Box(
                Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (task.completed) TickTickColors.Accent else Color.Transparent)
                    .border(1.4.dp, if (task.completed) TickTickColors.Accent else priorityColor, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (task.completed) AppIcon(AppSymbol.Check, Modifier.size(13.dp), Color.White)
                if (task.declined) AppIcon(AppSymbol.Close, Modifier.size(12.dp), TickTickColors.SecondaryText)
                if (task.isNote && task.isActive) AppIcon(AppSymbol.Note, Modifier.size(14.dp), Color(0xFFD5B32D))
            }
        }
        Column(Modifier.weight(1f).padding(top = 10.dp, bottom = if (showDetails) 12.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (showDetails) 2.dp else 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (!task.isActive) TickTickColors.SecondaryText else TickTickColors.Text,
            textDecoration = if (!task.isActive) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (task.pinned) AppIcon(AppSymbol.Pin, Modifier.size(13.dp).semantics { contentDescription = "Pinned" }, TickTickColors.DueTime)
        }
        if (showDetails) {
            if (task.description.isNotBlank()) Text(task.description, style = MaterialTheme.typography.bodyMedium,
                color = TickTickColors.SecondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val detailDate = task.dueDate?.let { date ->
                    (if (date == today) "Today" else date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))) +
                        (task.dueTime?.let { ", ${it.format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH))}" } ?: "")
                }.orEmpty()
                if (detailDate.isNotEmpty()) Text(detailDate,
                    Modifier.weight(1f).alignByBaseline()
                        .then(if (onDateClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Edit date", onClick = onDateClick) else Modifier),
                    color = if (task.dueDate!! < today && task.isActive) Color(0xFFB7494E) else TickTickColors.DueTime,
                    style = MaterialTheme.typography.bodyMedium)
                else Spacer(Modifier.weight(1f))
                if (task.hasReminder) AppIcon(AppSymbol.Alarm, Modifier.size(12.dp), TickTickColors.SecondaryText)
                if (listLabel.isNotEmpty()) Text(listLabel, Modifier.widthIn(max = 112.dp).alignByBaseline(), color = TickTickColors.SecondaryText,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        }
        val label = task.dueLabel(today)
        if (!showDetails && label.isNotEmpty()) {
            Column(Modifier.then(if (onDateClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Edit date", onClick = onDateClick) else Modifier)
                .heightIn(min = 44.dp).padding(start = 12.dp, top = if (task.hasReminder) 5.dp else 12.dp), horizontalAlignment = Alignment.End) {
                Text(label, color = if (task.dueDate != null && task.dueDate < today && task.isActive) Color(0xFFB7494E) else TickTickColors.DueTime, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                if (task.hasReminder) AppIcon(AppSymbol.Alarm, Modifier.padding(top = 2.dp).size(12.dp), TickTickColors.Checkbox)
            }
        }
    }
}

@Composable
private fun DetailNoteIcon() {
    val color = Color(0xFFD5B32D)
    Canvas(Modifier.size(16.dp).border(1.4.dp, color, RoundedCornerShape(4.dp)).padding(4.dp)) {
        repeat(3) { index ->
            val y = size.height * index / 2f
            drawLine(color, Offset(0f, y), Offset(if (index == 2) size.width * .6f else size.width, y),
                strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

fun Task.dueLabel(today: LocalDate): String = when {
    dueDate == null -> ""
    dueDate == today -> dueTime?.format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)) ?: "Today"
    dueDate == today.plusDays(1) -> "Tomorrow"
    else -> dueDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
}
