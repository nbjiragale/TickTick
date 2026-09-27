package com.niranjan.ticktick.feature.taskeditor.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val weekOrder = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
private val workdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private fun DayOfWeek.shortName(): String = getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)

internal fun repeatSummary(rule: RepeatRule): String = when {
    rule.basis == RepeatBasis.SpecificDates -> "${rule.dates.size} specific dates"
    rule.unit == RepeatUnit.None -> "None"
    rule.basis == RepeatBasis.Completion -> "Every ${rule.interval} ${rule.unit.name.lowercase()}${if (rule.interval == 1) "" else "s"} after completion"
    rule.interval != 1 -> "Every ${rule.interval} ${rule.unit.name.lowercase()}s"
    rule.unit == RepeatUnit.Week && rule.weekdays == workdays -> "Every Weekday"
    else -> when (rule.unit) { RepeatUnit.Day -> "Daily"; RepeatUnit.Week -> "Weekly"; RepeatUnit.Month -> "Monthly"; RepeatUnit.Year -> "Yearly"; else -> "None" }
}

@Composable
internal fun RepeatPicker(initial: RepeatRule, date: LocalDate, onCancel: () -> Unit, onCustom: () -> Unit, onApply: (RepeatRule) -> Unit) {
    val presets = listOf(
        Triple("None", "", RepeatRule()),
        Triple("Daily", "", RepeatRule(RepeatUnit.Day)),
        Triple("Weekly", " (${date.dayOfWeek.shortName()})", RepeatRule(RepeatUnit.Week, weekdays = setOf(date.dayOfWeek))),
        Triple("Monthly", " (The ${date.dayOfMonth} day)", RepeatRule(RepeatUnit.Month)),
        Triple("Yearly", " (on ${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))})", RepeatRule(RepeatUnit.Year)),
        Triple("Every Weekday", " (Mon - Fri)", RepeatRule(RepeatUnit.Week, weekdays = workdays)),
    )
    ScheduleDialog("Repeat", onCancel) {
        presets.forEachIndexed { index, (title, suffix, rule) ->
            if (index == 5) HorizontalDivider(Modifier.padding(horizontal = 14.dp, vertical = 5.dp), color = Color(0xFFF2F2F2))
            Row(Modifier.fillMaxWidth().heightIn(min = 47.dp).clickable { onApply(rule) }.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 16.sp, color = if (initial == rule) ScheduleLabelBlue else ScheduleInk)
                    if (suffix.isNotEmpty()) Text(suffix, fontSize = 14.sp, color = ScheduleMuted)
                }
                if (initial == rule) AppIcon(AppSymbol.Check, Modifier.size(18.dp), ScheduleLabelBlue)
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 14.dp, vertical = 5.dp), color = Color(0xFFF2F2F2))
        Row(Modifier.fillMaxWidth().clickable(onClick = onCustom).padding(horizontal = 24.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Custom", Modifier.weight(1f), fontSize = 16.sp, color = ScheduleInk)
            if (presets.none { it.third == initial }) AppIcon(AppSymbol.Check, Modifier.size(18.dp), ScheduleLabelBlue)
        }
        DialogActions(onCancel)
    }
}

@Composable
internal fun CustomRepeatScreen(initial: RepeatRule, date: LocalDate, today: LocalDate, onCancel: () -> Unit, onApply: (RepeatRule) -> Unit) {
    var draft by rememberSaveable { mutableStateOf(if (initial == RepeatRule()) RepeatRule(RepeatUnit.Week, weekdays = setOf(date.dayOfWeek)) else initial) }
    var menu by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val basisNames = mapOf(RepeatBasis.DueDates to "By Due Dates", RepeatBasis.Completion to "By Completion…", RepeatBasis.SpecificDates to "By Specific Dates")
    Column(Modifier.fillMaxSize().background(ScheduleBackground).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            ScheduleIcon(AppSymbol.Close, "Cancel custom repeat", onCancel)
            Text("Custom", Modifier.padding(start = 4.dp).weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ScheduleInk)
            ScheduleIcon(AppSymbol.Check, "Apply custom repeat", {
                error = when {
                    draft.basis == RepeatBasis.SpecificDates && draft.dates.isEmpty() -> "Choose at least one date."
                    draft.basis != RepeatBasis.SpecificDates && draft.unit == RepeatUnit.Week && draft.weekdays.isEmpty() -> "Choose at least one weekday."
                    else -> null
                }
                if (error == null) onApply(if (draft.basis == RepeatBasis.SpecificDates) draft.copy(unit = RepeatUnit.None, interval = 1, weekdays = emptySet())
                    else draft.copy(dates = emptyList(), weekdays = if (draft.unit == RepeatUnit.Week) draft.weekdays else emptySet()))
            })
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(start = 16.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Repeat Type", fontSize = 16.sp, color = ScheduleInk)
                Text(" ⓘ", fontSize = 14.sp, color = ScheduleMuted)
                Spacer(Modifier.weight(1f))
                Box {
                    Row(Modifier.heightIn(min = 46.dp).clickable { menu = true }, verticalAlignment = Alignment.CenterVertically) {
                        Text(basisNames.getValue(draft.basis), fontSize = 16.sp, color = ScheduleInk)
                        AppIcon(AppSymbol.Chevron, Modifier.padding(start = 6.dp).size(16.dp).rotate(90f), ScheduleMuted)
                    }
                    DropdownMenu(menu, { menu = false }, Modifier.width(170.dp), shape = RoundedCornerShape(16.dp), containerColor = Color.White, tonalElevation = 0.dp, shadowElevation = 3.dp) {
                        basisNames.forEach { (basis, label) ->
                            DropdownMenuItem(text = { Text(label, fontSize = 16.sp, color = if (draft.basis == basis) ScheduleLabelBlue else ScheduleInk, maxLines = 1) }, onClick = {
                                draft = draft.copy(basis = basis, unit = if (basis != RepeatBasis.SpecificDates && draft.unit == RepeatUnit.None) RepeatUnit.Week else draft.unit,
                                    weekdays = draft.weekdays.ifEmpty { setOf(date.dayOfWeek) })
                                menu = false; error = null
                            }, trailingIcon = { if (draft.basis == basis) AppIcon(AppSymbol.Check, Modifier.size(18.dp), ScheduleLabelBlue) })
                        }
                    }
                }
            }
            if (draft.basis == RepeatBasis.SpecificDates) {
                Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 12.dp)) {
                    Text("Specific dates", Modifier.padding(horizontal = 16.dp), fontSize = 16.sp, color = ScheduleInk)
                    MonthCalendar(draft.dates.toSet(), today, { selected ->
                        if (selected in draft.dates) draft = draft.copy(dates = draft.dates - selected)
                        else if (draft.dates.size < 366) draft = draft.copy(dates = (draft.dates + selected).sorted())
                        else error = "You can select up to 366 dates."
                    }, initialDate = draft.dates.firstOrNull() ?: date)
                    Text("${draft.dates.size} dates selected", Modifier.padding(16.dp), color = ScheduleMuted, fontSize = 14.sp)
                    draft.dates.forEach { chosen ->
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(chosen.label() + ", ${chosen.year}", Modifier.weight(1f), fontSize = 14.sp, color = ScheduleLabelBlue)
                            ScheduleIcon(AppSymbol.Close, "Remove ${chosen.label()}", { draft = draft.copy(dates = draft.dates - chosen) }, tint = ScheduleMuted)
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(top = 14.dp)) {
                    Text("Frequency", Modifier.padding(horizontal = 16.dp), fontSize = 16.sp, color = ScheduleInk)
                    Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Every", Modifier.weight(1f), fontSize = 18.sp, color = ScheduleInk, textAlign = TextAlign.Center)
                        FrequencyWheel((1..999).map(Int::toString), draft.interval - 1, "Repeat interval", Modifier.weight(1f)) { draft = draft.copy(interval = it + 1) }
                        val units = listOf(RepeatUnit.Day, RepeatUnit.Week, RepeatUnit.Month, RepeatUnit.Year)
                        FrequencyWheel(units.map { it.name }, units.indexOf(draft.unit).coerceAtLeast(0), "Repeat unit", Modifier.weight(1.2f)) {
                            draft = draft.copy(unit = units[it], weekdays = draft.weekdays.ifEmpty { setOf(date.dayOfWeek) }); error = null
                        }
                    }
                }
                Text(repeatSummary(draft) + if (draft.unit == RepeatUnit.Week) " on ${weekOrder.filter { it in draft.weekdays }.joinToString { it.shortName() }}" else "", Modifier.padding(horizontal = 8.dp), fontSize = 14.sp, color = Color(0xFF7B7D7D))
                if (draft.unit == RepeatUnit.Week) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp)) {
                    Text("Week", fontSize = 16.sp, color = ScheduleInk)
                    Spacer(Modifier.height(12.dp))
                    weekOrder.chunked(4).forEach { days ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                            days.forEach { day ->
                                val selected = day in draft.weekdays
                                Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(20.dp)).background(if (selected) ScheduleBlue else Color(0xFFF8F8F8)).clickable {
                                    draft = draft.copy(weekdays = if (selected) draft.weekdays - day else draft.weekdays + day); error = null
                                }, contentAlignment = Alignment.Center) { Text(day.shortName(), fontSize = 14.sp, color = if (selected) Color.White else Color(0xFF787A79)) }
                            }
                            repeat(4 - days.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            error?.let { Text(it, color = ScheduleRed, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun FrequencyWheel(values: List<String>, selected: Int, label: String, modifier: Modifier, onSelect: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val scope = rememberCoroutineScope()
    val change by rememberUpdatedState(onSelect)
    LaunchedEffect(state) {
        snapshotFlow {
            val layout = state.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        }.distinctUntilChanged().collect { index -> if (index != null) change(index) }
    }
    LazyColumn(state = state, flingBehavior = rememberSnapFlingBehavior(state), contentPadding = PaddingValues(vertical = 62.dp),
        horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.height(164.dp).semantics {
            stateDescription = "$label: ${values[selected]}"
            customActions = listOf(
                CustomAccessibilityAction("Increase $label") { scope.launch { state.animateScrollToItem((selected + 1).coerceAtMost(values.lastIndex)) }; true },
                CustomAccessibilityAction("Decrease $label") { scope.launch { state.animateScrollToItem((selected - 1).coerceAtLeast(0)) }; true },
            )
        }) {
        items(values.size) { index ->
            val distance = abs(index - selected)
            Box(Modifier.fillMaxWidth().height(40.dp).clickable { scope.launch { state.animateScrollToItem(index) } }, contentAlignment = Alignment.Center) {
                Text(values[index], fontSize = 20.sp, fontWeight = FontWeight.Medium, color = ScheduleInk.copy(alpha = when (distance) { 0 -> .9f; 1 -> .5f; else -> .18f }))
            }
        }
    }
}
