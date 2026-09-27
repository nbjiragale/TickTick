package com.niranjan.ticktick.feature.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.HabitRecordMode
import com.niranjan.ticktick.feature.taskeditor.schedule.*
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
internal fun HabitNumberWheel(value: Int, max: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = value - 1)
    val scope = rememberCoroutineScope()
    val change by rememberUpdatedState(onChange)
    LaunchedEffect(state) {
        snapshotFlow {
            val layout = state.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        }.distinctUntilChanged().collect { if (it != null) change(it + 1) }
    }
    LazyColumn(state = state, flingBehavior = rememberSnapFlingBehavior(state), contentPadding = PaddingValues(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.height(208.dp).semantics {
            stateDescription = "Frequency: $value"
            customActions = listOf(
                CustomAccessibilityAction("Increase frequency") { scope.launch { state.animateScrollToItem(value.coerceAtMost(max - 1)) }; true },
                CustomAccessibilityAction("Decrease frequency") { scope.launch { state.animateScrollToItem((value - 2).coerceAtLeast(0)) }; true },
            )
        }) {
        items(max) { index ->
            Box(Modifier.fillMaxWidth().height(48.dp).clickable { scope.launch { state.animateScrollToItem(index) } }, contentAlignment = Alignment.Center) {
                Text((index + 1).toString(), fontSize = 20.sp, fontWeight = FontWeight.Medium,
                    color = ScheduleInk.copy(alpha = when (abs(index + 1 - value)) { 0 -> 1f; 1 -> .5f; else -> .15f }))
            }
        }
    }
}

@Composable
internal fun HabitGoalDialog(initial: Habit, onCancel: () -> Unit, onApply: (Habit) -> Unit) {
    var amountGoal by rememberSaveable { mutableStateOf(initial.amount != null) }
    var amount by rememberSaveable { mutableStateOf((initial.amount ?: 1).toString()) }
    var unit by rememberSaveable { mutableStateOf(initial.unit) }
    var recordMode by rememberSaveable { mutableStateOf(initial.recordMode) }
    var recordAmount by rememberSaveable { mutableStateOf(initial.recordAmount.toString()) }
    var error by rememberSaveable { mutableStateOf(false) }
    ScheduleDialog("Goal", onCancel) {
        Column(Modifier.selectableGroup()) {
            HabitChoice("Achieve it all", !amountGoal, { amountGoal = false; error = false })
            HabitChoice("Reach a certain amount", amountGoal, { amountGoal = true })
        }
        if (amountGoal) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily", Modifier.weight(1f), fontSize = 16.sp)
                    HabitInput(amount, { amount = it.filter(Char::isDigit).take(6) }, "Amount", Modifier.width(58.dp), numeric = true)
                    Spacer(Modifier.width(12.dp))
                    HabitMenu(unit, listOf("Count", "Minute", "Hour", "Page", "ml", "km"), { unit = it }, Modifier.width(117.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("When checking", Modifier.weight(1f), fontSize = 16.sp)
                    HabitMenu(recordMode.name, HabitRecordMode.entries.map { it.name }, { recordMode = HabitRecordMode.valueOf(it) }, Modifier.width(117.dp))
                }
                if (recordMode == HabitRecordMode.Auto) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Record  ($unit)", Modifier.weight(1f), fontSize = 16.sp)
                    HabitInput(recordAmount, { recordAmount = it.filter(Char::isDigit).take(6) }, "Record amount", Modifier.width(117.dp), numeric = true)
                }
            }
        }
        if (error) Text("Enter amounts from 1 to 999999.", Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        DialogActions(onCancel, {
            val target = amount.toIntOrNull()
            val increment = recordAmount.toIntOrNull()
            if (amountGoal && (target == null || target !in 1..999999 || (recordMode == HabitRecordMode.Auto && (increment == null || increment !in 1..999999)))) error = true
            else onApply(initial.copy(amount = if (amountGoal) target else null, unit = unit, recordMode = recordMode, recordAmount = increment?.takeIf { it in 1..999999 } ?: 1))
        })
    }
}

@Composable
internal fun HabitGoalDaysDialog(initial: Int?, onCancel: () -> Unit, onApply: (Int?) -> Unit) {
    val presets = listOf(0, 7, 21, 30, 100, 365)
    var selected by rememberSaveable { mutableIntStateOf(if (initial == null) 0 else if (initial in presets) initial else -1) }
    var custom by rememberSaveable { mutableStateOf(initial?.toString() ?: "") }
    var error by rememberSaveable { mutableStateOf(false) }
    ScheduleDialog("Goal Days", onCancel) {
        Column(Modifier.selectableGroup()) {
            presets.forEach { value -> HabitChoice(if (value == 0) "Forever" else "$value days", selected == value, { selected = value; error = false }) }
            HabitChoice("Custom", selected == -1, { selected = -1 }) {
                if (selected == -1) {
                    HabitInput(custom, { custom = it.filter(Char::isDigit).take(3); error = false }, "1~999", Modifier.width(97.dp), numeric = true)
                    Text("Days", Modifier.padding(start = 8.dp), fontSize = 14.sp)
                }
            }
        }
        if (error) Text("Enter 1–999 days.", Modifier.padding(start = 24.dp, top = 8.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        DialogActions(onCancel, {
            val days = if (selected == -1) custom.toIntOrNull() else selected
            if (days == null || days !in 0..999 || (selected == -1 && days == 0)) error = true
            else onApply(days.takeIf { it > 0 })
        })
    }
}

@Composable
internal fun HabitDateDialog(initial: LocalDate, today: LocalDate, onCancel: () -> Unit, onApply: (LocalDate) -> Unit) {
    var day by rememberSaveable { mutableLongStateOf(initial.toEpochDay()) }
    ScheduleDialog("Date", onCancel) {
        MonthCalendar(setOf(LocalDate.ofEpochDay(day)), today, { day = it.toEpochDay() }, initial)
        Spacer(Modifier.height(52.dp))
        Row(Modifier.fillMaxWidth().padding(end = 14.dp), horizontalArrangement = Arrangement.End) {
            ScheduleTextButton("Cancel", onCancel)
            ScheduleTextButton("Confirm", { onApply(LocalDate.ofEpochDay(day)) })
        }
    }
}

@Composable
internal fun HabitLetterDialog(initial: String, onCancel: () -> Unit, onApply: (String) -> Unit) {
    var letter by rememberSaveable { mutableStateOf(initial) }
    ScheduleDialog("Letter avatar", onCancel) {
        HabitInput(letter, { letter = it.filter(Char::isLetter).take(1).uppercase() }, "Letter", Modifier.padding(24.dp).fillMaxWidth())
        DialogActions(onCancel, { if (letter.isNotEmpty()) onApply(letter) })
    }
}
