package com.niranjan.ticktick.feature.taskeditor.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.domain.model.ReminderConfig
import com.niranjan.ticktick.domain.model.TaskSchedule
import java.time.Clock

internal fun offsetLabel(minutes: Long): String = when {
    minutes == 0L -> "On time"
    minutes % 10080L == 0L -> "${minutes / 10080} week${if (minutes == 10080L) "" else "s"} early"
    minutes % 1440L == 0L -> "${minutes / 1440} day${if (minutes == 1440L) "" else "s"} early"
    minutes % 60L == 0L -> "${minutes / 60} hour${if (minutes == 60L) "" else "s"} early"
    else -> "$minutes minutes early"
}

internal fun reminderSummary(config: ReminderConfig, timed: Boolean): String = when (config.offsetsMinutes.size) {
    0 -> "None"
    1 -> if (config.offsetsMinutes.single() == 0L && !timed) "On the day" else offsetLabel(config.offsetsMinutes.single())
    else -> "${config.offsetsMinutes.size} reminders"
}

@Composable
internal fun ReminderPicker(schedule: TaskSchedule, clock: Clock, onCancel: () -> Unit, onApply: (ReminderConfig) -> Unit) {
    var draft by rememberSaveable { mutableStateOf(schedule.reminders) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val timed = schedule.time != null
    val standard = if (timed) listOf(0L, 5L, 30L, 60L, 1440L) else listOf(0L, 1440L, 2880L, 4320L, 10080L)
    fun toggle(offset: Long) {
        if (offset in draft.offsetsMinutes) { draft = draft.copy(offsetsMinutes = draft.offsetsMinutes - offset); error = null }
        else if (!schedule.copy(reminders = draft).reminderIsFuture(offset, clock)) error = "This reminder would be in the past. Choose a later task date or time."
        else { draft = draft.copy(offsetsMinutes = (draft.offsetsMinutes + offset).distinct().sorted()); error = null }
    }
    ScheduleDialog("Reminder", onCancel) {
        ReminderChoice("None", false, { draft = draft.copy(offsetsMinutes = emptyList(), constant = false); error = null })
        (standard + draft.offsetsMinutes.filterNot { it in standard }).forEach { offset ->
            ReminderChoice(if (offset == 0L && !timed) "On the day" else offsetLabel(offset), offset in draft.offsetsMinutes, { toggle(offset) },
                suffix = if (!timed) " (${draft.dateOnlyTime})" else "")
        }
        ReminderChoice("Custom", false, { custom = true }, chevron = true)
        HorizontalDivider(Modifier.padding(horizontal = 14.dp, vertical = 5.dp), color = Color(0xFFF2F2F2))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Constant Reminder", Modifier.weight(1f), fontSize = 16.sp, color = ScheduleInk)
            ReferenceSwitch(draft.constant, "Constant Reminder") { enabled ->
                if (enabled && draft.offsetsMinutes.isEmpty()) {
                    if (schedule.copy(reminders = draft).reminderIsFuture(0L, clock)) { draft = draft.copy(constant = true, offsetsMinutes = listOf(0L)); error = null }
                    else error = "Choose a future reminder before enabling Constant Reminder."
                } else draft = draft.copy(constant = enabled)
            }
        }
        error?.let { Text(it, Modifier.padding(horizontal = 24.dp, vertical = 6.dp), color = ScheduleRed, fontSize = 13.sp) }
        DialogActions(onCancel, {
            if (draft.offsetsMinutes.any { !schedule.copy(reminders = draft).reminderIsFuture(it, clock) }) error = "A selected reminder is in the past. Remove it or change the task date or time."
            else onApply(draft.copy(constant = draft.constant && draft.offsetsMinutes.isNotEmpty()))
        })
    }
    if (custom) CustomReminderPicker(schedule.copy(reminders = draft), clock, { custom = false }) { offset, at ->
        draft = draft.copy(offsetsMinutes = (draft.offsetsMinutes + offset).distinct().sorted(), dateOnlyTime = at)
        custom = false; error = null
    }
}

@Composable
private fun ReminderChoice(title: String, selected: Boolean, onClick: () -> Unit, suffix: String = "", chevron: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 47.dp).clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = if (selected) ScheduleLabelBlue else ScheduleInk, fontSize = 16.sp)
            if (suffix.isNotEmpty()) Text(suffix, fontSize = 14.sp, color = ScheduleMuted)
        }
        if (selected || chevron) AppIcon(if (selected) AppSymbol.Check else AppSymbol.Chevron, Modifier.size(18.dp), if (selected) ScheduleLabelBlue else ScheduleMuted)
    }
}

@Composable
private fun CustomReminderPicker(schedule: TaskSchedule, clock: Clock, onCancel: () -> Unit, onApply: (Long, java.time.LocalTime) -> Unit) {
    var days by rememberSaveable { mutableStateOf("0") }
    var hours by rememberSaveable { mutableStateOf("0") }
    var minutes by rememberSaveable { mutableStateOf("0") }
    var at by rememberSaveable { mutableStateOf(schedule.reminders.dateOnlyTime) }
    var chooseTime by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    ScheduleDialog("Custom reminder", onCancel) {
        Text("Before the task", Modifier.padding(horizontal = 24.dp, vertical = 10.dp), color = ScheduleMuted, fontSize = 14.sp)
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Triple("Days", days, { text: String -> days = text }), Triple("Hours", hours, { text: String -> hours = text }), Triple("Minutes", minutes, { text: String -> minutes = text })).forEach { (label, value, change) ->
                OutlinedTextField(value, { change(it.filter(Char::isDigit).take(3)); error = null }, Modifier.weight(1f), label = { Text(label, fontSize = 12.sp) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        if (schedule.time == null) ScheduleOption(AppSymbol.Clock, "At", at.label(), { chooseTime = true }, true)
        error?.let { Text(it, Modifier.padding(20.dp), fontSize = 14.sp, color = ScheduleRed) }
        DialogActions(onCancel, {
            val d = days.toLongOrNull(); val h = hours.toLongOrNull(); val m = minutes.toLongOrNull()
            val offset = (d ?: 0) * 1440 + (h ?: 0) * 60 + (m ?: 0)
            error = when {
                d == null || h == null || m == null || h !in 0..23 || m !in 0..59 -> "Enter days, hours (0–23), and minutes (0–59)."
                schedule.reminders.offsetsMinutes.size >= 32 && offset !in schedule.reminders.offsetsMinutes -> "You can add up to 32 reminders."
                !schedule.copy(reminders = schedule.reminders.copy(dateOnlyTime = at)).reminderIsFuture(offset, clock) -> "This reminder would be in the past."
                else -> null
            }
            if (error == null) onApply(offset, at)
        })
    }
    if (chooseTime) ReferenceTimePicker(at, { chooseTime = false }, { at = it; chooseTime = false })
}
