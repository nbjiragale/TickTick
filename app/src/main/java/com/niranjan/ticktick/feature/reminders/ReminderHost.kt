package com.niranjan.ticktick.feature.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.feature.taskeditor.schedule.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ReminderHost(vm: ReminderViewModel) {
    val observed by vm.uiState.collectAsStateWithLifecycle()
    val confirmation by vm.confirmation.collectAsStateWithLifecycle()
    // Each rendered action keeps its delivery ID, including while the queue advances.
    val state = observed
    if (state.active) Dialog(onDismissRequest = vm::dismissOverlay, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false,
    )) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let {
                it.setDimAmount(.16f)
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightNavigationBars = true
            }
        }
        BackHandler(onBack = vm::back)
        var hoursScrolling by remember { mutableStateOf(false) }
        var minutesScrolling by remember { mutableStateOf(false) }
        val custom = state.page == SnoozePage.Custom
        val reminder = state.page == SnoozePage.Reminder
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            val reminderHeight = (maxHeight - 20.dp).coerceIn(0.dp, 273.dp)
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { vm.dismissOverlay() } })
            Column(Modifier.align(Alignment.BottomCenter).padding(10.dp).widthIn(max = 480.dp).fillMaxWidth()
                .heightIn(max = (maxHeight - 20.dp).coerceAtLeast(0.dp))
                .clip(RoundedCornerShape(14.dp)).background(Color.White)
                .pointerInput(Unit) { detectTapGestures { } }
                .then(if (reminder) Modifier else Modifier.verticalScroll(rememberScrollState()))) {
                if (reminder && state.task != null) key(state.alertId) {
                    ReminderCard(state.task!!, state.list, vm.clock, state.error,
                        Modifier.fillMaxWidth().height(reminderHeight),
                        onSnooze = { vm.page(SnoozePage.Options) },
                        onComplete = { vm.complete(state.alertId) }, onClose = { vm.dismiss(state.alertId) })
                } else {
                Box(Modifier.fillMaxWidth().height(56.dp)) {
                    ScheduleIcon(AppSymbol.Back, "Back from snooze", vm::back, Modifier.align(Alignment.CenterStart).padding(start = 7.dp), ScheduleMuted)
                    Text("Snooze", Modifier.align(Alignment.Center), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScheduleInk)
                    if (custom && state.task != null) Box(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) {
                        val enabled = state.customMinutes > 0 && (state.keyboard || (!hoursScrolling && !minutesScrolling))
                        Box(Modifier.size(44.dp).clickable(enabled = enabled, role = Role.Button, onClick = { vm.confirmCustom(state.alertId) })
                            .semantics { contentDescription = "Apply custom snooze" }, contentAlignment = Alignment.Center) {
                            AppIcon(AppSymbol.Check, Modifier.size(24.dp), ScheduleInk.copy(alpha = if (enabled) .6f else .2f))
                        }
                    }
                }
                if (state.task == null) {
                    Text("This task is no longer available to snooze.", Modifier.padding(24.dp), color = ScheduleMuted, fontSize = 14.sp)
                    ScheduleTextButton("Close", { vm.dismiss(state.alertId) }, Modifier.fillMaxWidth())
                } else if (custom) {
                    if (state.keyboard) Row(Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SnoozeNumberInput("Hour", state.hours, 23, vm::setHours, Modifier.weight(1f))
                        SnoozeNumberInput("Minutes", state.minutes, 59, vm::setMinutes, Modifier.weight(1f))
                    } else Row(Modifier.fillMaxWidth().padding(horizontal = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                        SnoozeWheel(24, state.hours, "Hours", Modifier.weight(1f), { hoursScrolling = it }, vm::setHours)
                        Text("Hour", Modifier.padding(horizontal = 10.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ScheduleMuted)
                        SnoozeWheel(60, state.minutes, "Minutes", Modifier.weight(1f), { minutesScrolling = it }, vm::setMinutes)
                        Text("Minutes", Modifier.padding(start = 10.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ScheduleMuted)
                    }
                    state.error?.let { Text(it, Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = ScheduleRed, fontSize = 13.sp) }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(49.dp)) {
                        ScheduleIcon(if (state.keyboard) AppSymbol.Clock else AppSymbol.Keyboard,
                            if (state.keyboard) "Use snooze wheels" else "Type snooze duration", vm::toggleKeyboard,
                            Modifier.align(Alignment.CenterStart).padding(start = 8.dp), ScheduleMuted)
                        Text(state.customUntil.snoozeLabel(vm.clock), Modifier.align(Alignment.Center).padding(horizontal = 50.dp),
                            fontSize = 14.sp, color = ScheduleMuted, textAlign = TextAlign.Center)
                    }
                } else {
                    val task = state.task!!
                    val choices = SnoozeChoice.entries
                    listOf(choices.take(4), choices.drop(4)).forEachIndexed { rowIndex, row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                            row.forEach { choice ->
                                val deadline = choice.deadline(state.now, vm.clock, task.dueTime ?: task.reminders.dateOnlyTime)
                                val enabled = deadline.isAfter(state.now)
                                SnoozeOption(choice.label, when (choice) {
                                    SnoozeChoice.Tomorrow -> AppSymbol.Sunrise
                                    SnoozeChoice.TodayNight -> AppSymbol.Moon
                                    else -> AppSymbol.Snooze
                                }, if (rowIndex == 0) listOf("15m", "30m", "1h", "3h")[row.indexOf(choice)] else null,
                                    enabled, "${choice.label}, ${deadline.snoozeLabel(vm.clock)}", Modifier.weight(1f)) { vm.choose(choice, state.alertId) }
                            }
                            if (rowIndex == 1) SnoozeOption("Custom", AppSymbol.Pencil, null, true, "Custom snooze", Modifier.weight(1f)) { vm.page(SnoozePage.Custom) }
                        }
                    }
                    state.error?.let { Text(it, Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = ScheduleRed, fontSize = 13.sp) }
                    ScheduleTextButton("Change Date", { vm.page(SnoozePage.ChangeDate) }, Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 3.dp))
                }
                }
            }
        }
        if (state.page == SnoozePage.ChangeDate && state.task != null) key(state.task!!.id) {
            SchedulePicker(state.task!!.schedule, vm.clock, vm::back, { vm.applySchedule(it, state.alertId) }, centered = true)
        }
    }
    // Keep confirmation above any next queued reminder, as well as after the last one closes.
    confirmation?.let { SnoozeConfirmationCard(it) }
}

@Composable
private fun SnoozeOption(label: String, icon: AppSymbol, badge: String?, enabled: Boolean, description: String,
    modifier: Modifier, onClick: () -> Unit) {
    val tint = ScheduleLabelBlue.copy(alpha = if (enabled) 1f else .3f)
    Column(modifier.heightIn(min = 80.dp).clip(RoundedCornerShape(8.dp))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = description }
        .padding(top = 14.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(34.dp)) {
            AppIcon(icon, Modifier.size(32.dp), tint)
            if (badge != null) Text(badge, Modifier.align(Alignment.BottomCenter).background(Color.White).padding(horizontal = 2.dp),
                fontSize = 14.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = tint)
        }
        Text(label, Modifier.padding(top = 6.dp), fontSize = 12.sp, color = ScheduleInk.copy(alpha = if (enabled) .65f else .25f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun SnoozeNumberInput(label: String, value: Int, maximum: Int, onChange: (Int) -> Unit, modifier: Modifier) {
    var text by rememberSaveable { mutableStateOf(value.toString()) }
    OutlinedTextField(text, { input ->
        if (input.length <= 2 && input.all(Char::isDigit) && (input.toIntOrNull() ?: 0) <= maximum) {
            text = input; onChange(input.toIntOrNull() ?: 0)
        }
    }, modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

@Composable
private fun SnoozeWheel(count: Int, selected: Int, label: String, modifier: Modifier,
    onScrolling: (Boolean) -> Unit, onSelect: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = count * 1_000 + selected)
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)
    val scrolling by rememberUpdatedState(onScrolling)
    var selectedIndex by remember { mutableIntStateOf(count * 1_000 + selected) }
    LaunchedEffect(state) {
        snapshotFlow {
            val layout = state.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index to state.isScrollInProgress
        }.distinctUntilChanged().collect { (index, moving) ->
            scrolling(moving)
            if (index != null) { selectedIndex = index; select(index % count) }
        }
    }
    DisposableEffect(Unit) { onDispose { scrolling(false) } }
    LazyColumn(state = state, flingBehavior = rememberSnapFlingBehavior(state), contentPadding = PaddingValues(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.height(160.dp).semantics {
            stateDescription = "$label: $selected"
            customActions = listOf(
                CustomAccessibilityAction("Increase $label") { scope.launch { state.animateScrollToItem((selectedIndex + 1).coerceAtMost(count * 2_001 - 1)) }; true },
                CustomAccessibilityAction("Decrease $label") { scope.launch { state.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0)) }; true },
            )
        }) {
        items(count * 2_001) { index ->
            val distance = abs(index - selectedIndex)
            Box(Modifier.fillMaxWidth().height(32.dp).clickable { scope.launch { state.animateScrollToItem(index) } }, contentAlignment = Alignment.Center) {
                Text((index % count).toString(), fontSize = 20.sp, fontWeight = FontWeight.Medium,
                    color = ScheduleInk.copy(alpha = when (distance) { 0 -> .85f; 1 -> .5f; else -> .13f }))
            }
        }
    }
}
