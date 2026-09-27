package com.niranjan.ticktick.platform.reminders

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.niranjan.ticktick.app.TickTickApplication
import com.niranjan.ticktick.core.designsystem.TickTickTheme
import com.niranjan.ticktick.domain.model.habitReminderTarget
import com.niranjan.ticktick.domain.repository.ReminderAction
import com.niranjan.ticktick.domain.repository.TaskStoreState
import com.niranjan.ticktick.domain.repository.taskWriteResult
import com.niranjan.ticktick.feature.habits.HabitCheckInScreen
import com.niranjan.ticktick.feature.reminders.ReminderHost
import com.niranjan.ticktick.feature.reminders.ReminderViewModel
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A separate, transient task: closing an alarm returns to the app that was underneath it. */
class ReminderPopupActivity : ComponentActivity() {
    private var incoming by mutableStateOf(emptyList<Pair<String, Long>>())
    private val container get() = (application as TickTickApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        val restoredIds = savedInstanceState?.getStringArrayList("popupRequestIds").orEmpty()
        val restoredRevisions = savedInstanceState?.getLongArray("popupRequestRevisions")
        incoming = restoredIds.mapIndexedNotNull { index, id -> restoredRevisions?.getOrNull(index)?.let { id to it } }
        readIntent(intent)
        setContent { TickTickTheme { PopupContent() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("popupRequestIds", ArrayList(incoming.map { it.first }))
        outState.putLongArray("popupRequestRevisions", incoming.map { it.second }.toLongArray())
        super.onSaveInstanceState(outState)
    }

    private fun readIntent(intent: Intent) {
        intent.getStringExtra(EXTRA_DELIVERY)?.let {
            incoming = (incoming + (it to intent.getLongExtra(EXTRA_REVISION, -1))).distinct()
        }
    }

    @Composable
    private fun PopupContent() {
        val store by container.taskRepository.storeState.collectAsStateWithLifecycle()
        val alerts by container.reminderRepository.reminderState.collectAsStateWithLifecycle()
        val habits by container.habitRepository.snapshot.collectAsStateWithLifecycle()
        val lifecycleState by lifecycle.currentStateAsState()
        val mainVisible by container.reminderController.mainVisible.collectAsStateWithLifecycle()
        val factory = remember {
            viewModelFactory { initializer { ReminderViewModel(container.taskRepository, container.clock,
                createSavedStateHandle(), container.reminderRepository, requestedOnly = true) } }
        }
        val vm: ReminderViewModel = viewModel(factory = factory)
        val confirmation by vm.confirmation.collectAsStateWithLifecycle()
        var accepted by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
        var handled by rememberSaveable { mutableStateOf(false) }
        var handledRequests by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
        var error by remember { mutableStateOf<String?>(null) }
        var writing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(incoming, store, lifecycleState) {
            if (store != TaskStoreState.Ready || lifecycleState != Lifecycle.State.RESUMED) return@LaunchedEffect
            incoming.forEach { (id, revision) ->
                val requestKey = "$id:$revision"
                if (requestKey in handledRequests) return@forEach
                val alert = container.reminderRepository.reminderState.value.alerts.firstOrNull { it.id == id && it.revision == revision }
                if (alert != null && !alert.missed) {
                    if (habitReminderTarget(alert.taskId) != null || vm.enqueueDelivery(id, revision)) {
                        if (id !in accepted) accepted = ArrayList(accepted + id)
                    }
                }
                handledRequests = ArrayList(handledRequests + requestKey)
            }
            handled = true
        }
        val active = alerts.alerts.filter { alert ->
            val target = habitReminderTarget(alert.taskId)
            alert.id in accepted && !alert.missed &&
                (target == null || habits.habits.any { it.id == target.habitId && it.autoPopUp })
        }
        val awaitingRequest = incoming.any { (id, revision) -> "$id:$revision" !in handledRequests }
        LaunchedEffect(handled, active, confirmation, mainVisible, store, awaitingRequest) {
            if (mainVisible || store is TaskStoreState.Failed) finishAndRemoveTask()
            else if (store == TaskStoreState.Ready && handled && !awaitingRequest && active.isEmpty() && confirmation == null) {
                // The committed delivery can disappear just before the ViewModel publishes
                // its snooze confirmation. Give that publication time to cancel this effect.
                delay(250)
                finishAndRemoveTask()
            }
        }
        fun write(action: suspend () -> Unit) {
            if (writing) return
            writing = true
            scope.launch {
                try { taskWriteResult(action).onFailure { error = it.message ?: "Couldn't update this reminder." } }
                finally { writing = false }
            }
        }
        if (lifecycleState == Lifecycle.State.RESUMED) {
            val habitAlert = active.firstOrNull { habitReminderTarget(it.taskId) != null }
            val target = habitAlert?.let { habitReminderTarget(it.taskId) }
            val habit = target?.let { target -> habits.habits.firstOrNull { it.id == target.habitId } }
            // Task cards take priority; subsequent deliveries remain queued in this window.
            if (active.any { habitReminderTarget(it.taskId) == null }) ReminderHost(vm)
            else if (habit != null) key(habitAlert.id) {
                HabitCheckInScreen(habit, target.date, LocalDate.now(container.clock), habits,
                    onBack = { write {
                        check(container.reminderRepository.actOnReminder(habitAlert.id, habitAlert.revision, ReminderAction.Dismiss)) {
                            "This reminder is no longer active."
                        }
                    } },
                    onEdit = {}, onArchive = {}, onDelete = {}, showManagementActions = false,
                    onProgress = { amount -> taskWriteResult {
                        container.habitRepository.setProgress(habit.id, target.date, amount, habit.revision)
                    } })
            } else ReminderHost(vm) // Keep the two-second snooze confirmation visible.
        }
        error?.let { message -> AlertDialog(onDismissRequest = { error = null }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }) }
    }
}
