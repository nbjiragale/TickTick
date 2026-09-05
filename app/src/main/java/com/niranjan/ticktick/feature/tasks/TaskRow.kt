package com.niranjan.ticktick.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.core.designsystem.TickTickDimensions
import com.niranjan.ticktick.domain.model.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskRow(task: Task, today: LocalDate, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(TickTickDimensions.CardRadius)
    Row(
        modifier.fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = Color(0xFF17243A).copy(alpha = .12f), spotColor = Color(0xFF17243A).copy(alpha = .08f))
            .clip(shape).background(TickTickColors.Surface)
            .clickable(onClickLabel = "Edit ${task.title}", onClick = onOpen)
            .heightIn(min = TickTickDimensions.TaskHeight)
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp, 44.dp)
                .toggleable(task.completed, role = Role.Checkbox, onValueChange = { onToggle() })
                .semantics { contentDescription = if (task.completed) "Mark ${task.title} incomplete" else "Complete ${task.title}" },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (task.completed) TickTickColors.Accent else Color.Transparent)
                    .border(1.4.dp, if (task.completed) TickTickColors.Accent else TickTickColors.Checkbox, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (task.completed) AppIcon(AppSymbol.Check, Modifier.size(13.dp), Color.White)
            }
        }
        Text(
            text = task.title,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp, horizontal = 0.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (task.completed) TickTickColors.SecondaryText else TickTickColors.Text,
            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val label = task.dueLabel(today)
        if (label.isNotEmpty()) {
            Text(label, Modifier.padding(start = 12.dp), color = TickTickColors.DueTime, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

fun Task.dueLabel(today: LocalDate): String = when {
    dueDate == null -> ""
    dueDate == today -> dueTime?.format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)) ?: "Today"
    dueDate == today.plusDays(1) -> "Tomorrow"
    else -> dueDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
}
