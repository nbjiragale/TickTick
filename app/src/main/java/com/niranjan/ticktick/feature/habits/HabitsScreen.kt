package com.niranjan.ticktick.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.repository.HabitRepository
import com.niranjan.ticktick.domain.repository.UiStateRepository
import com.niranjan.ticktick.domain.model.habitsFor
import com.niranjan.ticktick.feature.taskeditor.schedule.*
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import com.niranjan.ticktick.domain.repository.taskWriteResult

@Composable
fun HabitsScreen(sounds: com.niranjan.ticktick.platform.sounds.AppSounds, repository: HabitRepository, clock: Clock, drafts: UiStateRepository, requestedHabitId: String? = null, requestedHabitDay: Long? = null,
    onExternalDetailClosed: () -> Unit = {}, onRequestHandled: () -> Unit = {}, currentDate: LocalDate = LocalDate.now(clock)) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var writeError by remember { mutableStateOf<String?>(null) }
    fun write(action: suspend () -> Unit) {
        if (saving) return
        saving = true
        scope.launch {
            try { taskWriteResult(action).onFailure { writeError = it.message } }
            finally { saving = false }
        }
    }
    var today by remember { mutableStateOf(LocalDate.now(clock)) }
    var selectedDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var weekEnd by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var editorId by rememberSaveable { mutableStateOf(drafts.currentDraft("habit")?.takeIf { it.payload != null }?.let { stored ->
        stored.ownerId.takeIf { id -> snapshot.habits.any { it.id == id } } ?: "new"
    }) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var externalDetail by rememberSaveable { mutableStateOf(false) }
    var detailDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var sectionReturn by rememberSaveable { mutableStateOf<String?>(null) }
    var archivedLibrary by rememberSaveable { mutableStateOf(false) }
    var collapsed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val date = LocalDate.ofEpochDay(selectedDay)
    fun closeDetail() {
        detailId = null
        if (externalDetail) {
            externalDetail = false
            onExternalDetailClosed()
        }
    }
    LaunchedEffect(requestedHabitId) {
        if (requestedHabitId != null) {
            panel = null
            detailId = requestedHabitId
            externalDetail = true
            detailDay = requestedHabitDay ?: today.toEpochDay()
            onRequestHandled()
        }
    }
    LaunchedEffect(currentDate) {
            val now = currentDate
            if (today != now) {
                if (selectedDay == today.toEpochDay()) { selectedDay = now.toEpochDay(); weekEnd = selectedDay }
                today = now
            }
    }
    fun shiftWeek(days: Long) { weekEnd += days; selectedDay += days }
    val visible = snapshot.habitsFor(date)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Habit", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                ScheduleIcon(AppSymbol.HabitStats, "Habit statistics", { panel = "stats" })
                ScheduleIcon(AppSymbol.HabitBook, "Active and archived habits", { panel = "all" })
                ScheduleIcon(AppSymbol.HabitSettings, "Habit settings", { panel = "settings" })
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp).selectableGroup().semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Previous week") { shiftWeek(-7); true },
                    CustomAccessibilityAction("Next week") { shiftWeek(7); true },
                    CustomAccessibilityAction("Today") { selectedDay = today.toEpochDay(); weekEnd = selectedDay; true },
                )
            }.pointerInput(weekEnd) {
                var distance = 0f
                detectHorizontalDragGestures(onDragStart = { distance = 0f }, onDragEnd = {
                    if (distance > 60) shiftWeek(-7) else if (distance < -60) shiftWeek(7)
                }) { change, amount -> change.consume(); distance += amount }
            }) {
                repeat(7) { index ->
                    val day = LocalDate.ofEpochDay(weekEnd - 6 + index)
                    val selected = day == date
                    Column(Modifier.weight(1f).selectable(selected, role = Role.Tab, onClick = { selectedDay = day.toEpochDay() })
                        .semantics { contentDescription = day.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)), Modifier.padding(top = 10.dp, bottom = 13.dp), fontSize = 12.sp, color = Color(0xFF86888D))
                        Box(Modifier.size(34.dp).background(if (selected) HabitBlue else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                            Text(day.dayOfMonth.toString(), fontSize = 15.sp, color = if (selected) Color.White else TickTickColors.Text)
                        }
                    }
                }
            }
            if (visible.isEmpty()) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    HabitAvatar("😊", Modifier.size(62.dp), true)
                    Text(if (snapshot.habits.isEmpty()) "Build a little good into every day" else "No habits scheduled", Modifier.padding(top = 18.dp), fontSize = 18.sp, textAlign = TextAlign.Center)
                    Text(if (snapshot.habits.isEmpty()) "Tap + to create your first habit." else "Choose another date to see your habits.", Modifier.padding(top = 8.dp), color = TickTickColors.SecondaryText, textAlign = TextAlign.Center)
                }
            } else LazyColumn(contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(snapshot.sections.filter { section -> visible.any { it.section == section } }, key = { it }) { section ->
                    val habits = visible.filter { it.section == section }
                    HabitCard {
                        Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(role = Role.Button) {
                            collapsed = if (section in collapsed) collapsed - section else collapsed + section
                        }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(section, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(habits.size.toString(), color = HabitMuted, fontSize = 13.sp)
                            AppIcon(AppSymbol.Chevron, Modifier.padding(start = 8.dp).size(12.dp).rotate(if (section in collapsed) 0f else 90f), HabitMuted)
                        }
                        if (section !in collapsed) habits.forEach { habit ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).clickable(role = Role.Button) { detailId = habit.id; detailDay = selectedDay }
                                .padding(start = 15.dp, end = 17.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                HabitAvatar(habit.icon)
                                Text(habit.name, Modifier.padding(start = 8.dp).weight(1f), fontSize = 16.sp)
                                Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End) {
                                    Text((snapshot.completedDates[habit.id]?.count { it <= date } ?: 0).toString(), fontSize = 15.sp)
                                    Text("Total Day", fontSize = 12.sp, color = HabitMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 8.dp).size(56.dp)
            .shadow(8.dp, CircleShape, spotColor = HabitBlue.copy(alpha = .3f)).clip(CircleShape).background(HabitBlue)
            .clickable(role = Role.Button) { editorId = "new" }.semantics { contentDescription = "Add habit" }, contentAlignment = Alignment.Center) {
            AppIcon(AppSymbol.Plus, Modifier.size(44.dp), Color.White)
        }
    }
    when (panel) {
        "all" -> HabitLibrary(snapshot, archivedLibrary, { archivedLibrary = it }, { panel = null },
            { detailId = it.id; detailDay = today.toEpochDay() }, { editorId = it.id }, { editorId = "new" }, { id, archived -> write { repository.setArchived(id, archived) } })
        "settings" -> HabitSettingsScreen(sounds, snapshot.settings, { settings -> write { repository.updateSettings(settings) } },
            { sectionReturn = "settings"; panel = "sections" }, { panel = null })
        "stats" -> ScheduleDialog("Habit statistics", { panel = null }) {
            Text("${snapshot.habits.size} habits", Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            Text("${snapshot.completedDates.values.sumOf { it.size }} total completed days", Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            ScheduleTextButton("Done", { panel = null }, Modifier.align(Alignment.End).padding(end = 16.dp))
        }
    }
    snapshot.habits.find { it.id == detailId }?.let { habit -> key(habit.id, detailDay) {
        HabitCheckInScreen(habit, LocalDate.ofEpochDay(detailDay), today, snapshot,
            onBack = ::closeDetail, onEdit = { editorId = habit.id },
            onArchive = { write { repository.setArchived(habit.id, !habit.archived); closeDetail() } },
            onDelete = { write { repository.delete(habit.id); closeDetail() } },
            onProgress = { amount -> taskWriteResult { repository.setProgress(habit.id, LocalDate.ofEpochDay(detailDay), amount, habit.revision) } })
    } }
    if (editorId != null) key(editorId) {
        HabitEditor(snapshot.habits.find { it.id == editorId }, today, snapshot.sections, drafts,
            onDismiss = { editorId = null }, onSave = { habit, token -> taskWriteResult {
                drafts.flushDrafts()
                repository.save(habit, token)
                var first = maxOf(today, habit.startDate)
                while (!habit.isScheduledOn(first)) first = first.plusDays(1)
                selectedDay = first.toEpochDay(); weekEnd = maxOf(today.toEpochDay(), selectedDay)
                collapsed = collapsed - habit.section
                editorId = null
            } }, onManageSections = { sectionReturn = panel; panel = "sections" })
    }
    writeError?.let { message -> AlertDialog(onDismissRequest = { writeError = null },
        text = { Text(message) }, confirmButton = { TextButton(onClick = { writeError = null }) { Text("OK") } }) }
    if (panel == "sections") HabitSections(snapshot.sections, { name -> taskWriteResult { repository.addSection(name) } }, { name, direction -> write { repository.moveSection(name, direction) } }) { panel = sectionReturn; sectionReturn = null }
}
