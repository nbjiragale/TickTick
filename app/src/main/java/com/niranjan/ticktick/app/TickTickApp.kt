package com.niranjan.ticktick.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.core.designsystem.TickTickDimensions
import com.niranjan.ticktick.feature.tasks.TasksScreen
import com.niranjan.ticktick.feature.tasks.TasksViewModel
import kotlinx.coroutines.launch

private enum class AppTab(val label: String, val symbol: AppSymbol) {
    Tasks("Tasks", AppSymbol.Tasks), Calendar("Calendar", AppSymbol.Calendar),
    Focus("Focus", AppSymbol.Focus), Habits("Habits", AppSymbol.Habits),
}

@Composable
fun TickTickApp(container: AppContainer) {
    val factory = remember(container) {
        viewModelFactory { initializer { TasksViewModel(container.taskRepository, container.clock, createSavedStateHandle()) } }
    }
    val viewModel: TasksViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val tabState = rememberSaveableStateHolder()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Tasks) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var showOverflow by remember { mutableStateOf(false) }
    val drawerWidth = (LocalConfiguration.current.screenWidthDp.dp * (494f / 576f)).coerceAtMost(400.dp)

    fun openOverlay(name: String) {
        scope.launch {
            drawer.close()
            overlay = name
        }
    }
    fun selectTasks(action: () -> Unit) {
        action()
        selectedTab = AppTab.Tasks
        scope.launch { drawer.close() }
    }

    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
    BackHandler(enabled = !drawer.isOpen && overlay == null && selectedTab != AppTab.Tasks) { selectedTab = AppTab.Tasks }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = selectedTab == AppTab.Tasks && overlay == null,
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
                onAddList = { openOverlay("addList") },
                onManageLists = { openOverlay("manageLists") },
            )
        },
    ) {
        Box(Modifier.fillMaxSize().background(TickTickColors.Background).statusBarsPadding().navigationBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(TickTickDimensions.HeaderHeight).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { scope.launch { drawer.open() } }, modifier = Modifier.size(32.dp).semantics { contentDescription = "Open navigation drawer" }) {
                        AppIcon(AppSymbol.Menu, Modifier.size(23.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (selectedTab == AppTab.Tasks) state.title else selectedTab.label,
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (selectedTab == AppTab.Tasks) {
                        IconButton(onClick = { overlay = "suggestions" }, modifier = Modifier.size(32.dp).semantics { contentDescription = "Suggested tasks" }) {
                            AppIcon(AppSymbol.Suggestion, Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(18.dp))
                        Box {
                            IconButton(onClick = { showOverflow = true }, modifier = Modifier.size(32.dp).semantics { contentDescription = "View options" }) {
                                AppIcon(AppSymbol.More, Modifier.size(23.dp))
                            }
                            DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                                DropdownMenuItem(text = { Text(if (state.showCompleted) "Hide Completed" else "Show Completed") }, onClick = { viewModel.toggleShowCompleted(); showOverflow = false })
                                DropdownMenuItem(text = { Text(if (state.sortByTime) "Use List Order" else "Sort by Due Date") }, onClick = { viewModel.toggleSort(); showOverflow = false })
                            }
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    tabState.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        AppTab.Tasks -> TasksScreen(state, viewModel::toggleTask, { editingTaskId = it.id; overlay = "task" })
                        AppTab.Calendar -> CalendarContent(state, viewModel::toggleTask) { editingTaskId = it.id; overlay = "task" }
                        AppTab.Focus -> FocusContent()
                        AppTab.Habits -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No habits yet", color = TickTickColors.SecondaryText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    }
                }
                Row(
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
            if (selectedTab == AppTab.Tasks) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 72.dp)
                        .size(TickTickDimensions.FabSize)
                        .shadow(10.dp, CircleShape, ambientColor = TickTickColors.Accent.copy(alpha = .22f), spotColor = TickTickColors.Accent.copy(alpha = .25f))
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF3E7AFF), Color(0xFF4D76FF))))
                        .clickable(role = Role.Button, onClick = { editingTaskId = null; overlay = "task" })
                        .semantics { contentDescription = "Add task" },
                    contentAlignment = Alignment.Center,
                ) { AppIcon(AppSymbol.Plus, Modifier.size(46.dp), Color.White) }
            }
        }
    }

    AppOverlays(
        overlay = overlay, editingTaskId = editingTaskId, state = state, viewModel = viewModel,
        onDismiss = { overlay = null },
        onOpenTask = { editingTaskId = it; overlay = "task" },
        onAddList = { overlay = "addList" },
    )
}
