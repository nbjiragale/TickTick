package com.niranjan.ticktick.feature.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared task-alert pause control; changes are acknowledged by durable reminder storage. */
@Composable
fun DailyAlertsToggle(enabled: Boolean, date: LocalDate, resumeDate: LocalDate?, onChange: (Boolean) -> Unit,
    onPauseDays: suspend (Int) -> Result<Unit>) {
    var menu by rememberSaveable { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf(false) }
    val offset by animateDpAsState(if (enabled) 23.dp else 3.dp, tween(220), label = "Daily bell position")
    val fill by animateColorAsState(if (enabled) TickTickColors.Accent else Color(0xFFE1E4EC), tween(220), label = "Daily bell fill")
    val iconColor by animateColorAsState(if (enabled) TickTickColors.Accent else TickTickColors.SecondaryText, tween(220), label = "Daily bell tint")
    val dayLabel = date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH))
    Box {
    Box(Modifier.size(52.dp, 48.dp).combinedClickable(role = Role.Switch,
        onClick = { onChange(!enabled) }, onLongClickLabel = "Notification pause options", onLongClick = { menu = true })
        .semantics {
            contentDescription = "Notifications, alarms and pop-ups for today, $dayLabel"
            toggleableState = ToggleableState(enabled)
            stateDescription = if (enabled) "On" else "Off. Notifications resume ${resumeDate?.format(ResumeDateFormat) ?: "tomorrow"}"
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(48.dp, 28.dp).background(fill, RoundedCornerShape(14.dp))
            .border(1.dp, fill, RoundedCornerShape(14.dp)), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.offset(x = offset).size(22.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                AppIcon(AppSymbol.Bell, Modifier.size(18.dp).padding(1.dp), iconColor)
            }
        }
    }
    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(14.dp), containerColor = Color.White) {
        DropdownMenuItem(text = { Text(if (enabled) "Disable for today" else "Enable for today") }, onClick = { onChange(!enabled); menu = false })
        DropdownMenuItem(text = { Text("Custom") }, onClick = { menu = false; custom = true })
    }
    }
    if (custom) CustomNotificationPause(date, resumeDate, onDismiss = { custom = false }, onPauseDays)
}

private val ResumeDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

@Composable
private fun CustomNotificationPause(today: LocalDate, currentResumeDate: LocalDate?, onDismiss: () -> Unit, onApply: suspend (Int) -> Result<Unit>) {
    var input by rememberSaveable { mutableStateOf(currentResumeDate?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it).toString() }.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val days = input.toIntOrNull()?.takeIf { it in 1..3650 }
    val resume = days?.let { today.plusDays(it.toLong()) }
    val focus = remember { FocusRequester() }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, shape = RoundedCornerShape(18.dp), containerColor = Color.White,
        title = { Text("Pause notifications") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("How many days do you want to disable notifications?")
                OutlinedTextField(input, { if (it.length <= 4 && it.all(Char::isDigit)) { input = it; error = null } },
                    modifier = Modifier.focusRequester(focus), label = { Text("Number of days") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = input.isNotEmpty() && days == null,
                    supportingText = { if (input.isNotEmpty() && days == null) Text("Enter 1–3650 days.") })
                if (resume != null) Text("Notifications resume from ${resume.format(ResumeDateFormat)}.",
                    color = TickTickColors.DueTime, style = MaterialTheme.typography.bodyMedium)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(enabled = days != null && !saving, onClick = {
            days?.let { value ->
                saving = true
                scope.launch {
                    try { onApply(value).fold(onSuccess = { onDismiss() }, onFailure = { error = it.message }) }
                    finally { saving = false }
                }
            }
        }) { Text("Apply") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } })
    LaunchedEffect(Unit) { focus.requestFocus() }
}
