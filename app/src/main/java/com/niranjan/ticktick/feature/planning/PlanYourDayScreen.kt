package com.niranjan.ticktick.feature.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskList
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun PlanYourDayHost(state: PlanYourDayState, vm: PlanYourDayViewModel) {
    if (!state.active) return
    Dialog(onDismissRequest = vm::back, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
    )) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let {
                it.setDimAmount(0f)
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightNavigationBars = true
            }
        }
        Column(Modifier.fillMaxSize().background(TickTickColors.Background).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 12.dp, top = 8.dp)) {
                IconButton(onClick = vm::back, modifier = Modifier.semantics {
                    contentDescription = if (state.menu != null) "Back to planning actions" else "Close Plan Your Day"
                }) {
                    AppIcon(if (state.menu != null) AppSymbol.Back else AppSymbol.Close, Modifier.size(22.dp))
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 26.dp)) {
                val greeting = when (state.hour) {
                    in 5..11 -> "Good morning!  ☀️"
                    in 12..17 -> "Good afternoon!  ☀️"
                    else -> "Good evening!  🌙"
                }
                Text(greeting, style = MaterialTheme.typography.titleLarge, color = Color(0xFF747679))
                Spacer(Modifier.height(8.dp))
                Text(if (state.hour in 5..11) "Start the day right with a smile." else "Make some time for what matters.",
                    style = MaterialTheme.typography.bodyMedium, color = TickTickColors.SecondaryText)
            }
            if (state.cards.isEmpty()) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (state.total == 0) "No tasks to plan" else "You're all set!", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(if (state.total == 0) "Add a task to start planning your day." else "You've reviewed all ${state.total} tasks.",
                        style = MaterialTheme.typography.bodyMedium, color = TickTickColors.SecondaryText)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = vm::close) { Text("Back to tasks") }
                    TextButton(onClick = vm::restart) { Text("Review again") }
                }
            } else key(state.cards.map { it.id }) {
                val pager = rememberPagerState(initialPage = state.selectedIndex) { state.cards.size }
                LaunchedEffect(pager.settledPage) { vm.select(state.cards[pager.settledPage].id) }
                HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 10.dp, end = 20.dp), pageSpacing = 10.dp,
                    key = { state.cards[it].id }) { page ->
                    val task = state.cards[page]
                    PlanningCard(task, state.lists.find { it.id == task.listId }, state.today,
                        "${state.resolved + page + 1}/${state.total}")
                }
                val task = state.cards[pager.settledPage]
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(min = 68.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    val enabled = !pager.isScrollInProgress && state.deleteId == null
                    when (state.menu.takeIf { state.menuTaskId == task.id }) {
                    PlanningMenu.Today -> TodayPeriod.entries.forEach { period ->
                        PlanningChoice(period.label, when (period) {
                            TodayPeriod.Morning -> "10 AM"
                            TodayPeriod.Afternoon -> "1 PM"
                            TodayPeriod.Evening -> "5 PM"
                            TodayPeriod.Night -> "9 PM"
                        }, enabled, Modifier.weight(1f)) { vm.chooseToday(task.id, period) }
                    }
                    PlanningMenu.Later -> LaterDay.entries.forEach { day ->
                        PlanningChoice(day.label, state.today.plusDays(day.days).format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)),
                            enabled, Modifier.weight(1f)) { vm.chooseLater(task.id, day) }
                    }
                    null -> {
                    PlanningAction("Done", AppSymbol.PlanDone, enabled, state.today, Modifier.weight(1f)) { vm.done(task.id) }
                    PlanningAction("Today", AppSymbol.PlanToday, enabled, state.today, Modifier.weight(1f)) { vm.today(task.id) }
                    PlanningAction("Later", AppSymbol.PlanLater, enabled, state.today, Modifier.weight(1f)) { vm.later(task.id) }
                    PlanningAction("Won't Do", AppSymbol.PlanDecline, enabled, state.today, Modifier.weight(1f)) { vm.wontDo(task.id) }
                    PlanningAction("Delete", AppSymbol.Trash, enabled, state.today, Modifier.weight(1f)) { vm.requestDelete(task.id) }
                    }
                    }
                }
            }
        }
    }
    state.deleteId?.let { id ->
        AlertDialog(onDismissRequest = vm::cancelDelete,
            title = { Text("Delete task?") },
            text = { Text(state.cards.find { it.id == id }?.title.orEmpty()) },
            confirmButton = { TextButton(onClick = vm::confirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = vm::cancelDelete) { Text("Cancel") } })
    }
    state.error?.let { error ->
        AlertDialog(onDismissRequest = vm::clearError, text = { Text(error) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } })
    }
}

@Composable
private fun PlanningChoice(label: String, detail: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 4.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val color = TickTickColors.DueTime.copy(alpha = if (enabled) 1f else .45f)
        Text(label, style = MaterialTheme.typography.bodySmall, color = color,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun PlanningCard(task: Task, list: TaskList?, today: LocalDate, progress: String) {
    Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                val date = task.dueDate
                val dateLabel = when {
                    date == null -> "No date"
                    date < today -> "${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))}, ${ChronoUnit.DAYS.between(date, today)}d overdue"
                    date == today -> "Today"
                    date == today.plusDays(1) -> "Tomorrow"
                    else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
                }
                Text(dateLabel + (task.dueTime?.let { " · ${it.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))}" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (date != null && date < today) Color(0xFFC34F58) else TickTickColors.DueTime)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val symbol = when (list?.symbol) {
                        ListSymbol.Inbox -> AppSymbol.Inbox
                        ListSymbol.Work -> AppSymbol.Work
                        ListSymbol.Personal -> AppSymbol.Home
                        else -> AppSymbol.List
                    }
                    AppIcon(symbol, Modifier.size(22.dp), TickTickColors.Checkbox)
                    Spacer(Modifier.width(10.dp))
                    Text(list?.name ?: "Inbox", style = MaterialTheme.typography.bodyLarge, color = TickTickColors.SecondaryText)
                }
                Spacer(Modifier.height(24.dp))
                Text(task.title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold))
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(task.description, style = MaterialTheme.typography.bodyLarge, color = TickTickColors.SecondaryText)
                }
            }
            Text(progress, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium, color = TickTickColors.SecondaryText)
        }
    }
}

@Composable
private fun PlanningAction(label: String, symbol: AppSymbol, enabled: Boolean, today: LocalDate,
    modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(top = 18.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val color = TickTickColors.DueTime.copy(alpha = if (enabled) 1f else .45f)
        AppIcon(symbol, Modifier.size(22.dp), color, today.dayOfMonth.toString())
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}
