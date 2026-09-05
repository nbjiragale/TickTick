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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskFilter
import com.niranjan.ticktick.feature.tasks.TasksUiState
import com.niranjan.ticktick.feature.tasks.TasksViewModel
import com.niranjan.ticktick.feature.tasks.dueLabel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Basic functional dialogs pending the next reference screenshots. */
@Composable
fun AppOverlays(
    overlay: String?,
    editingTaskId: String?,
    state: TasksUiState,
    viewModel: TasksViewModel,
    onDismiss: () -> Unit,
    onOpenTask: (String) -> Unit,
    onAddList: () -> Unit,
) {
    when (overlay) {
        "task" -> {
            val task = state.snapshot.tasks.find { it.id == editingTaskId }
            if (editingTaskId != null && task == null) {
                AlertDialog(onDismissRequest = onDismiss, title = { Text("Task unavailable") }, text = { Text("This task has been removed.") }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
            } else {
                TaskEntryDialog(task, state, onDismiss) { title, listId, date, time ->
                    viewModel.saveTask(task?.id, title, listId, date, time)
                    onDismiss()
                }
            }
        }
        "addList" -> {
            var name by rememberSaveable { mutableStateOf("") }
            val duplicate = state.snapshot.lists.any { it.name.equals(name.trim(), ignoreCase = true) }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Add List") },
                text = {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, isError = duplicate,
                        supportingText = { if (duplicate) Text("A list with this name already exists") })
                },
                confirmButton = { TextButton(enabled = name.isNotBlank() && !duplicate, onClick = { viewModel.addList(name); onDismiss() }) { Text("Save") } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            )
        }
        "search", "suggestions", "reminders" -> {
            var query by rememberSaveable(overlay) { mutableStateOf("") }
            val tasks = when (overlay) {
                "search" -> state.snapshot.tasks.filter { it.title.contains(query.trim(), ignoreCase = true) }
                "suggestions" -> state.snapshot.tasks.filter { !it.completed && it.dueDate?.isAfter(state.today) == true }
                else -> state.snapshot.tasks.filter { !it.completed && it.dueTime != null }
            }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(when (overlay) { "search" -> "Search"; "suggestions" -> "Suggested Tasks"; else -> "Scheduled tasks" }) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (overlay == "search") OutlinedTextField(query, { query = it }, label = { Text("Search tasks") }, singleLine = true)
                        if (tasks.isEmpty()) Text(if (overlay == "suggestions") "No upcoming tasks" else "No tasks found", color = TickTickColors.SecondaryText)
                        LazyColumn(Modifier.heightIn(max = 350.dp)) {
                            items(tasks, key = { it.id }) { task ->
                                Row(Modifier.fillMaxWidth().clickable { onOpenTask(task.id) }.padding(vertical = 12.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(task.title, style = MaterialTheme.typography.bodyLarge)
                                        Text(task.dueLabel(state.today), color = TickTickColors.DueTime, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (overlay == "suggestions") TextButton(onClick = { viewModel.moveToToday(task) }) { Text("Today") }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
            )
        }
        "manageLists" -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Lists") },
            text = {
                LazyColumn(Modifier.heightIn(max = 350.dp)) {
                    items(state.snapshot.lists, key = { it.id }) { list ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.selectList(list.id); onDismiss() }.padding(vertical = 12.dp)) {
                            Text(list.name, Modifier.weight(1f))
                            Text(state.listCount(list.id).toString(), color = TickTickColors.SecondaryText)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onAddList) { Text("Add List") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )
        "settings" -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Appearance", fontWeight = FontWeight.Medium)
                    Text("Light", color = TickTickColors.SecondaryText)
                    TextButton(onClick = viewModel::toggleShowCompleted) { Text(if (state.showCompleted) "Hide completed tasks" else "Show completed tasks") }
                    TextButton(onClick = viewModel::toggleSort) { Text(if (state.sortByTime) "Use list order" else "Sort tasks by due date") }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        )
    }
}

@Composable
private fun TaskEntryDialog(
    task: Task?,
    state: TasksUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate?, LocalTime?) -> Unit,
) {
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    val defaultList = (state.filter as? TaskFilter.ListFilter)?.listId ?: "inbox"
    var listId by rememberSaveable(task?.id) { mutableStateOf(task?.listId ?: defaultList) }
    var dateText by rememberSaveable(task?.id) {
        mutableStateOf((task?.dueDate ?: if (task == null && state.filter == TaskFilter.Today) state.today else null)?.toString().orEmpty())
    }
    var timeText by rememberSaveable(task?.id) { mutableStateOf(task?.dueTime?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()) }
    var expanded by remember { mutableStateOf(false) }
    val parsedDate = dateText.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val parsedTime = timeText.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val dateInvalid = dateText.isNotBlank() && parsedDate == null
    val timeInvalid = timeText.isNotBlank() && (parsedTime == null || parsedDate == null)
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val initialDate = (task?.dueDate ?: if (task == null && state.filter == TaskFilter.Today) state.today else null)?.toString().orEmpty()
    val dirty = title != task?.title.orEmpty() || listId != (task?.listId ?: defaultList) || dateText != initialDate || timeText != task?.dueTime?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
    fun requestDismiss() { if (dirty) confirmDiscard = true else onDismiss() }

    AlertDialog(
        onDismissRequest = ::requestDismiss,
        title = { Text(if (task == null) "Add task" else "Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                Column {
                    TextButton(onClick = { expanded = true }) { Text(state.snapshot.lists.find { it.id == listId }?.name ?: "Inbox") }
                    DropdownMenu(expanded, { expanded = false }) {
                        state.snapshot.lists.forEach { list ->
                            DropdownMenuItem(text = { Text(list.name) }, onClick = { listId = list.id; expanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(dateText == state.today.toString(), { dateText = state.today.toString() }, label = { Text("Today") })
                    FilterChip(dateText == state.today.plusDays(1).toString(), { dateText = state.today.plusDays(1).toString() }, label = { Text("Tomorrow") })
                }
                OutlinedTextField(dateText, { dateText = it }, label = { Text("Date · YYYY-MM-DD") }, isError = dateInvalid, singleLine = true)
                OutlinedTextField(timeText, { timeText = it }, label = { Text("Time · HH:mm") }, isError = timeInvalid, singleLine = true,
                    supportingText = { if (timeInvalid) Text("Enter a valid date and 24-hour time") })
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank() && !dateInvalid && !timeInvalid, onClick = { onSave(title, listId, parsedDate, parsedTime) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = ::requestDismiss) { Text("Cancel") } },
    )
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard changes?") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
    )
}
