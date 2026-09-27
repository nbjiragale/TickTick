package com.niranjan.ticktick.feature.habits

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.HabitRecordMode
import com.niranjan.ticktick.domain.model.HabitSnapshot
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.feature.taskeditor.schedule.*
import java.time.LocalDate
import kotlinx.coroutines.launch

private val CheckInBlue = Color(0xFF688AE4)

@Composable
internal fun HabitCheckInScreen(
    habit: Habit, date: LocalDate, today: LocalDate, snapshot: HabitSnapshot,
    onBack: () -> Unit, onEdit: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit,
    onProgress: suspend (Int) -> Result<Unit>,
    showManagementActions: Boolean = true,
) {
    val context = LocalContext.current
    var menu by rememberSaveable { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var writeError by remember { mutableStateOf<String?>(null) }
    fun record(value: Int, closePanel: Boolean = false) {
        if (saving) return
        saving = true
        scope.launch {
            try {
                onProgress(value).onSuccess { if (closePanel) panel = null }
                    .onFailure { writeError = it.message ?: "Couldn't save this check-in." }
            } finally { saving = false }
        }
    }
    val amount = snapshot.progressFor(habit, date)
    val rule = snapshot.configurationOn(habit, date)
    val target = rule.amount ?: 1
    val stats = remember(snapshot, habit.id, today) { snapshot.stats(habit, today) }
    val completed = date in snapshot.completedDates[habit.id].orEmpty()
    val eligible = snapshot.canRecord(habit, date, today)
    fun checkIn() {
        if (!eligible || completed) return
        if (rule.amount != null && habit.recordMode == HabitRecordMode.Manual) panel = "amount"
        else record(if (rule.amount == null) 1 else (amount.toLong() + habit.recordAmount).coerceAtMost(999999).toInt())
    }
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.let {
            it.setDimAmount(0f)
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightNavigationBars = true
        } }
        Column(Modifier.fillMaxSize().background(CheckInBlue).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ScheduleIcon(AppSymbol.Back, "Back to habits", onBack, tint = Color.White)
                Spacer(Modifier.weight(1f))
                if (showManagementActions) Box {
                    ScheduleIcon(AppSymbol.More, "Habit actions", { menu = true }, tint = Color.White)
                    DropdownMenu(menu, { menu = false }, Modifier.width(216.dp), offset = DpOffset(0.dp, (-38).dp),
                        shape = RoundedCornerShape(20.dp), containerColor = Color.White, tonalElevation = 0.dp, shadowElevation = 0.dp) {
                        HabitAction("Edit", AppSymbol.Pencil) { menu = false; onEdit() }
                        HabitAction("Share", AppSymbol.Share) {
                            menu = false
                            try {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${habit.name}\n${habit.quote}\n${snapshot.completedDates[habit.id]?.size ?: 0} total completed days")
                                }, "Share habit"))
                            } catch (_: ActivityNotFoundException) { panel = "shareError" }
                        }
                        HabitAction(if (habit.archived) "Unarchive" else "Archive", AppSymbol.Archive) { menu = false; onArchive() }
                        HabitAction("Delete", AppSymbol.Trash) { menu = false; panel = "delete" }
                    }
                }
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val height = maxHeight
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Column(Modifier.fillMaxWidth().heightIn(min = height), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height((height * .25f).coerceAtMost(190.dp)))
                        HabitBlockIllustration(Modifier.fillMaxWidth().height((height * .24f).coerceIn(140.dp, 190.dp)))
                        Spacer(Modifier.height((height * .145f).coerceAtMost(110.dp)))
                        Text(habit.name, Modifier.padding(horizontal = 24.dp), fontSize = 32.sp, lineHeight = 40.sp,
                            fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        if (habit.quote.isNotBlank()) Text(habit.quote, Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
                            fontSize = 16.sp, lineHeight = 22.sp, color = Color.White.copy(alpha = .75f), textAlign = TextAlign.Center)
                        if (date != today) Text(date.label(), Modifier.padding(top = 8.dp), color = Color.White.copy(alpha = .8f), fontSize = 14.sp)
                        if (rule.amount != null) Text("$amount / $target ${rule.unit}", Modifier.padding(top = 8.dp), color = Color.White, fontSize = 15.sp)
                        Spacer(Modifier.height(30.dp))
                        HabitCheckInControl(completed, eligible, ::checkIn)
                        if (!eligible) Text(when {
                            habit.archived -> "Archived habit"
                            date > today -> "Check in on this date"
                            snapshot.achievementDate(habit, today) != null -> "Goal achieved"
                            snapshot.weeklyTargetMet(habit, date) -> "Weekly target achieved"
                            else -> "Not scheduled for this date"
                        }, Modifier.padding(top = 12.dp), color = Color.White.copy(alpha = .8f), fontSize = 14.sp)
                        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
                        ScheduleIcon(AppSymbol.Chevron, "Open habit log", { panel = "log" }, Modifier.padding(bottom = 8.dp).rotate(-90f), Color.White)
                    }
                }
            }
        }
    }
    when (panel) {
        "delete" -> AlertDialog(onDismissRequest = { panel = null }, title = { Text("Delete habit?") },
            text = { Text("Delete ${habit.name} and its check-in history? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { panel = null; onDelete() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { panel = null }) { Text("Cancel") } })
        "shareError" -> ScheduleDialog("Share habit", { panel = null }) {
            Text("No sharing app is available on this device.", Modifier.padding(24.dp))
            ScheduleTextButton("OK", { panel = null }, Modifier.align(Alignment.End))
        }
        "amount" -> HabitAmountDialog(amount, rule.unit, { panel = null }) { value ->
            onProgress(value).onSuccess { panel = null }
        }
        "log" -> ScheduleDialog("Habit log", { panel = null }) {
            Text(date.label(), Modifier.padding(horizontal = 24.dp, vertical = 12.dp), fontWeight = FontWeight.Medium)
            Text(if (completed) "Checked in" else if (amount > 0) "$amount / $target ${rule.unit}" else "Not checked in", Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Text("${snapshot.completedDates[habit.id]?.size ?: 0} total completed days", Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Text("Current streak: ${stats.currentStreak} · Best: ${stats.bestStreak} ${if (stats.weekly) "weeks" else "scheduled days"}",
                Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (amount > 0 && date <= today) ScheduleTextButton("Undo check-in", { record(0, closePanel = true) }, Modifier.padding(start = 12.dp))
            ScheduleTextButton("Done", { panel = null }, Modifier.align(Alignment.End).padding(end = 12.dp))
        }
    }
    writeError?.let { message -> AlertDialog(onDismissRequest = { writeError = null }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { writeError = null }) { Text("OK") } }) }
}

@Composable
private fun HabitAction(label: String, symbol: AppSymbol, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 22.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(symbol, Modifier.size(20.dp), ScheduleInk)
        Text(label, Modifier.padding(start = 12.dp), fontSize = 16.sp, color = ScheduleInk)
    }
}

@Composable
private fun HabitCheckInControl(completed: Boolean, eligible: Boolean, onCheckIn: () -> Unit) {
    var drag by remember { mutableFloatStateOf(0f) }
    val travel = with(LocalDensity.current) { 152.dp.toPx() }
    val position by animateFloatAsState(if (completed) 1f else drag, label = "Habit check-in position")
    val action by rememberUpdatedState(onCheckIn)
    Box(Modifier.width(220.dp).height(68.dp).clip(RoundedCornerShape(36.dp)).background(Color(0xFF5E7FD0))
        .clickable(enabled = eligible && !completed, role = Role.Button, onClickLabel = "Check in", onClick = onCheckIn)
        .semantics {
            contentDescription = "Check in habit"
            stateDescription = if (completed) "Checked in" else if (eligible) "Not checked in" else "Unavailable on this date"
        }.pointerInput(eligible, completed, travel) {
            if (eligible && !completed) detectHorizontalDragGestures(onDragEnd = {
                if (drag >= .7f) action()
                drag = 0f
            }, onDragCancel = { drag = 0f }) { event, amount -> event.consume(); drag = (drag + amount / travel).coerceIn(0f, 1f) }
        }, contentAlignment = Alignment.CenterStart) {
        if (completed) Text("Checked in", Modifier.padding(start = 20.dp), color = Color.White, fontSize = 16.sp)
        Box(Modifier.offset(x = 152.dp * position).size(68.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            AppIcon(AppSymbol.Check, Modifier.size(30.dp), CheckInBlue)
        }
    }
}

@Composable
private fun HabitAmountDialog(initial: Int, unit: String, onCancel: () -> Unit, onApply: suspend (Int) -> Result<Unit>) {
    var input by rememberSaveable { mutableStateOf(initial.toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cancel = { if (!saving) onCancel() }
    ScheduleDialog("Record $unit", cancel) {
        HabitInput(input, { if (!saving) { input = it.filter(Char::isDigit).take(6); error = null } }, "Amount", Modifier.padding(24.dp).fillMaxWidth(), numeric = true)
        error?.let { Text(it, Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.error) }
        DialogActions(cancel, {
            if (!saving) {
                val value = input.toIntOrNull()
                if (value == null) error = "Enter an amount from 0 to 999999."
                else {
                    saving = true
                    scope.launch {
                        try { onApply(value).onFailure { error = it.message ?: "Couldn't save this check-in." } }
                        finally { saving = false }
                    }
                }
            }
        })
    }
}
