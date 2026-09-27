package com.niranjan.ticktick.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.core.designsystem.TickTickDimensions
import com.niranjan.ticktick.feature.calendar.CalendarScreen
import com.niranjan.ticktick.feature.tasks.TasksScreen
import com.niranjan.ticktick.feature.tasks.TaskOverflowMenu
import com.niranjan.ticktick.feature.tasks.TaskBackgroundSheet
import com.niranjan.ticktick.feature.tasks.TaskBackground
import com.niranjan.ticktick.feature.tasks.TaskGroupSortSheet
import com.niranjan.ticktick.feature.tasks.DailyAlertsToggle
import com.niranjan.ticktick.feature.habits.HabitsScreen
import com.niranjan.ticktick.feature.tasks.TasksViewModel
import com.niranjan.ticktick.feature.tasks.TaskSelectionViewModel
import com.niranjan.ticktick.feature.tasks.TaskSelectionToolbar
import com.niranjan.ticktick.feature.tasks.TaskSelectionOverlays
import com.niranjan.ticktick.feature.focus.FocusViewModel
import com.niranjan.ticktick.feature.focus.FocusScreen
import com.niranjan.ticktick.feature.focus.FocusBackground
import com.niranjan.ticktick.feature.planning.PlanYourDayHost
import com.niranjan.ticktick.feature.planning.PlanYourDayViewModel
import com.niranjan.ticktick.feature.taskeditor.TaskEditorHost
import com.niranjan.ticktick.feature.taskeditor.TaskEditorViewModel
import com.niranjan.ticktick.feature.reminders.ReminderHost
import com.niranjan.ticktick.feature.reminders.ReminderViewModel
import com.niranjan.ticktick.feature.reminders.snoozeLabel
import com.niranjan.ticktick.domain.model.TaskFilter
import com.niranjan.ticktick.domain.model.habitReminderTarget
import com.niranjan.ticktick.domain.repository.taskWriteResult
import com.niranjan.ticktick.domain.repository.ReminderAction
import com.niranjan.ticktick.domain.repository.ReminderAlert
import kotlinx.coroutines.launch

private enum class AppTab(val label: String, val symbol: AppSymbol) {
    Tasks("Tasks", AppSymbol.Tasks), Calendar("Calendar", AppSymbol.Calendar),
    Habits("Habits", AppSymbol.Habits), Focus("Timer", AppSymbol.TimerRing),
}

@Composable
fun TickTickApp(container: AppContainer, reminderRequest: com.niranjan.ticktick.platform.reminders.ReminderOpenRequest? = null, onReminderRequestHandled: () -> Unit = {}) {
    val storeState by container.taskRepository.storeState.collectAsStateWithLifecycle()
    val uiStoreState by container.uiStateRepository.storeState.collectAsStateWithLifecycle()
    if (storeState != com.niranjan.ticktick.domain.repository.TaskStoreState.Ready || uiStoreState != com.niranjan.ticktick.domain.repository.TaskStoreState.Ready) {
        Box(Modifier.fillMaxSize().background(TickTickColors.Background), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val failure = (storeState as? com.niranjan.ticktick.domain.repository.TaskStoreState.Failed)
                    ?: (uiStoreState as? com.niranjan.ticktick.domain.repository.TaskStoreState.Failed)
                if (failure == null) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Text("Loading saved data...", Modifier.padding(top = 16.dp))
                } else {
                    Text(failure.message)
                    androidx.compose.material3.TextButton(onClick = { container.taskRepository.retryLoad(); container.uiStateRepository.retryLoad() }) { Text("Retry") }
                }
            }
        }
        return
    }
    val factory = remember(container) {
        viewModelFactory { initializer { TasksViewModel(container.taskRepository, container.clock, createSavedStateHandle(), container.reminderRepository, container.uiStateRepository) } }
    }
    val viewModel: TasksViewModel = viewModel(factory = factory)
    val focusFactory = remember { viewModelFactory { initializer { FocusViewModel(createSavedStateHandle()) } } }
    val focus: FocusViewModel = viewModel(factory = focusFactory)
    val focusState by focus.uiState.collectAsStateWithLifecycle()
    val selectionFactory = remember(container) {
        viewModelFactory { initializer { TaskSelectionViewModel(container.taskRepository, container.clock, createSavedStateHandle()) } }
    }
    val taskSelection: TaskSelectionViewModel = viewModel(factory = selectionFactory)
    val selectionState by taskSelection.uiState.collectAsStateWithLifecycle()
    val selectionBusy by taskSelection.busy.collectAsStateWithLifecycle()
    val selectionError by taskSelection.writeError.collectAsStateWithLifecycle()
    val taskError by viewModel.writeError.collectAsStateWithLifecycle()
    var selectionPanel by rememberSaveable { mutableStateOf<String?>(null) }
    val editorFactory = remember(container) {
        viewModelFactory { initializer { TaskEditorViewModel(container.taskRepository, container.clock, createSavedStateHandle(), container.uiStateRepository) } }
    }
    val editor: TaskEditorViewModel = viewModel(factory = editorFactory)
    val planningFactory = remember(container) {
        viewModelFactory { initializer { PlanYourDayViewModel(container.taskRepository, container.clock, container.uiStateRepository) } }
    }
    val planning: PlanYourDayViewModel = viewModel(factory = planningFactory)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, viewModel, planning) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) { viewModel.refreshTime(); planning.refreshTime() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val reminderFactory = remember(container) {
        viewModelFactory { initializer { ReminderViewModel(container.taskRepository, container.clock, createSavedStateHandle(), container.reminderRepository) } }
    }
    val reminders: ReminderViewModel = viewModel(factory = reminderFactory)
    var reminderEntryError by remember { mutableStateOf(false) }
    if (reminderEntryError) androidx.compose.material3.AlertDialog(onDismissRequest = { reminderEntryError = false },
        text = { Text("This reminder is no longer active. The task or habit may have changed or been removed.") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { reminderEntryError = false }) { Text("OK") } })
    val planningState by planning.uiState.collectAsStateWithLifecycle()
    val editorState by editor.uiState.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val habitSnapshot by container.habitRepository.snapshot.collectAsStateWithLifecycle()
    val holidaySnapshot by container.holidayRepository.snapshot.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val tabState = rememberSaveableStateHolder()
    var selectedTab by rememberSaveable { mutableStateOf(if (!editorState.active && container.uiStateRepository.currentDraft("habit")?.payload != null) AppTab.Habits else AppTab.Tasks) }
    val selecting = selectedTab == AppTab.Tasks && selectionState.active
    LaunchedEffect(selectionState.active, selectionState.tasks.isEmpty()) {
        if (!selectionState.active || selectionState.tasks.isEmpty()) selectionPanel = null
    }
    var requestedHabitId by rememberSaveable { mutableStateOf<String?>(null) }
    var requestedHabitDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var returnToTasksFromHabit by rememberSaveable { mutableStateOf(false) }
    var autoHabitOpen by rememberSaveable { mutableStateOf(false) }
    val reminderSnapshot by container.reminderRepository.reminderState.collectAsStateWithLifecycle()
    suspend fun openHabitAlert(alert: ReminderAlert, autoSound: Boolean = false): Boolean {
        val target = habitReminderTarget(alert.taskId) ?: return false
        if (!container.reminderRepository.actOnReminder(alert.id, alert.revision, ReminderAction.Dismiss)) return false
        autoHabitOpen = true
        requestedHabitId = target.habitId
        requestedHabitDay = target.date.toEpochDay()
        returnToTasksFromHabit = selectedTab == AppTab.Tasks
        selectedTab = AppTab.Habits
        if (autoSound) container.sounds.play(com.niranjan.ticktick.domain.model.AppSound.Popup)
        return true
    }
    LaunchedEffect(reminderRequest, lifecycleState) {
        if (lifecycleState != androidx.lifecycle.Lifecycle.State.RESUMED) return@LaunchedEffect
        reminderRequest?.let { request ->
            val alert = container.reminderRepository.reminderState.value.alerts.firstOrNull {
                if (request.habitSummary) habitReminderTarget(it.taskId) != null
                else it.id == request.id && it.revision == request.revision
            }
            taskWriteResult {
                reminderEntryError = if (alert != null && habitReminderTarget(alert.taskId) != null) !openHabitAlert(alert)
                    else if (request.habitSummary) true else !reminders.openDelivery(request.id, request.revision, request.changeDate)
            }.onFailure { reminderEntryError = true }
            onReminderRequestHandled()
        }
    }
    LaunchedEffect(reminderSnapshot.alerts, autoHabitOpen, reminderRequest, lifecycleState) {
        if (lifecycleState == androidx.lifecycle.Lifecycle.State.RESUMED && !autoHabitOpen && reminderRequest == null) {
            val alert = reminderSnapshot.alerts.firstOrNull { alert ->
                val target = habitReminderTarget(alert.taskId)
                !alert.missed && target != null && habitSnapshot.habits.any { it.id == target.habitId && it.autoPopUp }
            }
            if (alert != null) taskWriteResult { openHabitAlert(alert, autoSound = true) }.onFailure { reminderEntryError = true }
        }
    }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    var showOverflow by remember { mutableStateOf(false) }
    var taskOptionsSheet by rememberSaveable { mutableStateOf<String?>(null) }
    val drawerWidth = (LocalConfiguration.current.screenWidthDp.dp * (494f / 576f)).coerceAtMost(400.dp)

    fun openOverlay(name: String) {
        scope.launch {
            drawer.close()
            overlay = name
        }
    }
    fun selectTasks(action: () -> Unit) {
        taskSelection.close()
        action()
        selectedTab = AppTab.Tasks
        scope.launch { drawer.close() }
    }

    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
    BackHandler(enabled = selecting && selectionPanel == null) { taskSelection.close() }
    BackHandler(enabled = !drawer.isOpen && overlay == null && selectedTab != AppTab.Tasks) { selectedTab = AppTab.Tasks }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = selectedTab == AppTab.Tasks && !selecting && overlay == null && !editorState.active && !planningState.active,
        scrimColor = Color.Black.copy(alpha = .76f),
        drawerContent = {
            NavigationDrawer(
                width = drawerWidth,
                state = state,
                onToday = { selectTasks { viewModel.selectToday() } },
                onList = { id -> selectTasks { viewModel.selectList(id) } },
                onSearch = { openOverlay("search") },
                onNotifications = { openOverlay("reminders") },
                onSettings = { openOverlay("settings") },
                onAdd = ::openOverlay,
                onTag = { name -> selectTasks { viewModel.selectTag(name) } },
                onFilter = { id -> selectTasks { viewModel.selectFilter(id) } },
            )
        },
    ) {
        Box(Modifier.fillMaxSize().background(if (selectedTab == AppTab.Focus) FocusBackground else TickTickColors.Background).statusBarsPadding().navigationBarsPadding()) {
            if (selectedTab == AppTab.Tasks) TaskBackground(state.presentation)
            Column(Modifier.fillMaxSize()) {
                if (selectedTab != AppTab.Habits && selectedTab != AppTab.Focus) {
                Row(
                    Modifier.fillMaxWidth().height(TickTickDimensions.HeaderHeight).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { if (selecting) taskSelection.close() else scope.launch { drawer.open() } },
                        modifier = Modifier.size(32.dp).semantics { contentDescription = if (selecting) "Exit selection" else "Open navigation drawer" }) {
                        AppIcon(if (selecting) AppSymbol.Back else AppSymbol.Menu, Modifier.size(23.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (selecting) "${selectionState.tasks.size} Selected" else if (selectedTab == AppTab.Tasks) state.title else selectedTab.label,
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (selecting) {
                        IconButton(onClick = { taskSelection.selectAll(state.tasks) }, modifier = Modifier.semantics { contentDescription = "Select all tasks" }) {
                            AppIcon(AppSymbol.PlanDone, Modifier.size(23.dp))
                        }
                    }
                    if (selectedTab == AppTab.Tasks && !selecting) {
                        if (state.filter == TaskFilter.Today) {
                        DailyAlertsToggle(state.dailyAlertsEnabled, state.today, state.alertsResumeDate,
                            viewModel::setDailyAlertsEnabled, viewModel::pauseAlertsForDays)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = planning::open, modifier = Modifier.size(32.dp).semantics { contentDescription = "Plan Your Day" }) {
                            AppIcon(AppSymbol.Suggestion, Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(18.dp))
                        }
                        Box {
                            IconButton(onClick = { showOverflow = true }, modifier = Modifier.size(32.dp).semantics { contentDescription = "Task list options" }) {
                                AppIcon(AppSymbol.More, Modifier.size(23.dp))
                            }
                            if (showOverflow) TaskOverflowMenu(state, onDismiss = { showOverflow = false },
                                onChange = viewModel::updatePresentation, onCompleted = viewModel::toggleShowCompleted,
                                onBackground = { taskOptionsSheet = "background" }, onGroupSort = { taskOptionsSheet = "groupSort" },
                                onSelect = { taskSelection.start() })
                        }
                    }
                }
                }
                Box(Modifier.weight(1f)) {
                    tabState.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        AppTab.Tasks -> TasksScreen(state,
                            { if (selecting) taskSelection.toggle(it.id) else viewModel.toggleTask(it) },
                            { if (selecting) taskSelection.toggle(it.id) else editor.beginExisting(it.id) },
                            onHomeSection = { taskSelection.close(); viewModel.selectHomeSection(it) }, habits = habitSnapshot,
                            onOpenHabit = { habit, date ->
                                returnToTasksFromHabit = true
                                requestedHabitId = habit.id; requestedHabitDay = date.toEpochDay(); selectedTab = AppTab.Habits
                            }, onEditDate = { editor.beginSchedule(it.id) }, selecting = selecting, selectedIds = selectionState.ids,
                            onLongPress = { if (selecting) taskSelection.toggle(it.id) else taskSelection.start(it.id) })
                        AppTab.Calendar -> CalendarScreen(state, habitSnapshot, holidaySnapshot, container.clock, container.uiStateRepository,
                            onToggleTask = viewModel::toggleTask, onOpenTask = { editor.beginExisting(it.id) },
                            onOpenHabit = { habit, date -> returnToTasksFromHabit = false; requestedHabitId = habit.id; requestedHabitDay = date.toEpochDay(); selectedTab = AppTab.Habits },
                            onToggleShowCompleted = viewModel::toggleCalendarCompleted)
                        AppTab.Focus -> FocusScreen(focusState, focus, state.snapshot.tasks)
                        AppTab.Habits -> HabitsScreen(container.sounds, container.habitRepository, container.clock, container.uiStateRepository, requestedHabitId, requestedHabitDay,
                            onExternalDetailClosed = {
                                autoHabitOpen = false
                                if (returnToTasksFromHabit) selectedTab = AppTab.Tasks
                                returnToTasksFromHabit = false
                            }, onRequestHandled = { requestedHabitId = null; requestedHabitDay = null }, currentDate = state.today)
                    }
                    }
                }
                if (selectedTab != AppTab.Focus || !focusState.session.expanded) {
                AnimatedContent(targetState = selecting, transitionSpec = {
                    (slideInVertically(tween(260)) { it } + fadeIn(tween(180))) togetherWith
                        (slideOutVertically(tween(200)) { it } + fadeOut(tween(120)))
                }, label = "Selection toolbar") { showSelection ->
                if (showSelection) TaskSelectionToolbar(selectionState.tasks.isNotEmpty() && !selectionBusy) { action ->
                    if (action == "delete") { selectionPanel = null; taskSelection.delete() } else selectionPanel = action
                } else Row(
                    Modifier.fillMaxWidth().height(TickTickDimensions.BottomBarHeight).padding(top = 8.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppTab.entries.forEach { tab ->
                        Box(
                            Modifier.weight(1f).height(56.dp)
                                .selectable(selectedTab == tab, role = Role.Tab, onClick = { selectedTab = tab })
                                .semantics { contentDescription = tab.label },
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIcon(tab.symbol, Modifier.size(24.dp), if (selectedTab == tab) TickTickColors.Accent else TickTickColors.NavigationInactive, state.today.dayOfMonth.toString())
                        }
                    }
                }
                }
                }
            }
            AnimatedVisibility(visible = selectedTab == AppTab.Tasks && !selecting && selectionState.canUndo,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 76.dp),
                enter = slideInVertically(tween(260)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(120))) {
                Box(Modifier.size(48.dp)
                    .clip(CircleShape).background(Color(0xFFFFC400))
                    .clickable(role = Role.Button, onClick = taskSelection::undo)
                    .semantics { contentDescription = "Undo task deletion" }, contentAlignment = Alignment.Center) {
                    AppIcon(AppSymbol.Undo, Modifier.size(27.dp), Color.White)
                }
            }
            if (selectedTab == AppTab.Tasks && !selecting) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 72.dp)
                        .size(TickTickDimensions.FabSize)
                        .shadow(10.dp, CircleShape, ambientColor = TickTickColors.Accent.copy(alpha = .22f), spotColor = TickTickColors.Accent.copy(alpha = .25f))
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF3E7AFF), Color(0xFF4D76FF))))
                        .clickable(role = Role.Button, onClick = {
                            editor.beginNew((state.filter as? TaskFilter.ListFilter)?.listId ?: "inbox", if (state.filter == TaskFilter.Today) state.today else null)
                        })
                        .semantics { contentDescription = "Add task" },
                    contentAlignment = Alignment.Center,
                ) { AppIcon(AppSymbol.Plus, Modifier.size(46.dp), Color.White) }
            }
        }
    }

    val writeError = selectionError ?: taskError
    if (writeError != null) {
        val clear = { taskSelection.clearWriteError(); viewModel.clearWriteError() }
        androidx.compose.material3.AlertDialog(onDismissRequest = clear, text = { Text(writeError) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = clear) { Text("OK") } })
    }
    TaskSelectionOverlays(selectionPanel, { selectionPanel = it }, selectionState, state, taskSelection)
    when (taskOptionsSheet) {
        "background" -> TaskBackgroundSheet(state.presentation, viewModel::updatePresentation) { taskOptionsSheet = null }
        "groupSort" -> TaskGroupSortSheet(state.presentation, viewModel::updatePresentation) { taskOptionsSheet = null }
    }
    AppOverlays(
        sounds = container.sounds,
        overlay = overlay, state = state, viewModel = viewModel, reminderController = container.reminderController,
        onDismiss = { overlay = null },
        onOpenTask = { overlay = null; editor.beginExisting(it) },
        onOrganizationSaved = { selectedTab = AppTab.Tasks },
        onPreviewReminder = if (reminderPreviewEnabled) ({ id -> overlay = null; reminders.deliver(id) }) else null,
        onDelayedReminder = if (reminderPreviewEnabled) ({ id -> overlay = null; reminders.previewAfterDelay(id) }) else null,
        onPreviewAllReminders = if (reminderPreviewEnabled) ({
            overlay = null
            state.snapshot.tasks.filter { it.isActive && !it.isNote && it.dueDate != null && (it.hasReminder || it.dueTime != null) }
                .forEach { reminders.deliver(it.id) }
        }) else null,
        snoozeSummary = { id -> state.snapshot.snoozedUntil[id]?.let { "Snoozed until ${it.snoozeLabel(container.clock)}" } },
    )
    TaskEditorHost(editor, state.today)
    PlanYourDayHost(planningState, planning)
    if (lifecycleState == androidx.lifecycle.Lifecycle.State.RESUMED) ReminderHost(reminders)
    val draftError by container.uiStateRepository.draftError.collectAsStateWithLifecycle()
    var draftErrorHidden by remember(draftError) { mutableStateOf(false) }
    draftError?.takeUnless { draftErrorHidden }?.let { message -> androidx.compose.material3.AlertDialog(onDismissRequest = { draftErrorHidden = true },
        text = { Text(message) }, confirmButton = {
            androidx.compose.material3.TextButton(onClick = container.uiStateRepository::retryDrafts) { Text("Retry") }
        }, dismissButton = { androidx.compose.material3.TextButton(onClick = { draftErrorHidden = true }) { Text("Keep editing") } }) }
}
