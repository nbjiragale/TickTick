package com.niranjan.ticktick.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.core.designsystem.TickTickDimensions
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskFilter

@Composable
fun TasksScreen(state: TasksUiState, onToggleTask: (Task) -> Unit, onOpenTask: (Task) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        if (state.filter == TaskFilter.Today) {
            Text(
                "Today",
                Modifier.padding(start = 16.dp, top = 6.dp)
                    .background(TickTickColors.TodayChip, RoundedCornerShape(20.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                color = TickTickColors.DueTime,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        if (state.tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.Center) {
                Text("No tasks", color = TickTickColors.SecondaryText, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = TickTickDimensions.PageInset, end = TickTickDimensions.PageInset, bottom = 160.dp),
            ) {
                items(state.tasks, key = { it.id }) { task ->
                    TaskRow(task, state.today, { onToggleTask(task) }, { onOpenTask(task) })
                }
            }
        }
    }
}
