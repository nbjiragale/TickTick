package com.niranjan.ticktick.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.HabitFrequency
import com.niranjan.ticktick.domain.model.encodeHabit
import com.niranjan.ticktick.domain.model.decodeHabit
import com.niranjan.ticktick.domain.repository.UiStateRepository
import com.niranjan.ticktick.domain.repository.DraftToken
import org.json.JSONObject
import com.niranjan.ticktick.feature.taskeditor.schedule.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

private val quotes = listOf("Whatever you do, do it well.", "Small steps, every day.", "Make time for what matters.", "A little progress is still progress.")
private val weekDays = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

@Composable
internal fun HabitEditor(initial: Habit?, today: LocalDate, sections: List<String>, drafts: UiStateRepository, onDismiss: () -> Unit, onSave: suspend (Habit, DraftToken) -> Result<Unit>, onManageSections: () -> Unit) {
    val recovered = remember { drafts.currentDraft("habit")?.takeIf { it.payload != null } }
    val recovery = remember { runCatching { recovered?.payload?.let { payload ->
        JSONObject(payload).also { require(it.getInt("version") == 1); it.getString("habit").decodeHabit() }
    } } }
    if (recovery.isFailure) {
        AlertDialog(onDismissRequest = {}, title = { Text("Draft recovery") },
            text = { Text("This habit draft couldn't be read. It has been kept in storage; you can discard it to start again.") },
            confirmButton = { TextButton(onClick = {
                recovered?.let { drafts.stageDraft("habit", it.token.sessionId, it.ownerId, it.baseRevision, null) }
                onDismiss()
            }) { Text("Discard draft") } })
        return
    }
    val recoveredJson = recovery.getOrNull()
    val session = remember { recovered?.token?.sessionId ?: UUID.randomUUID().toString() }
    var draft by rememberSaveable { mutableStateOf(recoveredJson?.getString("habit")?.decodeHabit() ?: initial ?: Habit(UUID.randomUUID().toString(), "", startDate = today)) }
    var step by rememberSaveable { mutableIntStateOf(recoveredJson?.optInt("step", 0) ?: 0) }
    val persistence = remember { HabitDraftPersistence(drafts, session) }
    SideEffect { persistence.stage(draft, step) }
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    fun back() { if (saving) return; if (step == 1) step = 0 else { persistence.clear(draft); onDismiss() } }
    Dialog(onDismissRequest = ::back, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.let {
            it.setDimAmount(0f)
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightNavigationBars = true
        } }
        Column(Modifier.fillMaxSize().background(TickTickColors.Background).systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ScheduleIcon(AppSymbol.Back, if (step == 0) "Close habit editor" else "Back to habit details", ::back)
                Text(if (initial == null) "New Habit" else "Edit Habit", Modifier.padding(start = 4.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            key(step) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (step == 0) {
                        HabitCard {
                            Text("Name", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp), fontSize = 16.sp)
                            HabitInput(draft.name, { draft = draft.copy(name = it.take(100)) }, "Daily Check-in", Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth())
                        }
                        HabitCard {
                            Text("Icon", Modifier.padding(start = 16.dp, top = 12.dp, bottom = 14.dp), fontSize = 16.sp)
                            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                HabitAvatar(if (draft.icon in habitIcons) draft.icon else "😊", Modifier.size(54.dp)
                                    .clickable(role = Role.Button) { draft = draft.copy(icon = "😊") }.semantics { contentDescription = "Use smile icon" }, draft.icon in habitIcons)
                                Spacer(Modifier.width(14.dp))
                                HabitAvatar(if (draft.icon in habitIcons) "A" else draft.icon, Modifier.size(48.dp)
                                    .clickable(role = Role.Button) { panel = "letter" }.semantics { contentDescription = "Choose letter avatar" }, draft.icon !in habitIcons)
                            }
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                habitIcons.chunked(8).forEach { icons ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        icons.forEach { icon ->
                                            Box(Modifier.size(38.dp).selectable(draft.icon == icon, role = Role.RadioButton, onClick = { draft = draft.copy(icon = icon) })
                                                .semantics { contentDescription = "Habit icon $icon" }) {
                                                HabitAvatar(icon)
                                                if (draft.icon == icon) Box(Modifier.align(Alignment.BottomEnd).size(14.dp).background(HabitBlue, CircleShape), contentAlignment = Alignment.Center) {
                                                    AppIcon(AppSymbol.Check, Modifier.size(10.dp), Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HabitCard {
                            Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Quote", Modifier.weight(1f), fontSize = 16.sp)
                                ScheduleIcon(AppSymbol.Repeat, "Refresh quote", { draft = draft.copy(quote = quotes[(quotes.indexOf(draft.quote) + 1) % quotes.size]) }, tint = ScheduleLabelBlue)
                            }
                            HabitInput(draft.quote, { draft = draft.copy(quote = it.take(240)) }, "Add a quote", Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth())
                        }
                    } else {
                        HabitCard {
                            Text("Frequency", Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp), fontSize = 16.sp)
                            Row(Modifier.padding(horizontal = 16.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                HabitFrequency.entries.forEach { frequency ->
                                    val active = draft.frequency == frequency
                                    Column(Modifier.width(IntrinsicSize.Max).height(43.dp).selectable(active, role = Role.Tab, onClick = {
                                        draft = draft.copy(frequency = frequency, frequencyCount = if (frequency == HabitFrequency.Weekly) draft.frequencyCount.coerceAtMost(7) else draft.frequencyCount)
                                    }), verticalArrangement = Arrangement.Bottom) {
                                        Text(frequency.name.uppercase(Locale.ENGLISH), fontSize = 14.sp, color = if (active) ScheduleLabelBlue else HabitMuted, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(6.dp))
                                        Box(Modifier.fillMaxWidth().height(3.dp).background(if (active) ScheduleBlue else Color.Transparent, RoundedCornerShape(2.dp)))
                                    }
                                }
                            }
                            if (draft.frequency == HabitFrequency.Daily) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    weekDays.forEach { day ->
                                        val selected = day in draft.weekdays
                                        Box(Modifier.weight(1f).height(44.dp).toggleable(selected, role = Role.Checkbox, onValueChange = { checked ->
                                            error = null
                                            if (checked) draft = draft.copy(weekdays = draft.weekdays + day)
                                            else if (draft.weekdays.size > 1) draft = draft.copy(weekdays = draft.weekdays - day)
                                            else error = "Keep at least one weekday selected."
                                        }).semantics { contentDescription = day.name.lowercase().replaceFirstChar(Char::uppercase) }, contentAlignment = Alignment.Center) {
                                            Box(Modifier.size(36.dp).background(if (selected) HabitBlue else HabitField, CircleShape), contentAlignment = Alignment.Center) {
                                                Text(day.name.take(1), color = if (selected) Color.White else HabitMuted, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            } else Row(Modifier.fillMaxWidth().height(232.dp).padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                if (draft.frequency == HabitFrequency.Interval) Text("Every", fontSize = 20.sp)
                                key(draft.frequency) {
                                    HabitNumberWheel(draft.frequencyCount, if (draft.frequency == HabitFrequency.Weekly) 7 else 30, Modifier.width(96.dp)) { draft = draft.copy(frequencyCount = it) }
                                }
                                Text(if (draft.frequency == HabitFrequency.Weekly) "days per week" else "days", fontSize = if (draft.frequency == HabitFrequency.Weekly) 16.sp else 20.sp)
                            }
                        }
                        HabitCard {
                            HabitSetting("Goal", draft.amount?.let { "$it ${draft.unit}" } ?: "Achieve it all") { panel = "goal" }
                            HabitSetting("Start Date", draft.startDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))) { panel = "date" }
                            HabitSetting("Goal Days  ⓘ", draft.goalDays?.let { "$it days" } ?: "Forever") { panel = "days" }
                        }
                        HabitCard {
                            val sectionScroll = rememberScrollState()
                            LaunchedEffect(sectionScroll.maxValue, draft.section) {
                                if (draft.section == sections.lastOrNull()) sectionScroll.scrollTo(sectionScroll.maxValue)
                            }
                            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Section", Modifier.weight(1f), fontSize = 16.sp)
                                ScheduleIcon(AppSymbol.Plus, "Manage habit sections", onManageSections, tint = Color(0xFFCCCCCC))
                            }
                            Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp).horizontalScroll(sectionScroll).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                sections.forEach { section ->
                                    Box(Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(8.dp)).background(if (draft.section == section) HabitBlue else Color(0xFFF1F1F1))
                                        .selectable(draft.section == section, role = Role.RadioButton, onClick = { draft = draft.copy(section = section) }).padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(section, color = if (draft.section == section) Color.White else TickTickColors.Text, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                        HabitReminderCard(
                            reminders = draft.reminders,
                            constantReminder = draft.constantReminder,
                            onEdit = { time -> panel = "time:${time.toSecondOfDay()}" },
                            onAdd = { panel = "time:new" },
                            onConstantChange = { draft = draft.copy(constantReminder = it) },
                        )
                        HabitCard {
                            Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Auto pop-up of habit log", Modifier.weight(1f), fontSize = 16.sp)
                                ReferenceSwitch(draft.autoPopUp, "Auto pop-up of habit log") { draft = draft.copy(autoPopUp = it) }
                            }
                        }
                        if (initial != null) {
                            val effective = if (initial.frequency == HabitFrequency.Weekly || draft.frequency == HabitFrequency.Weekly)
                                today.minusDays((today.dayOfWeek.value - 1).toLong()).plusWeeks(1) else today.plusDays(1)
                            Text("Schedule and goal changes apply from ${effective.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))}. Earlier check-ins keep their original targets.",
                                color = HabitMuted, fontSize = 12.sp)
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            Button(onClick = {
                focus.clearFocus(); keyboard?.hide()
                if (step == 0) { step = 1; error = null }
                else if (!saving) {
                    saving = true
                    scope.launch {
                        try {
                            val token = persistence.stage(draft, step)
                            onSave(draft.copy(name = draft.name.trim(), quote = draft.quote.trim()), token)
                                .onSuccess { persistence.clear(draft) }.onFailure { error = it.message }
                        }
                        finally { saving = false }
                    }
                }
            }, enabled = draft.name.isNotBlank() && !saving, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp).height(44.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = HabitBlue)) {
                Text(if (step == 0) "Next" else "Save", fontSize = 16.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
    when (panel) {
        "letter" -> HabitLetterDialog(if (draft.icon in habitIcons) "A" else draft.icon, { panel = null }) { draft = draft.copy(icon = it); panel = null }
        "goal" -> HabitGoalDialog(draft, { panel = null }) { draft = it; panel = null }
        "days" -> HabitGoalDaysDialog(draft.goalDays, { panel = null }) { draft = draft.copy(goalDays = it); panel = null }
        "date" -> HabitDateDialog(draft.startDate, today, { panel = null }) { draft = draft.copy(startDate = it); panel = null }
    }
    if (panel?.startsWith("time:") == true) {
        val original = panel?.substringAfter(':')?.toIntOrNull()?.let { LocalTime.ofSecondOfDay(it.toLong()) }
        ReferenceTimePicker(original ?: LocalTime.now(), { panel = null }, { time ->
            draft = draft.copy(reminders = (draft.reminders.filter { it != original } + time).distinct().sorted()); panel = null
        }, onClear = original?.let { time -> {
            val remaining = draft.reminders - time
            draft = draft.copy(reminders = remaining, constantReminder = draft.constantReminder && remaining.isNotEmpty())
            panel = null
        } })
    }
    if (saving) AlertDialog(onDismissRequest = {}, text = { Text("Saving habit…") }, confirmButton = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false))
}

private class HabitDraftPersistence(private val repository: UiStateRepository, private val session: String) {
    private var last: String? = null
    private var token: DraftToken? = null
    private var closed = false
    fun stage(habit: Habit, step: Int): DraftToken {
        val payload = JSONObject().put("version", 1).put("habit", habit.encodeHabit()).put("step", step).toString()
        if (!closed && (last != payload || token == null)) {
            token = repository.stageDraft("habit", session, habit.id, habit.revision, payload, newSession = token == null)
            last = payload
        }
        return requireNotNull(token)
    }
    fun clear(habit: Habit) {
        closed = true
        repository.stageDraft("habit", session, habit.id, habit.revision, null)
    }
}
