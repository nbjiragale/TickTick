package com.niranjan.ticktick.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.feature.tasks.TasksUiState
import com.niranjan.ticktick.feature.tasks.TasksViewModel
import com.niranjan.ticktick.feature.tasks.dueLabel
import com.niranjan.ticktick.feature.organization.AddListOrTagScreen
import com.niranjan.ticktick.feature.organization.AddFilterScreen

/** Basic functional dialogs pending the next reference screenshots. */
@Composable
fun AppOverlays(
    sounds: com.niranjan.ticktick.platform.sounds.AppSounds,
    reminderController: com.niranjan.ticktick.platform.reminders.ReminderController,
    overlay: String?,
    state: TasksUiState,
    viewModel: TasksViewModel,
    onDismiss: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOrganizationSaved: () -> Unit = {},
    onPreviewReminder: ((String) -> Unit)? = null,
    onDelayedReminder: ((String) -> Unit)? = null,
    onPreviewAllReminders: (() -> Unit)? = null,
    snoozeSummary: (String) -> String? = { null },
) {
    when (overlay) {
        "addList", "addTag" -> AddListOrTagScreen(overlay == "addTag", onDismiss) { name, color, view ->
            (if (overlay == "addTag") viewModel.addTag(name, color) else viewModel.addList(name, color, view))
                .onSuccess { onOrganizationSaved() }
        }
        "addFilter" -> AddFilterScreen(state.snapshot, onDismiss) { name, rules, matchAll ->
            viewModel.addFilter(name, rules, matchAll).onSuccess { onOrganizationSaved() }
        }
        "search", "reminders" -> {
            var query by rememberSaveable(overlay) { mutableStateOf("") }
            val tasks = when (overlay) {
                "search" -> state.snapshot.tasks.filter { it.title.contains(query.trim(), ignoreCase = true) }
                else -> state.snapshot.tasks.filter { it.isActive && !it.isNote && it.dueDate != null &&
                    (it.dueTime != null || it.hasReminder || it.id in state.snapshot.snoozedUntil) }
            }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(if (overlay == "search") "Search" else "Scheduled tasks") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (overlay == "reminders") com.niranjan.ticktick.feature.reminders.ReminderSettings(reminderController)
                        if (overlay == "search") OutlinedTextField(query, { query = it }, label = { Text("Search tasks") }, singleLine = true)
                        if (overlay == "reminders" && onPreviewReminder != null) {
                            Text("Debug preview. Actions change saved tasks; snooze can schedule a real reminder.",
                                style = MaterialTheme.typography.bodySmall, color = TickTickColors.SecondaryText)
                            if (tasks.size > 1 && onPreviewAllReminders != null)
                                TextButton(onClick = onPreviewAllReminders) { Text("Preview all reminders") }
                        }
                        if (tasks.isEmpty()) Text("No tasks found", color = TickTickColors.SecondaryText)
                        LazyColumn(Modifier.heightIn(max = 350.dp)) {
                            items(tasks, key = { it.id }) { task ->
                                Row(Modifier.fillMaxWidth().clickable { onOpenTask(task.id) }.padding(vertical = 12.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(task.title, style = MaterialTheme.typography.bodyLarge)
                                        Text(task.dueLabel(state.today), color = TickTickColors.DueTime, style = MaterialTheme.typography.bodySmall)
                                        if (overlay == "reminders" && onPreviewReminder != null) {
                                            snoozeSummary(task.id)?.let { Text(it, color = TickTickColors.DueTime, style = MaterialTheme.typography.bodySmall) }
                                            TextButton(onClick = { onPreviewReminder(task.id) }) { Text("Preview Reminder") }
                                            if (onDelayedReminder != null)
                                                TextButton(onClick = { onDelayedReminder(task.id) }) { Text("Preview in 10 seconds") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
            )
        }
        "settings" -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Settings") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    com.niranjan.ticktick.feature.settings.SoundSettings(sounds)
                    com.niranjan.ticktick.feature.reminders.ReminderSettings(reminderController)
                    Text("Appearance", fontWeight = FontWeight.Medium)
                    Text("Light", color = TickTickColors.SecondaryText)
                    TextButton(onClick = viewModel::toggleShowCompleted) { Text(if (state.showCompleted) "Hide completed tasks" else "Show completed tasks") }
                    TextButton(onClick = viewModel::toggleSort) { Text(if (state.sortByTime) "Sort tasks by creation time" else "Sort tasks by due date") }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        )
    }
}
