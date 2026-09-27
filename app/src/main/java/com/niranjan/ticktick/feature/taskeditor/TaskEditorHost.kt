package com.niranjan.ticktick.feature.taskeditor

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.feature.taskeditor.schedule.SchedulePicker
import com.niranjan.ticktick.feature.reminders.snoozeLabel
import java.time.Instant
import java.time.LocalDate

@Composable
fun TaskEditorHost(vm: TaskEditorViewModel, today: LocalDate) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snapshot by vm.lists.collectAsStateWithLifecycle()
    if (!state.active) return
    if (state.recoveryFailed) {
        AlertDialog(onDismissRequest = {}, title = { Text("Draft recovery") }, text = { Text(state.error.orEmpty()) },
            confirmButton = { TextButton(onClick = vm::discard) { Text("Discard draft") } })
        return
    }
    if (state.panel == EditorPanel.Date) {
        SchedulePicker(state.schedule, vm.clock, vm::cancelSchedule, vm::applySchedule)
        if (state.error != null) AlertDialog(onDismissRequest = vm::clearError, text = { Text(state.error!!) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("Keep editing") } },
            dismissButton = { TextButton(onClick = vm::requestDiscard) { Text("Discard draft") } })
        return
    }
    val attachmentAction = rememberAttachmentActions(vm)
    fun chooseImage() { attachmentAction(AttachmentSource.ChoosePhoto) }

    Dialog(
        onDismissRequest = vm::back,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val keyboard = LocalSoftwareKeyboardController.current
        val focus = LocalFocusManager.current
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val titleFocus = remember { FocusRequester() }
        fun enterTag(tag: String? = null) {
            vm.beginTagEntry(tag)
            titleFocus.requestFocus()
            keyboard?.show()
        }
        fun enterList() {
            vm.beginListEntry()
            titleFocus.requestFocus()
            keyboard?.show()
        }
        SideEffect {
            window?.let {
                it.setDimAmount(if (state.fullScreen) 0f else .32f)
                // Needed for IME insets on older supported Android versions.
                @Suppress("DEPRECATION")
                it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightNavigationBars = true
            }
        }
        LaunchedEffect(state.id, state.fullScreen) {
            if (!state.existing) {
                titleFocus.requestFocus()
                keyboard?.show()
            }
        }
        BackHandler {
            when {
                state.panel != null -> vm.panel(null)
                imeVisible -> { keyboard?.hide(); focus.clearFocus() }
                else -> vm.back()
            }
        }
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .then(if (state.fullScreen) Modifier.background(Color.White) else Modifier)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        ) {
            val statusInset = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
            val menuMaxHeight = (maxHeight - statusInset - 16.dp).coerceAtLeast(48.dp)
            if (state.fullScreen) {
                FullScreenEditor(state, vm, today, snapshot.lists, titleFocus, ::chooseImage, attachmentAction, ::enterTag, menuMaxHeight,
                    snapshot.snoozedUntil[state.id].takeIf { state.existing && !state.completed && !state.declined && !state.isNote })
            } else {
                Box(Modifier.fillMaxSize().pointerInput(state.panel) {
                    detectTapGestures { if (state.panel != null) vm.panel(null) else vm.back() }
                })
                Box(Modifier.align(Alignment.BottomCenter)) {
                    QuickAddSheet(state, vm, today, snapshot.lists.find { it.id == state.listId }, titleFocus, ::chooseImage, { enterTag() }, ::enterList, menuMaxHeight, Modifier)
                    if (state.panel == EditorPanel.Lists) ListMenu(snapshot.lists, state.listId, true, menuMaxHeight, vm::setList, { vm.panel(null) })
                }
            }
        }
        EditorPanels(state, vm, attachmentAction)
        if (state.confirmDiscard) AlertDialog(
            onDismissRequest = vm::keepEditing, title = { Text("Discard unsaved changes?") },
            text = { Text("Changes already saved to this task will be kept.") },
            confirmButton = { TextButton(onClick = vm::discard) { Text("Discard") } },
            dismissButton = { TextButton(onClick = vm::keepEditing) { Text("Keep editing") } },
        )
        if (state.error != null) AlertDialog(
            onDismissRequest = vm::clearError, text = { Text(state.error!!) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("Keep editing") } },
            dismissButton = { TextButton(onClick = vm::requestDiscard) { Text("Discard draft") } },
        )
    }
}

@Composable
private fun QuickAddSheet(state: TaskEditorState, vm: TaskEditorViewModel, today: LocalDate, selectedList: TaskList?, titleFocus: FocusRequester, onImage: () -> Unit, onTag: () -> Unit, onList: () -> Unit, menuMaxHeight: Dp, modifier: Modifier) {
    val showListLabel = selectedList != null && (selectedList.symbol != ListSymbol.Inbox || selectedList.tokenSpans(state.title.text).isNotEmpty())
    val listActionWidth = if (showListLabel) 104.dp else 38.dp
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(Color.White)
        .pointerInput(Unit) { detectTapGestures { } }.padding(top = 16.dp)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            EditorTextField(state.title, vm::setTitle, if (state.isNote) "What would you like to note?" else "What would you like to do?", "Task title", MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                Modifier.heightIn(min = 24.dp), state.titleHighlights(selectedList), titleFocus)
            Spacer(Modifier.height(4.dp))
            EditorTextField(state.description, vm::setDescription, "", "Description", MaterialTheme.typography.bodyMedium, Modifier.heightIn(min = 20.dp))
            if (state.checklist.isNotEmpty()) Text("${state.checklist.size} checklist items", Modifier.padding(top = 8.dp).clickable { vm.showFullScreen() }, color = EditorBlue, style = MaterialTheme.typography.bodySmall)
            AttachmentCards(state.attachments, vm::removeAttachment, vm::reportError, state.attachmentLoading)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.dueDate != null) {
                val dateColor = if (state.dueDate!!.isBefore(today)) Color(0xFFD84E57) else EditorBlue
                val maxDateWidth = (LocalConfiguration.current.screenWidthDp.dp - 176.dp - listActionWidth).coerceAtLeast(48.dp)
                Row(Modifier.widthIn(max = maxDateWidth).height(48.dp).clickable(role = Role.Button, onClick = vm::tapDate).padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppSymbol.Date, Modifier.size(21.dp), dateColor)
                    Spacer(Modifier.width(6.dp))
                    Text(state.dateLabel(today), Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, color = dateColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else EditorIconButton(AppSymbol.Date, "Date and time", { vm.panel(EditorPanel.Date) })
            Box {
                EditorIconButton(AppSymbol.Flag, "Priority", { vm.panel(EditorPanel.Priority) }, tint = state.priority.color())
                if (state.panel == EditorPanel.Priority) PriorityMenu(state.priority, true, menuMaxHeight, vm::setPriority, { vm.panel(null) })
            }
            EditorIconButton(AppSymbol.Tag, "Add tag", onTag, tint = if (state.allTags.isEmpty()) EditorMuted else EditorBlue)
            if (showListLabel) {
                Row(Modifier.widthIn(max = listActionWidth).height(48.dp)
                    .clickable(role = Role.Button, onClick = onList)
                    .semantics { contentDescription = "Select list, ${selectedList.name}" }
                    .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    EditorListIcon(selectedList.symbol)
                    Spacer(Modifier.width(6.dp))
                    Text(selectedList.name, Modifier.weight(1f, fill = false), color = EditorBlue,
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else EditorIconButton(AppSymbol.Move, "Select list", onList)
            Box {
                EditorIconButton(AppSymbol.MoreHorizontal, "More task options", { vm.panel(EditorPanel.More) })
                EditorMoreMenu(state, vm, onImage)
            }
            Spacer(Modifier.weight(1f))
            if (state.title.text.isBlank()) EditorIconButton(AppSymbol.Microphone, "Voice input", { vm.panel(EditorPanel.Voice) })
            else Box(Modifier.size(48.dp).semantics { contentDescription = "Save task" }.clickable(enabled = !state.saving, role = Role.Button, onClick = vm::saveAndClose), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp, 30.dp).background(TickTickColors.Accent, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                    AppIcon(AppSymbol.Send, Modifier.size(24.dp), Color.White)
                }
            }
        }
    }
}

@Composable
private fun FullScreenEditor(state: TaskEditorState, vm: TaskEditorViewModel, today: LocalDate, lists: List<TaskList>, titleFocus: FocusRequester, onImage: () -> Unit, onAttachment: (AttachmentSource) -> Unit, onTag: (String?) -> Unit, menuMaxHeight: Dp, snoozedUntil: Instant?) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            EditorIconButton(AppSymbol.Back, "Back", vm::back, tint = TickTickColors.Text)
            Box(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth().height(48.dp).clickable(role = Role.Button) { vm.panel(EditorPanel.Lists) }.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(lists.find { it.id == state.listId }?.name ?: "Inbox", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    AppIcon(AppSymbol.Chevrons, Modifier.padding(start = 4.dp).size(15.dp), EditorMuted)
                }
                if (state.panel == EditorPanel.Lists) ListMenu(lists, state.listId, false, menuMaxHeight, vm::setList, { vm.panel(null) })
            }
            if (!state.existing) TextButton(onClick = vm::saveAndClose, enabled = state.title.text.isNotBlank() && !state.saving) { Text("Save") }
            Box {
                EditorIconButton(AppSymbol.Flag, "Priority", { vm.panel(EditorPanel.Priority) }, tint = if (state.priority == com.niranjan.ticktick.domain.model.TaskPriority.None) TickTickColors.Text else state.priority.color())
                if (state.panel == EditorPanel.Priority) PriorityMenu(state.priority, false, menuMaxHeight, vm::setPriority, { vm.panel(null) })
            }
            Spacer(Modifier.width(10.dp))
            Box {
                EditorIconButton(AppSymbol.More, "More task options", { vm.panel(EditorPanel.More) }, tint = TickTickColors.Text)
                EditorMoreMenu(state, vm, onImage)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp).heightIn(min = 40.dp),
                verticalAlignment = if (snoozedUntil != null) Alignment.Top else Alignment.CenterVertically) {
                if (state.declined) EditorIconButton(AppSymbol.PlanDecline, "Restore task marked Won't Do", vm::toggleComplete)
                else if (!state.isNote) EditorCheckbox(state.completed, if (state.completed) "Restore task" else "Complete task", vm::toggleComplete)
                else Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.panel(EditorPanel.Date) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(state.dateLabel(today, full = true), style = MaterialTheme.typography.bodyLarge, color = if (state.dueDate == null) EditorMuted else EditorBlue, modifier = Modifier.weight(1f, fill = false))
                        if (state.hasReminder) AppIcon(AppSymbol.Alarm, Modifier.padding(start = 6.dp).size(16.dp), EditorBlue)
                        if (state.repeat != com.niranjan.ticktick.domain.model.RepeatRule()) AppIcon(AppSymbol.Repeat, Modifier.padding(start = 6.dp).size(16.dp), EditorBlue)
                    }
                    if (snoozedUntil != null) Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(AppSymbol.Snooze, Modifier.size(16.dp), EditorBlue)
                        Text("Snoozed until ${snoozedUntil.snoozeLabel(vm.clock)}", Modifier.padding(start = 6.dp).weight(1f),
                            style = MaterialTheme.typography.bodyMedium, color = EditorBlue)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                if (state.declined) Text("Won't Do", color = TickTickColors.SecondaryText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                EditorTextField(state.title, vm::setTitle, "Title", "Task title", MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold),
                    highlights = state.titleHighlights(lists.find { it.id == state.listId }), focusRequester = titleFocus)
                Spacer(Modifier.height(12.dp))
                EditorTextField(state.description, vm::setDescription, "", "Description", MaterialTheme.typography.bodyLarge, Modifier.heightIn(min = 40.dp))
                EditorExtraContent(state, vm, onTag)
            }
        }
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            EditorIconButton(AppSymbol.Tag, "Add tag", { onTag(null) }, Modifier.width(46.dp), tint = if (state.allTags.isEmpty()) Color(0xFF8D9092) else EditorBlue)
            EditorIconButton(AppSymbol.List, "Checklist", vm::toggleChecklist, Modifier.width(46.dp), tint = Color(0xFF8D9092))
            Box {
                EditorIconButton(AppSymbol.Attachment, "Add attachment", { vm.panel(EditorPanel.Attachments) }, Modifier.width(46.dp), tint = Color(0xFF8D9092))
                if (state.panel == EditorPanel.Attachments) AttachmentMenu(menuMaxHeight, onAttachment, { vm.panel(null) })
            }
        }
    }
}
