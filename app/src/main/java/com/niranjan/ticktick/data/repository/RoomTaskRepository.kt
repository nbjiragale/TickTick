package com.niranjan.ticktick.data.repository

import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.repository.*
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import com.niranjan.ticktick.data.local.TickTickDatabase
import com.niranjan.ticktick.data.local.TaskStoreMetadata
import com.niranjan.ticktick.data.mapper.*
import com.niranjan.ticktick.data.mapper.writeChanges
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomTaskRepository(private val database: TickTickDatabase, private val clock: Clock,
    private val scope: CoroutineScope) : TaskRepository, ReminderRepository {
    private val mutex = Mutex()
    private val reminders = ReminderStore(database.reminders(), clock)
    private val reminderData = MutableStateFlow(ReminderState())
    override val reminderState = reminderData.asStateFlow()
    @Volatile var onReminderChange: () -> Unit = {}
    @Volatile var onCompletion: (AppSound) -> Unit = {}
    private val data = MutableStateFlow(TaskSnapshot())
    private val habitData = MutableStateFlow(HabitSnapshot())
    val habits: HabitRepository = object : HabitRepository {
        override val snapshot = habitData.asStateFlow()
        override suspend fun save(habit: Habit, draft: DraftToken?) = mutateHabit(draft) { save(habit) }
        override suspend fun setArchived(id: String, archived: Boolean) = mutateHabit { setArchived(id, archived) }
        override suspend fun delete(id: String) = mutateHabit { delete(id) }
        override suspend fun setProgress(id: String, date: java.time.LocalDate, amount: Int, expectedRevision: Long) {
            mutateHabit { setProgress(id, date, amount, expectedRevision) }
        }
        override suspend fun updateSettings(settings: HabitSettings) = mutateHabit { updateSettings(settings) }
        override suspend fun addSection(name: String) = mutateHabit { addSection(name) }
        override suspend fun moveSection(name: String, direction: Int) = mutateHabit { moveSection(name, direction) }
    }
    private val status = MutableStateFlow<TaskStoreState>(TaskStoreState.Loading)
    override val snapshot = data.asStateFlow()
    override val storeState = status.asStateFlow()
    private var loading: Job? = null

    init { retryLoad() }

    override fun retryLoad() {
        if (loading?.isActive == true) return
        loading = scope.launch {
            status.value = TaskStoreState.Loading
            try {
                // Every aggregate refresh takes the same lock as a write, preventing stale publication.
                database.invalidationTracker.createFlow("tasks", "task_lists", "task_tags", "task_tag_links",
                    "task_checklist", "task_attachments", "task_snoozes", "saved_filters", "filter_rules", "task_store_metadata", "task_recurrence", "task_occurrence_history", "habits", "habit_schedule_history", "habit_checkins", "habit_sections", "habit_settings")
                    .collect {
                        mutex.withLock {
                            val loaded = database.withTransaction {
                                val dao = database.tasks()
                                if (dao.metadata() == null) {
                                    val initial = TaskSnapshot(lists = listOf(TaskList("inbox", "Inbox", ListSymbol.Inbox)))
                                    dao.writeChanges(TaskSnapshot(), initial)
                                    dao.putMetadata(TaskStoreMetadata())
                                }
                                val stored = dao.readSnapshot()
                                val snapshot = TaskMutation(stored, clock).snapshot
                                if (stored != snapshot) dao.writeChanges(stored, snapshot)
                                database.habits().initialize()
                                val habits = database.habits().readHabits()
                                reminders.sync(snapshot, habits = habits)
                                Triple(snapshot, reminders.state(), habits)
                            }
                            data.value = loaded.first
                            reminderData.value = loaded.second
                            habitData.value = loaded.third
                            status.value = TaskStoreState.Ready
                            onReminderChange()
                        }
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                status.value = TaskStoreState.Failed("Couldn't open your saved data. Your saved data has not been reset. Please retry.")
            }
        }
    }

    private suspend fun <T> mutate(draft: DraftToken? = null, checkpoint: PreferenceCommit? = null, action: TaskMutation.() -> T): T = mutex.withLock {
        check(status.value == TaskStoreState.Ready) { "Task storage is not ready." }
        // Once a transaction starts, finish commit and publish its result even if a screen closes.
        // This prevents a committed write being reported as cancelled before acknowledgement.
        withContext(NonCancellable) {
            var completed = false
            val (result, saved, reminderSnapshot) = database.withTransaction {
                val dao = database.tasks()
                val before = dao.readSnapshot()
                val mutation = TaskMutation(before, clock)
                val result = action(mutation)
                completed = mutation.completedSomething
                if (mutation.snapshot != before) dao.writeChanges(before, mutation.snapshot)
                val snapshot = dao.readSnapshot()
                reminders.sync(snapshot, before, database.habits().readHabits())
                draft?.let { database.uiState().clearDraft(it.key, it.sessionId, it.version) }
                checkpoint?.let { database.uiState().putPreference(com.niranjan.ticktick.data.local.UiPreferenceEntity(it.key, payload = it.payload)) }
                Triple(result, snapshot, reminders.state())
            }
            data.value = saved
            reminderData.value = reminderSnapshot
            onReminderChange()
            if (completed) onCompletion(AppSound.TaskCompletion)
            result
        }
    }

    private suspend fun <T> reminderTransaction(notify: Boolean = true,
        operation: suspend (TaskSnapshot) -> T): T = mutex.withLock {
        check(status.value == TaskStoreState.Ready)
        withContext(NonCancellable) {
            val (result, saved) = database.withTransaction {
                val before = database.tasks().readSnapshot()
                reminders.sync(before, habits = database.habits().readHabits())
                val result = operation(before)
                result to Triple(database.tasks().readSnapshot(), reminders.state(), database.habits().readHabits())
            }
            data.value = saved.first
            reminderData.value = saved.second
            habitData.value = saved.third
            if (notify) onReminderChange()
            result
        }
    }

    private suspend fun <T> mutateHabit(draft: DraftToken? = null, action: HabitMutation.() -> T): T = mutex.withLock {
        check(status.value == TaskStoreState.Ready)
        withContext(NonCancellable) {
            var completed = false
            val (result, saved, alerts) = database.withTransaction {
                val before = database.habits().readHabits()
                val mutation = HabitMutation(before, clock)
                val result = action(mutation)
                completed = mutation.completedSomething
                database.habits().writeHabits(before, mutation.snapshot, clock)
                draft?.let { database.uiState().clearDraft(it.key, it.sessionId, it.version) }
                val tasks = database.tasks().readSnapshot()
                reminders.sync(tasks, tasks, mutation.snapshot, before)
                // Any check-in/Undo is an explicit action stopping this day's active re-alert cycle.
                mutation.snapshot.habits.forEach { habit ->
                    val dates = before.progress[habit.id].orEmpty().keys + mutation.snapshot.progress[habit.id].orEmpty().keys
                    dates.filter { before.progress[habit.id]?.get(it) != mutation.snapshot.progress[habit.id]?.get(it) }
                        .forEach { reminders.dismissHabitDay("habit:${habit.id}:${it.toEpochDay()}") }
                }
                Triple(result, mutation.snapshot, reminders.state())
            }
            habitData.value = saved
            reminderData.value = alerts
            onReminderChange()
            if (completed) onCompletion(AppSound.HabitCompletion)
            result
        }
    }

    override suspend fun importAlertPause(legacy: AlertPause?) = reminderTransaction { reminders.importPause(legacy) }
    override suspend fun setAlertPause(pause: AlertPause) = reminderTransaction { reminders.setPause(pause) }
    override suspend fun reconcileReminders(force: Boolean) = reminderTransaction(notify = false) { reminders.reconcile(it, force, database.habits().readHabits()) }
    override suspend fun applyReminderEffect(id: String, revision: Long, apply: () -> Boolean): Boolean = mutex.withLock {
        withContext(NonCancellable) {
            val current = database.reminders().delivery(id)
            if (current == null || current.revision != revision || !current.dirty) true
            else if (apply()) {
                database.withTransaction { reminders.acknowledge(id, revision) }
                true
            } else false
        }
    }
    override suspend fun fireReminder(id: String, revision: Long) = reminderTransaction { reminders.fire(id, revision) }
    override suspend fun actOnReminder(id: String, revision: Long, action: ReminderAction): Boolean {
        var completion: AppSound? = null
        val result = reminderTransaction { before ->
            val delivery = reminders.actionable(id, revision) ?: return@reminderTransaction false
            val target = habitReminderTarget(delivery.taskId)
            if (target != null) {
                val stored = database.habits().readHabits()
                val habit = stored.habits.find { it.id == target.habitId } ?: return@reminderTransaction false
                if (action == ReminderAction.Dismiss) reminders.dismissHabitDay(delivery.taskId)
                else if (action == ReminderAction.Done) {
                    val rule = stored.configurationOn(habit, target.date)
                    if (!stored.canRecord(habit, target.date, java.time.LocalDate.now(clock)) ||
                        rule.amount != null && habit.recordMode == HabitRecordMode.Manual) return@reminderTransaction false
                    val amount = if (rule.amount == null) 1 else (stored.progressFor(habit, target.date).toLong() + habit.recordAmount).coerceAtMost(999999).toInt()
                    val mutation = HabitMutation(stored, clock)
                    mutation.setProgress(habit.id, target.date, amount, habit.revision)
                    if (mutation.completedSomething) completion = AppSound.HabitCompletion
                    database.habits().writeHabits(stored, mutation.snapshot, clock)
                    reminders.sync(before, before, mutation.snapshot, stored)
                    reminders.dismissHabitDay(delivery.taskId)
                } else return@reminderTransaction false
                return@reminderTransaction true
            }
            val task = before.tasks.find { it.id == delivery.taskId && it.isActive && !it.isNote } ?: return@reminderTransaction false
            if (action == ReminderAction.Dismiss) reminders.dismiss(delivery)
            else {
                val mutation = TaskMutation(before, clock)
                when (action) {
                    ReminderAction.Done -> mutation.setCompleted(task.id, true, task.revision)
                    is ReminderAction.Snooze -> {
                        require(action.replay || action.until.isAfter(clock.instant())) { "Choose a future snooze time." }
                        mutation.snooze(task.id, action.until)
                    }
                    is ReminderAction.Reschedule -> {
                        require(action.schedule.validationError(clock) == null) { "Check the new reminder time." }
                        mutation.changeSchedule(task.id, action.schedule)
                    }
                    ReminderAction.Dismiss -> Unit
                }
                database.tasks().writeChanges(before, mutation.snapshot)
                if (mutation.completedSomething) completion = AppSound.TaskCompletion
                reminders.sync(mutation.snapshot, before, database.habits().readHabits())
            }
            true
        }
        if (result) completion?.let(onCompletion)
        return result
    }

    override suspend fun save(task: Task, draft: DraftToken?) = mutate(draft) { save(task) }
    override suspend fun saveTasks(tasks: List<Task>) = mutate { saveTasks(tasks) }
    override suspend fun setCompleted(id: String, completed: Boolean, expectedRevision: Long, checkpoint: PreferenceCommit?) = mutate(checkpoint = checkpoint) { setCompleted(id, completed, expectedRevision) }
    override suspend fun skipOccurrences(expectedRevisions: Map<String, Long>) = mutate { skipOccurrences(expectedRevisions) }
    override suspend fun setDeclined(id: String, declined: Boolean, checkpoint: PreferenceCommit?) = mutate(checkpoint = checkpoint) { setDeclined(id, declined) }
    override suspend fun snooze(id: String, until: Instant) = mutate { snooze(id, until) }
    override suspend fun changeSchedule(id: String, schedule: TaskSchedule, checkpoint: PreferenceCommit?) = mutate(checkpoint = checkpoint) { changeSchedule(id, schedule) }
    override suspend fun addList(name: String, color: Int?, view: TaskListView) = mutate { addList(name, color, view) }
    override suspend fun addTag(name: String, color: Int?) = mutate { addTag(name, color) }
    override suspend fun addFilter(name: String, rules: List<FilterRule>, matchAll: Boolean) = mutate { addFilter(name, rules, matchAll) }
    override suspend fun deleteTask(id: String, draft: DraftToken?, checkpoint: PreferenceCommit?) = mutate(draft, checkpoint) { deleteTask(id) }
    override suspend fun deleteTasks(ids: Set<String>) = mutate { deleteTasks(ids) }
    override suspend fun restoreTasks(deleted: DeletedTasks) = mutate { restoreTasks(deleted) }
}
