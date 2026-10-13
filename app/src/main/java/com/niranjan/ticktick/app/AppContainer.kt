package com.niranjan.ticktick.app

import android.app.Application
import com.niranjan.ticktick.data.repository.RoomTaskRepository
import com.niranjan.ticktick.data.local.TickTickDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.niranjan.ticktick.data.repository.InMemoryHolidayRepository
import com.niranjan.ticktick.domain.repository.HabitRepository
import com.niranjan.ticktick.domain.repository.HolidayRepository
import com.niranjan.ticktick.domain.repository.TaskRepository
import java.time.Clock
import com.niranjan.ticktick.core.time.DeviceClock
import com.niranjan.ticktick.domain.repository.ReminderRepository
import com.niranjan.ticktick.platform.reminders.ReminderController
import com.niranjan.ticktick.platform.attachments.ManagedAttachmentStore
import com.niranjan.ticktick.domain.repository.TaskStoreState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import androidx.room.withTransaction

class AppContainer(application: Application) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = TickTickDatabase.open(application)
    val clock: Clock = DeviceClock()
    val uiStateRepository: com.niranjan.ticktick.domain.repository.UiStateRepository =
        com.niranjan.ticktick.data.repository.RoomUiStateRepository(database, applicationScope)
    private val taskStore = RoomTaskRepository(database, clock, applicationScope)
    val taskRepository: TaskRepository = taskStore
    val reminderRepository: ReminderRepository = taskStore
    val habitRepository: HabitRepository = taskStore.habits
    val focusRepository: com.niranjan.ticktick.domain.repository.FocusRepository =
        com.niranjan.ticktick.data.repository.RoomFocusRepository(database, application, applicationScope)
    val focusController = com.niranjan.ticktick.platform.focus.FocusController(application, focusRepository, applicationScope) { database.restoring.value }
    val sounds = com.niranjan.ticktick.platform.sounds.AppSounds(application, uiStateRepository, habitRepository, applicationScope)
    val reminderController = ReminderController(application, taskRepository, reminderRepository, habitRepository, clock, applicationScope, sounds)
    // Startup cleanup must finish before the UI can create an Undo batch or start restore.
    val startupReady = kotlinx.coroutines.flow.MutableStateFlow(false)
    val restoreEpoch = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val backupManager = com.niranjan.ticktick.platform.backup.BackupManager(application, database, uiStateRepository,
        beforeReplace = { reminderController.cancelForRestore(database.reminders().deliveries().map { it.id }) },
        afterReplace = { replaced ->
            if (replaced) {
                try {
                    taskStore.reloadAfterRestore()
                    (uiStateRepository as com.niranjan.ticktick.data.repository.RoomUiStateRepository).reloadAfterRestore()
                    (focusRepository as com.niranjan.ticktick.data.repository.RoomFocusRepository).reloadAfterRestore()
                } finally { restoreEpoch.value += 1 }
            }
            reminderController.requestReconcile(force = true)
        })
    init {
        applicationScope.launch {
            uiStateRepository.snapshot.collect { snapshot ->
                com.niranjan.ticktick.core.settings.AppDisplay.preferences = com.niranjan.ticktick.core.settings.AppPreferences.decode(snapshot.preferences["app"])
            }
        }
        applicationScope.launch {
            database.restoring.collect { if (!it) { reminderController.requestReconcile(force = true); focusController.retry() } }
        }
        taskStore.onReminderChange = { reminderController.requestReconcile() }
        taskStore.onCompletion = sounds::play
        applicationScope.launch {
            combine(taskRepository.storeState, uiStateRepository.storeState) { tasks, drafts ->
                tasks == TaskStoreState.Ready && drafts == TaskStoreState.Ready
            }.first { it }
            try {
                database.operationLock.withLock {
                    // Read committed ownership under the same lock used by writes/restore.
                    // A load failure or unreadable recovery copy makes cleanup a no-op.
                    if (database.restoring.value || database.generation != 0L) return@withLock
                    val recoveryReferences = backupManager.recoveryAttachmentUris() ?: return@withLock
                    val (attachments, draft) = database.withTransaction {
                        database.tasks().attachments().map { it.uri } to database.uiState().draft("task")?.payload
                    }
                    ManagedAttachmentStore(application).cleanOldOrphans(attachments + recoveryReferences, draft)
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { /* Preserve app availability if best-effort cleanup fails. */ }
            finally { startupReady.value = true }
        }
    }
    val holidayRepository: HolidayRepository = InMemoryHolidayRepository(initialHolidaySnapshot(clock))
}

class TickTickApplication : Application() {
    val container by lazy { AppContainer(this) }
}
