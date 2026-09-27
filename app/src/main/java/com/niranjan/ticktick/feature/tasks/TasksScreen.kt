package com.niranjan.ticktick.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.core.designsystem.TickTickDimensions
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskFilter
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.HabitSnapshot
import java.time.LocalDate

@Composable
fun TasksScreen(state: TasksUiState, onToggleTask: (Task) -> Unit, onOpenTask: (Task) -> Unit,
    onHomeSection: (HomeTaskSection) -> Unit, habits: HabitSnapshot, onOpenHabit: (Habit, LocalDate) -> Unit,
    modifier: Modifier = Modifier, onEditDate: (Task) -> Unit = onOpenTask,
    selecting: Boolean = false, selectedIds: Set<String> = emptySet(), onLongPress: (Task) -> Unit = {}) {
    val habitOccurrences = listHabitOccurrences(state, habits)
    Column(modifier.fillMaxSize()) {
        if (state.filter == TaskFilter.Today) {
            Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup()
                .padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeTaskSection.entries.forEach { section ->
                    val selected = section == state.homeSection
                    Box(Modifier.height(48.dp).selectable(selected, role = Role.Tab, onClick = { onHomeSection(section) }),
                        contentAlignment = Alignment.Center) {
                        Text(section.label, Modifier.background(if (selected) TickTickColors.TodayChip else Color.Transparent, RoundedCornerShape(20.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                            color = if (selected) TickTickColors.DueTime else TickTickColors.SecondaryText,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
        key(state.filter, if (state.filter == TaskFilter.Today) state.homeSection else null) {
        if (state.tasks.isEmpty() && habitOccurrences.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.Center) {
                Text(if (state.filter == TaskFilter.Today) when (state.homeSection) {
                    HomeTaskSection.Overdue -> "No overdue tasks"
                    HomeTaskSection.Today -> if (state.presentation.layout == TaskLayout.List && habits.settings.showInTodayAndNext7Days) "No tasks or habits today" else "No tasks today"
                    HomeTaskSection.Upcoming -> if (state.presentation.layout == TaskLayout.List && habits.settings.showInTodayAndNext7Days) "No upcoming tasks or habits" else "No upcoming tasks"
                } else "No tasks", color = TickTickColors.SecondaryText, style = MaterialTheme.typography.bodyMedium)
            }
        } else if (state.presentation.layout == TaskLayout.Kanban) {
            val width = (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp - 52.dp).coerceIn(180.dp, 360.dp)
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 22.dp),
            ) {
                items(state.groups, key = { it.key }) { group ->
                    Column(Modifier.width(width).fillParentMaxHeight()) {
                        GroupHeading(group)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 160.dp)) {
                            items(group.tasks, key = { it.id }) { task ->
                                PresentationTaskRow(task, state, onToggleTask, onOpenTask, onEditDate,
                                    selecting = selecting, selected = task.id in selectedIds, onLongPress = onLongPress)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 160.dp)) {
                state.groups.forEach { group ->
                    item(key = "header:${group.key}") {
                        if (state.presentation.grouping != TaskGrouping.None) {
                            GroupHeading(group, Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Color.White))
                        }
                    }
                    itemsIndexed(group.tasks, key = { _, task -> "${group.key}:${task.id}" }) { index, task ->
                        val top = if (index == 0 && state.presentation.grouping == TaskGrouping.None) 16.dp else 0.dp
                        val bottom = if (index == group.tasks.lastIndex) 16.dp else 0.dp
                        PresentationTaskRow(task, state, onToggleTask, onOpenTask, onEditDate,
                            Modifier.clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)), grouped = true,
                            selecting = selecting, selected = task.id in selectedIds, onLongPress = onLongPress)
                    }
                    item(key = "space:${group.key}") { Spacer(Modifier.height(10.dp)) }
                }
                if (!selecting) taskListHabits(habitOccurrences, state.today, onOpenHabit)
            }
        }
        }
    }
}

@Composable
private fun GroupHeading(group: TaskGroup, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(group.label, Modifier.weight(1f), color = TickTickColors.DueTime, style = MaterialTheme.typography.titleMedium)
        Text(group.tasks.size.toString(), color = TickTickColors.SecondaryText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PresentationTaskRow(task: Task, state: TasksUiState, onToggle: (Task) -> Unit, onOpen: (Task) -> Unit,
    onEditDate: (Task) -> Unit, modifier: Modifier = Modifier, grouped: Boolean = false,
    selecting: Boolean = false, selected: Boolean = false, onLongPress: (Task) -> Unit = {}) {
    val list = state.snapshot.lists.find { it.id == task.listId }
    val label = when (list?.symbol) {
        com.niranjan.ticktick.domain.model.ListSymbol.Work -> "💼 ${list.name}"
        else -> list?.name.orEmpty()
    }
    TaskRow(task, state.today, { onToggle(task) }, { onOpen(task) }, modifier, grouped,
        onDateClick = if (selecting) null else { { onEditDate(task) } }, showDetails = state.presentation.details, listLabel = label,
        selecting = selecting, selected = selected, onLongPress = { onLongPress(task) })
}
