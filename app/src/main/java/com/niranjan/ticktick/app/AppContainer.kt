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
    val sounds = com.niranjan.ticktick.platform.sounds.AppSounds(application, uiStateRepository, habitRepository, applicationScope)
    val reminderController = ReminderController(application, taskRepository, reminderRepository, habitRepository, clock, applicationScope, sounds)
    init {
        taskStore.onReminderChange = { reminderController.requestReconcile() }
        taskStore.onCompletion = sounds::play
        applicationScope.launch {
            combine(taskRepository.storeState, uiStateRepository.storeState) { tasks, drafts ->
                tasks == TaskStoreState.Ready && drafts == TaskStoreState.Ready
            }.first { it }
            runCatching {
                ManagedAttachmentStore(application).cleanOldOrphans(
                    taskRepository.snapshot.value.tasks.flatMap { task -> task.attachments.map { it.uri } },
                    uiStateRepository.currentDraft("task")?.payload,
                )
            }
        }
    }
    val holidayRepository: HolidayRepository = InMemoryHolidayRepository(initialHolidaySnapshot(clock))
}

class TickTickApplication : Application() {
    val container by lazy { AppContainer(this) }
}
