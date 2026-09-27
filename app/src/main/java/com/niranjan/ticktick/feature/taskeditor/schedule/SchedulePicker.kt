package com.niranjan.ticktick.feature.taskeditor.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.domain.model.*
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private enum class ScheduleOverlay { Time, Reminder, Repeat, DurationDates, StartTime, EndTime }

@Composable
internal fun SchedulePicker(initial: TaskSchedule, clock: Clock, onCancel: () -> Unit, onApply: (TaskSchedule) -> Unit, centered: Boolean = false, animateEntrance: Boolean = false) {
    val today = LocalDate.now(clock)
    var draft by rememberSaveable { mutableStateOf(initial.copy(date = initial.date ?: today)) }
    var durationMode by rememberSaveable { mutableStateOf(initial.duration != null) }
    var range by rememberSaveable { mutableStateOf(initial.duration) }
    var overlay by rememberSaveable { mutableStateOf<ScheduleOverlay?>(null) }
    var customRepeat by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun normalized(): TaskSchedule = if (durationMode && range != null) {
        val current = range!!
        draft.copy(date = current.startDate, time = current.startTime.takeUnless { current.allDay }, duration = current)
    } else draft.copy(duration = null)
    fun confirm() {
        val result = normalized()
        error = result.validationError(clock, requireFutureReminders = result != initial)
        if (error == null) onApply(result)
    }
    fun cancelCustom() { customRepeat = false; overlay = ScheduleOverlay.Repeat }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let {
                it.setDimAmount(if (customRepeat) 0f else .32f)
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightNavigationBars = true
            }
        }
        BackHandler { if (customRepeat) cancelCustom() else onCancel() }
        if (customRepeat) {
            CustomRepeatScreen(draft.repeat, normalized().date ?: today, today, ::cancelCustom) {
                draft = draft.copy(repeat = it); customRepeat = false
            }
        } else {
        var entered by remember { mutableStateOf(!animateEntrance) }
        LaunchedEffect(Unit) { entered = true }
        val entrance by animateFloatAsState(if (entered) 1f else 0f, tween(260), label = "Date sheet entrance")
        BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer { translationY = size.height * (1f - entrance) }
            .then(if (centered) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onCancel() } })
            val container = if (centered) Modifier.align(Alignment.Center).padding(horizontal = 29.dp, vertical = 16.dp)
                .widthIn(max = 360.dp).fillMaxWidth().heightIn(max = (maxHeight - 32.dp).coerceAtLeast(0.dp))
                .clip(RoundedCornerShape(20.dp)).background(ScheduleBackground)
            else Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.74f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(ScheduleBackground).navigationBarsPadding()
            Column(container.then(if (centered) Modifier.pointerInput(Unit) { detectTapGestures { } } else Modifier)) {
                Row(Modifier.fillMaxWidth().height(if (centered) 48.dp else 58.dp).padding(horizontal = if (centered) 10.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!centered) ScheduleIcon(AppSymbol.Close, "Cancel date changes", onCancel)
                    listOf("Date", "Duration").forEachIndexed { index, title ->
                        val selected = (index == 1) == durationMode
                        Column(Modifier.padding(horizontal = 5.dp).clickable {
                            if (durationMode && index == 0 && range != null) {
                                draft = draft.copy(date = range!!.startDate, time = range!!.startTime.takeUnless { range!!.allDay })
                            }
                            if (!durationMode && index == 1 && range != null) {
                                val current = range!!
                                val start = draft.date ?: today
                                val length = ChronoUnit.DAYS.between(current.startDate, current.endDate)
                                range = current.copy(startDate = start, endDate = start.plusDays(length), startTime = draft.time ?: current.startTime)
                            }
                            durationMode = index == 1
                            if (durationMode && range == null) {
                                val date = draft.date ?: today
                                val start = draft.time ?: LocalTime.of(10, 0)
                                val end = date.atTime(start).plusHours(1)
                                range = TaskDuration(date, end.toLocalDate(), start, end.toLocalTime())
                            }
                            error = null
                        }.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(title, Modifier.padding(top = 10.dp, bottom = 6.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (selected) ScheduleBlue else ScheduleMuted)
                            Box(Modifier.height(3.dp).width(if (index == 0) 35.dp else 64.dp).background(if (selected) ScheduleBlue else Color.Transparent, RoundedCornerShape(2.dp)))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (!centered) ScheduleIcon(AppSymbol.Check, "Apply schedule", ::confirm)
                }
                Column(Modifier.weight(1f, fill = !centered).verticalScroll(rememberScrollState())) {
                    if (durationMode && range != null) {
                        DurationContent(range!!, today, { overlay = ScheduleOverlay.DurationDates }, { overlay = ScheduleOverlay.StartTime }, { range = range!!.copy(allDay = it) })
                    } else MonthCalendar(setOfNotNull(draft.date), today, { draft = draft.copy(date = it); error = null })
                    Spacer(Modifier.height(if (durationMode) 12.dp else 8.dp))
                    Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)) {
                        if (!durationMode) ScheduleOption(AppSymbol.Clock, "Time", draft.time?.label() ?: "None", { overlay = ScheduleOverlay.Time }, draft.time != null,
                            onClear = if (centered && draft.time != null) ({ draft = draft.copy(time = null); error = null }) else null, mutedValue = centered)
                        ScheduleOption(AppSymbol.Alarm, "Reminder", reminderSummary(draft.reminders, normalized().time != null), { overlay = ScheduleOverlay.Reminder }, draft.reminders.offsetsMinutes.isNotEmpty(),
                            onClear = if (centered && draft.reminders != ReminderConfig()) ({ draft = draft.copy(reminders = ReminderConfig()); error = null }) else null, mutedValue = centered)
                        ScheduleOption(AppSymbol.Repeat, "Repeat", repeatSummary(draft.repeat), { overlay = ScheduleOverlay.Repeat }, draft.repeat != RepeatRule(), mutedValue = centered)
                    }
                    error?.let { Text(it, Modifier.padding(20.dp), fontSize = 14.sp, color = ScheduleRed) }
                    Spacer(Modifier.height(16.dp))
                }
                if (centered) Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScheduleTextButton("Clear", { draft = TaskSchedule(); durationMode = false; range = null; error = null })
                    Spacer(Modifier.weight(1f))
                    ScheduleTextButton("Cancel", onCancel)
                    ScheduleTextButton("OK", ::confirm)
                } else ScheduleTextButton("Clear", { onApply(TaskSchedule()) }, Modifier.fillMaxWidth().padding(bottom = 8.dp), ScheduleRed)
            }
        }
        }
        when (overlay) {
            ScheduleOverlay.Time, ScheduleOverlay.StartTime, ScheduleOverlay.EndTime -> {
                val which = overlay
                val time = when (which) { ScheduleOverlay.StartTime -> range?.startTime; ScheduleOverlay.EndTime -> range?.endTime; else -> draft.time }
                key(which) { ReferenceTimePicker(time ?: LocalTime.of(10, 0), { overlay = null }, { picked ->
                    when (which) {
                        ScheduleOverlay.StartTime -> { range = range!!.copy(startTime = picked); overlay = ScheduleOverlay.EndTime }
                        ScheduleOverlay.EndTime -> { range = range!!.copy(endTime = picked); overlay = null }
                        else -> { draft = draft.copy(time = picked); overlay = null }
                    }
                    error = null
                }, title = when (which) { ScheduleOverlay.StartTime -> "Start Time"; ScheduleOverlay.EndTime -> "End Time"; else -> "Time" },
                    onClear = if (which == ScheduleOverlay.Time && draft.time != null) ({ draft = draft.copy(time = null); overlay = null }) else null) }
            }
            ScheduleOverlay.Reminder -> ReminderPicker(normalized(), clock, { overlay = null }) {
                draft = draft.copy(reminders = it); overlay = null; error = null
            }
            ScheduleOverlay.Repeat -> RepeatPicker(draft.repeat, normalized().date ?: today, { overlay = null }, {
                overlay = null; customRepeat = true
            }) { draft = draft.copy(repeat = it); overlay = null; error = null }
            ScheduleOverlay.DurationDates -> DurationDatePicker(range!!, today, { overlay = null }) { range = it; overlay = null; error = null }
            null -> Unit
        }
    }
}

@Composable
private fun DurationContent(range: TaskDuration, today: LocalDate, onDate: () -> Unit, onTime: () -> Unit, onAllDay: (Boolean) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(onClick = onDate).padding(16.dp)) {
                Text("Date", fontSize = 14.sp, color = ScheduleInk)
                Spacer(Modifier.height(12.dp))
                Text(if (range.startDate == range.endDate) range.startDate.label() else "${range.startDate.label()} – ${range.endDate.label()}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ScheduleLabelBlue)
                val days = ChronoUnit.DAYS.between(today, range.startDate)
                Text(when { days == 0L -> "Today"; days > 0 -> "$days Days Later"; else -> "${-days} Days Ago" }, Modifier.padding(top = 5.dp), fontSize = 12.sp, color = ScheduleMuted)
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(onClick = onTime).padding(16.dp)) {
                Text("Time", fontSize = 14.sp, color = ScheduleInk)
                Spacer(Modifier.height(12.dp))
                Text(if (range.allDay) "All day" else "${range.startTime.label()} - ${range.endTime.label()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScheduleLabelBlue)
                val minutes = Duration.between(range.startDate.atTime(range.startTime), range.endDate.atTime(range.endTime)).toMinutes()
                val length = if (range.allDay) "${ChronoUnit.DAYS.between(range.startDate, range.endDate) + 1} day(s)" else if (minutes <= 0) "Choose end time" else if (minutes % 60 == 0L) "${minutes / 60} hour${if (minutes == 60L) "" else "s"}" else "${minutes / 60}h ${minutes % 60}m"
                Text("Duration: $length", Modifier.padding(top = 5.dp), fontSize = 12.sp, color = ScheduleMuted)
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("All day", Modifier.weight(1f), fontSize = 16.sp, color = ScheduleInk)
            ReferenceSwitch(range.allDay, "All day", onAllDay)
        }
    }
}

@Composable
private fun DurationDatePicker(initial: TaskDuration, today: LocalDate, onCancel: () -> Unit, onApply: (TaskDuration) -> Unit) {
    var range by rememberSaveable { mutableStateOf(initial) }
    var end by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    ScheduleDialog("Duration dates", onCancel) {
        Row(Modifier.padding(horizontal = 12.dp)) {
            ScheduleTextButton("Start: ${range.startDate.label()}", { end = false }, Modifier.weight(1f), if (!end) ScheduleBlue else ScheduleMuted)
            ScheduleTextButton("End: ${range.endDate.label()}", { end = true }, Modifier.weight(1f), if (end) ScheduleBlue else ScheduleMuted)
        }
        key(end) { MonthCalendar(setOf(if (end) range.endDate else range.startDate), today, { range = if (end) range.copy(endDate = it) else range.copy(startDate = it); error = false }) }
        if (error) Text("End date must be on or after the start date.", Modifier.padding(horizontal = 20.dp), color = ScheduleRed, fontSize = 14.sp)
        DialogActions(onCancel, { if (range.endDate < range.startDate) error = true else onApply(range) })
    }
}
