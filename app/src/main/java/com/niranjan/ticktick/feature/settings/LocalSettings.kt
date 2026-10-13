package com.niranjan.ticktick.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niranjan.ticktick.core.settings.AppPreferences
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.repository.UiStateRepository
import com.niranjan.ticktick.domain.repository.taskWriteResult
import java.time.LocalTime
import kotlinx.coroutines.launch

@Composable
fun LocalSettings(repository: UiStateRepository, lists: List<TaskList>) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val preferences = remember(snapshot.preferences["app"]) { AppPreferences.decode(snapshot.preferences["app"]) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun save(change: (AppPreferences) -> AppPreferences) {
        if (busy) return
        busy = true
        scope.launch {
            try { taskWriteResult { repository.updatePreference("app") { change(AppPreferences.decode(it)).encode() } }
                .onFailure { error = it.message ?: "Couldn't save settings." } }
            finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Text("Light theme accent")
        Row {
            listOf("Blue" to 0xFF4778FF.toInt(), "Purple" to 0xFF7C4DCC.toInt(), "Green" to 0xFF248461.toInt()).forEach { (name, color) ->
                TextButton(enabled = !busy, onClick = { save { it.copy(accent = color) } }) { Text(if (preferences.accent == color) "✓ $name" else name) }
            }
        }
        Text("Navigation", style = MaterialTheme.typography.titleMedium)
        Text("Tasks stays first. Enable and reorder the other tabs.")
        val ordered = preferences.tabs.drop(1) + listOf("Calendar", "Habits", "Focus").filter { it !in preferences.tabs }
        ordered.forEach { tab ->
            val index = preferences.tabs.indexOf(tab)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(index >= 0, enabled = !busy, onCheckedChange = { visible -> save { it.copy(tabs = if (visible) it.tabs + tab else it.tabs - tab) } })
                Text(if (tab == "Focus") "Timer" else tab, Modifier.weight(1f))
                TextButton(enabled = !busy && index > 1, onClick = { save { current ->
                    val tabs = current.tabs.toMutableList(); val i = tabs.indexOf(tab)
                    if (i > 1) { tabs[i] = tabs[i - 1]; tabs[i - 1] = tab }; current.copy(tabs = tabs)
                } }) { Text("Up") }
            }
        }
        Text("Date & time", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Monday first", Modifier.weight(1f)); Switch(preferences.mondayFirst, enabled = !busy,
                onCheckedChange = { checked -> save { it.copy(mondayFirst = checked) } })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("24-hour time", Modifier.weight(1f)); Switch(preferences.use24Hour, enabled = !busy,
                onCheckedChange = { checked -> save { it.copy(use24Hour = checked) } })
        }
        var time by remember(preferences.defaultReminderTime) { mutableStateOf(preferences.defaultReminderTime.toString()) }
        val parsed = runCatching { LocalTime.parse(time) }.getOrNull()?.takeIf { it.second == 0 && it.nano == 0 }
        OutlinedTextField(time, { time = it }, label = { Text("Date-only reminder time (HH:mm)") }, singleLine = true, isError = parsed == null)
        TextButton(enabled = !busy && parsed != null && parsed != preferences.defaultReminderTime,
            onClick = { parsed?.let { value -> save { it.copy(defaultReminderTime = value) } } }) { Text("Save reminder time") }
        Text("Applies to newly created tasks. Existing reminders keep their saved time.", style = MaterialTheme.typography.bodySmall)
        Text("Default task list", style = MaterialTheme.typography.titleMedium)
        val selectedList = preferences.defaultList.takeIf { id -> lists.any { it.id == id } } ?: "inbox"
        lists.forEach { list ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selectedList == list.id, enabled = !busy, onClick = { save { it.copy(defaultList = list.id) } })
                Text(list.name)
            }
        }
        Text("Used when adding outside a selected list.", style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
