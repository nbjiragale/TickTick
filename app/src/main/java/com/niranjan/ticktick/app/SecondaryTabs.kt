package com.niranjan.ticktick.app

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.feature.tasks.TaskRow
import com.niranjan.ticktick.feature.tasks.TasksUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CalendarContent(state: TasksUiState, onToggle: (Task) -> Unit, onOpen: (Task) -> Unit) {
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(state.today.toEpochDay()) }
    val date = LocalDate.ofEpochDay(selectedEpochDay)
    val tasks = state.snapshot.tasks.filter { it.dueDate == date && (state.showCompleted || !it.completed) }
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { selectedEpochDay-- }) { Text("Previous") }
            Text(date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)), fontWeight = FontWeight.Medium)
            TextButton(onClick = { selectedEpochDay++ }) { Text("Next") }
        }
        if (tasks.isEmpty()) Text("No tasks", Modifier.padding(top = 32.dp), color = TickTickColors.SecondaryText)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { task -> TaskRow(task, state.today, { onToggle(task) }, { onOpen(task) }) }
        }
    }
}

@Composable
fun FocusContent() {
    var remainingMs by rememberSaveable { mutableLongStateOf(25 * 60 * 1000L) }
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    var running by rememberSaveable { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(running) {
        while (running) {
            tick = SystemClock.elapsedRealtime()
            if (tick >= deadline) { remainingMs = 0; running = false }
            delay(250)
        }
    }
    val ms = if (running) (deadline - tick).coerceAtLeast(0) else remainingMs
    val seconds = (ms + 999) / 1000
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(String.format(Locale.ENGLISH, "%02d:%02d", seconds / 60, seconds % 60), fontSize = 52.sp, fontWeight = FontWeight.Light)
        Text(if (ms == 0L) "Session complete" else "Focus time", style = MaterialTheme.typography.bodyMedium, color = TickTickColors.SecondaryText)
        Row {
            TextButton(onClick = {
                if (running) { remainingMs = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0); running = false }
                else {
                    if (remainingMs == 0L) remainingMs = 25 * 60 * 1000L
                    tick = SystemClock.elapsedRealtime(); deadline = tick + remainingMs; running = true
                }
            }) { Text(if (running) "Pause" else "Start") }
            TextButton(onClick = { running = false; remainingMs = 25 * 60 * 1000L }) { Text("Reset") }
        }
    }
}
