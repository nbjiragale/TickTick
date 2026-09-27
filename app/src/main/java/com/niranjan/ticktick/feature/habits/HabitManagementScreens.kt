package com.niranjan.ticktick.feature.habits

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
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
import com.niranjan.ticktick.domain.model.HabitSettings
import com.niranjan.ticktick.domain.model.HabitSnapshot
import com.niranjan.ticktick.feature.taskeditor.schedule.*

@Composable
private fun HabitPage(onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.let {
            it.setDimAmount(0f)
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightNavigationBars = true
        } }
        Column(Modifier.fillMaxSize().background(TickTickColors.Background).systemBarsPadding(), content = content)
    }
}

@Composable
private fun HabitPageHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        ScheduleIcon(AppSymbol.Back, "Back", onBack)
        Text(title, Modifier.padding(start = 4.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun HabitLibrary(snapshot: HabitSnapshot, archived: Boolean, onSelectArchived: (Boolean) -> Unit, onBack: () -> Unit, onOpen: (Habit) -> Unit, onEdit: (Habit) -> Unit, onAdd: () -> Unit, onArchive: (String, Boolean) -> Unit) {
    var menuId by rememberSaveable { mutableStateOf<String?>(null) }
    val habits = snapshot.habits.filter { it.archived == archived }
    HabitPage(onBack) {
        Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 6.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            ScheduleIcon(AppSymbol.Back, "Back to Habits", onBack)
            Row(Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf(false to "Active", true to "Archived").forEach { (value, label) ->
                    val selected = archived == value
                    Column(Modifier.width(IntrinsicSize.Max).selectable(selected, role = Role.Tab, onClick = { onSelectArchived(value); menuId = null }).padding(top = 6.dp)) {
                        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (selected) TickTickColors.Text else HabitMuted)
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(3.dp).background(if (selected) ScheduleBlue else Color.Transparent, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (habits.isEmpty()) Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.Center) {
                Text(if (archived) "No archived habits" else "No active habits", color = TickTickColors.SecondaryText)
            }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 144.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(habits, key = { it.id }) { habit ->
                    Box {
                        HabitCard {
                            Row(Modifier.fillMaxWidth().heightIn(min = 68.dp)
                                .combinedClickable(role = Role.Button, onClick = { onOpen(habit) }, onLongClickLabel = "Habit actions", onLongClick = { menuId = habit.id })
                                .semantics { customActions = listOf(CustomAccessibilityAction(if (archived) "Unarchive habit" else "Archive habit") { onArchive(habit.id, !archived); true }) }
                                .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                HabitAvatar(habit.icon)
                                Text(habit.name, Modifier.padding(start = 8.dp).weight(1f), fontSize = 16.sp)
                                Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End) {
                                    Text((snapshot.completedDates[habit.id]?.size ?: 0).toString(), fontSize = 16.sp)
                                    Text("Total Day", fontSize = 13.sp, color = HabitMuted)
                                }
                            }
                        }
                        DropdownMenu(menuId == habit.id, { menuId = null }, containerColor = Color.White) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { menuId = null; onEdit(habit) })
                            DropdownMenuItem(text = { Text(if (archived) "Unarchive" else "Archive") }, onClick = { menuId = null; onArchive(habit.id, !archived) })
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 72.dp).size(56.dp).shadow(8.dp, CircleShape)
                .clip(CircleShape).combinedClickable(role = Role.Button, onClick = { onSelectArchived(false); onAdd() }).background(HabitBlue)
                .semantics { contentDescription = "Add habit" }, contentAlignment = Alignment.Center) {
                AppIcon(AppSymbol.Plus, Modifier.size(44.dp), Color.White)
            }
        }
    }
}

@Composable
internal fun HabitSettingsScreen(sounds: com.niranjan.ticktick.platform.sounds.AppSounds, settings: HabitSettings, onChange: (HabitSettings) -> Unit, onSections: () -> Unit, onBack: () -> Unit) {
    HabitPage(onBack) {
        HabitPageHeader("Habit Settings", onBack)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp)) {
            HabitCard {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    com.niranjan.ticktick.feature.settings.SoundSetting(sounds, com.niranjan.ticktick.domain.model.AppSound.Notification)
                }
                HabitToggleSetting("Sort by Check-in Status", settings.sortByCheckInStatus,
                    "If enabled, unchecked-in habits will be shown on the top of the list.") { onChange(settings.copy(sortByCheckInStatus = it)) }
                HabitSetting("Manage Section", "", onSections)
                HabitToggleSetting("Show in \"Today\" & \"Next 7 days\"", settings.showInTodayAndNext7Days) { onChange(settings.copy(showInTodayAndNext7Days = it)) }
                HabitToggleSetting("Count in the App Badge", settings.countInAppBadge) { onChange(settings.copy(countInAppBadge = it)) }
            }
            Text("Notification sound is shared with the main Settings page. Popup sound is configured separately there. Android channel settings can override sounds; badges depend on your launcher.",
                Modifier.padding(horizontal = 4.dp, vertical = 12.dp), fontSize = 12.sp, color = HabitMuted)
        }
    }

}

@Composable
private fun HabitToggleSetting(title: String, checked: Boolean, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = if (description == null) 52.dp else 88.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (description != null) Text(description, Modifier.padding(top = 3.dp, end = 8.dp), fontSize = 12.sp, lineHeight = 16.sp, color = HabitMuted)
        }
        ReferenceSwitch(checked, title, onChange)
    }
}

@Composable
internal fun HabitSections(sections: List<String>, onAdd: suspend (String) -> Result<Boolean>, onMove: (String, Int) -> Unit, onDismiss: () -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val move by rememberUpdatedState(onMove)
    val rowHeight = with(LocalDensity.current) { 50.dp.toPx() }
    HabitPage(onDismiss) {
        HabitPageHeader("Manage Section", onDismiss)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp)) {
            HabitCard {
                sections.forEachIndexed { index, section -> key(section) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(section, Modifier.weight(1f), fontSize = 16.sp)
                        Box(Modifier.size(48.dp).semantics {
                            contentDescription = "Reorder $section"
                            customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction("Move up") { onMove(section, -1); true })
                                if (index < sections.lastIndex) add(CustomAccessibilityAction("Move down") { onMove(section, 1); true })
                            }
                        }.pointerInput(section, rowHeight) {
                            var distance = 0f
                            detectDragGestures(onDragStart = { distance = 0f }) { event, delta ->
                                event.consume(); distance += delta.y
                                if (distance >= rowHeight) { move(section, 1); distance -= rowHeight }
                                else if (distance <= -rowHeight) { move(section, -1); distance += rowHeight }
                            }
                        }, contentAlignment = Alignment.Center) {
                            AppIcon(AppSymbol.Reorder, Modifier.size(22.dp), Color(0xFF929497))
                        }
                    }
                } }
                ScheduleTextButton("＋  Add Section", { adding = true; error = false }, Modifier.padding(start = 8.dp, bottom = 8.dp))
            }
        }
    }
    if (adding) ScheduleDialog("Add Section", { if (!saving) adding = false }) {
        HabitInput(name, { name = it.take(40); error = false }, "Section name", Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth())
        if (error) Text("Enter a new, unique section name.", Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        saveError?.let { Text(it, Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.error) }
        DialogActions({ if (!saving) adding = false }, {
            if (!saving) { saving = true; scope.launch {
                try { onAdd(name).fold(onSuccess = { if (it) { name = ""; adding = false } else error = true }, onFailure = { saveError = it.message }) }
                finally { saving = false }
            } }
        })
    }
}
