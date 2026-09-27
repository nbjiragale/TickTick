package com.niranjan.ticktick.feature.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.feature.organization.AddListOrTagScreen
import com.niranjan.ticktick.feature.taskeditor.schedule.SchedulePicker
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Composable
fun TaskSelectionToolbar(enabled: Boolean, onAction: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically) {
        listOf(Triple("date", "Date", AppSymbol.Date), Triple("section", "Move to another section", AppSymbol.SectionMove),
            Triple("move", "Move to", AppSymbol.Move), Triple("delete", "Delete", AppSymbol.Trash),
            Triple("more", "More actions", AppSymbol.More)).forEach { (key, label, icon) ->
            IconButton(onClick = { onAction(key) }, enabled = enabled,
                modifier = Modifier.weight(1f).height(56.dp).semantics { contentDescription = label }) {
                AppIcon(icon, Modifier.size(23.dp), if (enabled) TickTickColors.Text else TickTickColors.SecondaryText)
            }
        }
    }
}

@Composable
fun TaskSelectionOverlays(panel: String?, onPanel: (String?) -> Unit, selection: TaskSelectionState,
    state: TasksUiState, vm: TaskSelectionViewModel) {
    val dismiss = { onPanel(null) }
    fun apply(action: () -> Unit) { action() }
    if (!selection.active || selection.tasks.isEmpty()) return
    when (panel) {
        "date" -> SelectionCard("Date", dismiss) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    DateChoice("Today", AppSymbol.PlanToday, Modifier.weight(1f), day = state.today.dayOfMonth.toString()) { apply { vm.setDate(state.today) } }
                    DateChoice("Tomorrow", AppSymbol.Sunrise, Modifier.weight(1f)) { apply { vm.setDate(state.today.plusDays(1)) } }
                    DateChoice("Next Monday", AppSymbol.PlanToday, Modifier.weight(1f), day = "MO") {
                        apply { vm.setDate(state.today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))) }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    DateChoice("Pick Date", AppSymbol.Date, Modifier.weight(1f)) { onPanel("pickDate") }
                    DateChoice("Skip the\nRecurrence", AppSymbol.Move, Modifier.weight(1f), enabled = selection.tasks.all { it.isActive && !it.isNote && it.recurrence != null }) { vm.skipRecurrence() }
                    DateChoice("Clear", AppSymbol.DateClear, Modifier.weight(1f)) { apply { vm.setDate(null) } }
                }
            }
        }
        "pickDate" -> SchedulePicker(selection.tasks.first().schedule, vm.clock, { onPanel("date") },
            { schedule -> apply { vm.setSchedule(schedule) } }, animateEntrance = true)
        "section" -> SelectionCard("Move to Another Section", dismiss) {
            Column(Modifier.padding(24.dp)) {
                Text("Move to Another Section", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                when (state.presentation.grouping) {
                    TaskGrouping.List -> state.snapshot.lists.forEach { list -> SectionChoice(list.name) { apply { vm.move(list.id) } } }
                    TaskGrouping.Priority -> TaskPriority.entries.forEach { priority ->
                        SectionChoice(if (priority == TaskPriority.None) "No Priority" else "${priority.name} Priority") { apply { vm.priority(priority) } }
                    }
                    TaskGrouping.Tag -> {
                        state.snapshot.knownTags.forEach { tag -> SectionChoice("#${tag.name}") {
                            apply { vm.tags(state.snapshot.knownTags.associate { it.name to it.name.equals(tag.name, true) }, "") }
                        } }
                        SectionChoice("No Tag") { apply { vm.tags(state.snapshot.knownTags.associate { it.name to false }, "") } }
                    }
                    TaskGrouping.Created -> Text("Created Time sections are assigned from each task's creation date.", Modifier.padding(vertical = 12.dp))
                    else -> {
                        SectionChoice("Overdue") { apply { vm.setDate(state.today.minusDays(1)) } }
                        SectionChoice("Today") { apply { vm.setDate(state.today) } }
                        state.groups.filter { it.key != "overdue" && it.key != state.today.toString() && it.key != "none" && it.key != "all" }
                            .forEach { group ->
                                group.tasks.firstOrNull()?.dueDate?.let { date ->
                                    SectionChoice(group.label) { apply { vm.setDate(date) } }
                                }
                            }
                    }
                }
                TextButton(onClick = dismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
        "move" -> MoveTasksSheet(state.snapshot, selection.tasks, dismiss, { apply { vm.move(it) } }, vm::createList)
        "more" -> SelectionCard("More actions", dismiss, more = true) {
            Column(Modifier.padding(vertical = 10.dp)) {
                MoreChoice("Done", AppSymbol.PlanDone) { apply(vm::done) }
                MoreChoice(if (selection.tasks.all { it.pinned }) "Unpin" else "Pin", AppSymbol.Pin) { apply(vm::pin) }
                MoreChoice("Set Priority", AppSymbol.Flag) { onPanel("priority") }
                MoreChoice("Tags", AppSymbol.Tag) { onPanel("tags") }
                MoreChoice("Duplicate", AppSymbol.Duplicate) { apply(vm::duplicate) }
                MoreChoice(if (selection.tasks.all { it.isNote }) "Convert to Task" else "Convert to Note", AppSymbol.Note) { apply(vm::convert) }
            }
        }
        "priority" -> SelectionCard("Set Priority", dismiss) {
            Column(Modifier.padding(20.dp)) {
                Text("Set Priority", style = MaterialTheme.typography.titleMedium)
                TaskPriority.entries.forEach { priority -> MoreChoice(if (priority == TaskPriority.None) "No Priority" else "${priority.name} Priority", AppSymbol.Flag) {
                    apply { vm.priority(priority) }
                } }
                TextButton(onClick = dismiss, Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
        "tags" -> SelectionTags(state.snapshot, selection.tasks, dismiss) { changes, additional ->
            vm.tags(changes, additional)
        }
    }
}

@Composable
private fun SelectionCard(title: String, onDismiss: () -> Unit, more: Boolean = false, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(if (more) 0f else .32f) }
        Box(Modifier.fillMaxSize().imePadding()) {
            Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = onDismiss))
            val transition = remember { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(transition, modifier = Modifier.align(if (more) Alignment.BottomEnd else Alignment.Center)
                .padding(start = 29.dp, end = if (more) 16.dp else 29.dp, top = 16.dp, bottom = if (more) 72.dp else 16.dp),
                enter = slideInVertically(tween(260)) { it } + fadeIn(tween(180))) {
                Surface(Modifier.widthIn(max = if (more) 210.dp else 360.dp).fillMaxWidth()
                    .semantics { paneTitle = title }, shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 12.dp) {
                    Column(Modifier.verticalScroll(rememberScrollState())) { content() }
                }
            }
        }
    }
}

@Composable
private fun DateChoice(label: String, icon: AppSymbol, modifier: Modifier, enabled: Boolean = true, day: String = "", onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(vertical = 8.dp).heightIn(min = 74.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(icon, Modifier.size(32.dp), if (enabled) TickTickColors.DueTime else Color(0xFFB9B9B9), day)
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TickTickColors.SecondaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun SectionChoice(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = false, onClick = null, Modifier.size(24.dp))
        Text(label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MoreChoice(label: String, symbol: AppSymbol, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 44.dp).padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(symbol, Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveTasksSheet(snapshot: TaskSnapshot, tasks: List<Task>, onDismiss: () -> Unit, onMove: (String) -> Unit,
    onCreate: suspend (String, Int?, TaskListView) -> Result<Unit>) {
    var query by rememberSaveable { mutableStateOf("") }
    var adding by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF7F7F7), dragHandle = null, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.62f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = "Cancel move" }) { AppIcon(AppSymbol.Close) }
                Text("Move to", style = MaterialTheme.typography.titleLarge)
            }
            TextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 10.dp), placeholder = { Text("Search") }, singleLine = true,
                shape = RoundedCornerShape(10.dp), colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF0F0F0), unfocusedContainerColor = Color(0xFFF0F0F0)))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 4.dp)) {
                val lists = snapshot.lists.filter { it.name.contains(query.trim(), true) }
                if (lists.isEmpty()) Text("No lists found", Modifier.padding(20.dp), color = TickTickColors.SecondaryText)
                lists.forEach { list ->
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onMove(list.id) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val symbol = when (list.symbol) {
                            ListSymbol.Inbox -> AppSymbol.InboxOutline
                            ListSymbol.Work -> AppSymbol.Work
                            ListSymbol.Personal -> AppSymbol.Home
                            ListSymbol.Welcome -> AppSymbol.Habits
                            ListSymbol.Custom -> AppSymbol.List
                        }
                        AppIcon(symbol, Modifier.size(22.dp), list.color?.let { Color(it) } ?: TickTickColors.SecondaryText)
                        Text(list.name, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        if (tasks.all { it.listId == list.id }) AppIcon(AppSymbol.Check, Modifier.size(20.dp), TickTickColors.DueTime)
                    }
                }
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { adding = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppSymbol.Plus, Modifier.size(22.dp), TickTickColors.DueTime)
                    Text("Add List", Modifier.padding(start = 12.dp), color = TickTickColors.DueTime, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    if (adding) AddListOrTagScreen(false, { adding = false }) { name, color, view ->
        onCreate(name, color, view).onSuccess { query = "" }
    }
}

@Composable
private fun SelectionTags(snapshot: TaskSnapshot, tasks: List<Task>, onDismiss: () -> Unit, onApply: (Map<String, Boolean>, String) -> Unit) {
    var changes by rememberSaveable { mutableStateOf(hashMapOf<String, Boolean>()) }
    var additional by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    SelectionCard("Tags", onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("Tags", style = MaterialTheme.typography.titleMedium)
            snapshot.knownTags.forEach { tag ->
                val count = tasks.count { task -> task.tags.any { it.equals(tag.name, true) } }
                val value = changes[tag.name]?.let { ToggleableState(it) } ?: when (count) {
                    0 -> ToggleableState.Off
                    tasks.size -> ToggleableState.On
                    else -> ToggleableState.Indeterminate
                }
                Row(Modifier.fillMaxWidth().clickable { changes = HashMap(changes).apply { put(tag.name, value != ToggleableState.On) } },
                    verticalAlignment = Alignment.CenterVertically) {
                    TriStateCheckbox(value, onClick = null)
                    Text("#${tag.name}", Modifier.padding(start = 8.dp))
                }
            }
            OutlinedTextField(additional, { additional = it; error = null }, label = { Text("Add tags") },
                supportingText = { Text(error ?: "Separate tags with spaces or commas.") }, isError = error != null)
            Row(Modifier.align(Alignment.End)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { onApply(changes, additional) }) { Text("Apply") }
            }
        }
    }
}
