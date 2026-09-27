package com.niranjan.ticktick.feature.taskeditor.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val ScheduleBackground = Color(0xFFF7F7F7)
internal val ScheduleBlue = Color(0xFF4178F6)
internal val ScheduleLabelBlue = Color(0xFF557EBA)
internal val ScheduleMuted = Color(0xFFAAAAAA)
internal val ScheduleInk = Color(0xFF303231)
internal val ScheduleRed = Color(0xFFC73542)
internal fun LocalTime.label(): String = format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH))
internal fun LocalDate.label(): String = format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))

@Composable
internal fun ScheduleIcon(symbol: AppSymbol, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = ScheduleInk) {
    Box(modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        AppIcon(symbol, Modifier.size(24.dp), tint)
    }
}

@Composable
internal fun ScheduleTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = ScheduleLabelBlue) {
    Box(modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun ScheduleDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let {
                it.setDimAmount(.25f)
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightNavigationBars = true
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp).widthIn(max = 326.dp).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)).background(Color.White).verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 14.dp)) {
            Text(title, Modifier.padding(horizontal = 24.dp, vertical = 4.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScheduleInk)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
internal fun DialogActions(onCancel: () -> Unit, onConfirm: (() -> Unit)? = null, leading: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        leading?.invoke()
        Spacer(Modifier.weight(1f))
        ScheduleTextButton("Cancel", onCancel)
        if (onConfirm != null) ScheduleTextButton("OK", onConfirm)
    }
}

@Composable
internal fun ScheduleOption(symbol: AppSymbol, title: String, value: String, onClick: () -> Unit, selected: Boolean = false,
    onClear: (() -> Unit)? = null, mutedValue: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 49.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(symbol, Modifier.size(21.dp), Color(0xFF858887))
        Text(title, Modifier.padding(start = 12.dp).weight(1f), fontSize = 16.sp, color = ScheduleInk)
        Text(value, Modifier.widthIn(max = 155.dp), fontSize = 16.sp, color = if (selected && !mutedValue) ScheduleLabelBlue else ScheduleMuted, textAlign = TextAlign.End)
        if (onClear != null) ScheduleIcon(AppSymbol.Close, "Clear $title", onClear, Modifier.padding(start = 3.dp).size(24.dp), ScheduleMuted)
        else AppIcon(AppSymbol.Chevron, Modifier.padding(start = 7.dp).size(14.dp), ScheduleMuted)
    }
}

@Composable
internal fun ReferenceSwitch(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    Box(Modifier.size(48.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(30.dp, 14.dp).background(if (checked) ScheduleBlue.copy(alpha = .45f) else Color(0xFFE7E7E7), CircleShape))
        Box(Modifier.offset(x = if (checked) 8.dp else (-8).dp).size(20.dp).shadow(1.dp, CircleShape)
            .background(if (checked) ScheduleBlue else Color(0xFFF0F1F6), CircleShape))
    }
}

@Composable
internal fun MonthCalendar(selected: Set<LocalDate>, today: LocalDate, onSelect: (LocalDate) -> Unit, initialDate: LocalDate = selected.minOrNull() ?: today) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(initialDate).toString()) }
    val month = YearMonth.parse(monthText)
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(40.dp).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(month.format(DateTimeFormatter.ofPattern(if (month.year == today.year) "MMMM" else "MMMM yyyy", Locale.ENGLISH)), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScheduleInk)
            ScheduleIcon(AppSymbol.Chevron, "Previous month", { monthText = month.minusMonths(1).toString() }, Modifier.rotate(180f), ScheduleMuted)
            ScheduleIcon(AppSymbol.Chevron, "Next month", { monthText = month.plusMonths(1).toString() }, tint = ScheduleMuted)
        }
        Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = ScheduleMuted) }
        }
        val offset = month.atDay(1).dayOfWeek.value % 7
        val rows = (offset + month.lengthOfMonth() + 6) / 7
        repeat(rows) { week ->
            Row(Modifier.fillMaxWidth().height(43.dp)) {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - offset + 1
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            val active = date in selected
                            Box(Modifier.size(38.dp).clip(CircleShape)
                                .background(if (active) ScheduleBlue else if (date == today) Color.White else Color.Transparent)
                                .clickable(role = Role.Button) { onSelect(date) }
                                .semantics { contentDescription = "${date.label()}, ${date.year}${if (active) ", selected" else ""}" }, contentAlignment = Alignment.Center) {
                                Text(day.toString(), fontSize = 14.sp, color = if (active) Color.White else if (date == today) ScheduleLabelBlue else ScheduleInk)
                            }
                        }
                    }
                }
            }
        }
    }
}
