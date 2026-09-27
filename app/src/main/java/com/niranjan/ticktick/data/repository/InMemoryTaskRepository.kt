package com.niranjan.ticktick.data.repository

import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.repository.*
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Explicit session/demo store. The application uses RoomTaskRepository. */
class InMemoryTaskRepository(initial: TaskSnapshot, private val clock: Clock = Clock.systemDefaultZone()) : TaskRepository {
    private val mutex = Mutex()
    private val data = MutableStateFlow(TaskMutation(initial, clock).snapshot)
    override val snapshot = data.asStateFlow()
    override val storeState = MutableStateFlow<TaskStoreState>(TaskStoreState.Ready).asStateFlow()
    override fun retryLoad() = Unit
    private suspend fun <T> mutate(action: TaskMutation.() -> T): T = mutex.withLock {
        val mutation = TaskMutation(data.value, clock)
        action(mutation).also { data.value = mutation.snapshot }
    }

    override suspend fun save(task: Task, draft: DraftToken?) = mutate { save(task) }
    override suspend fun saveTasks(tasks: List<Task>) = mutate { saveTasks(tasks) }
    override suspend fun setCompleted(id: String, completed: Boolean, expectedRevision: Long, checkpoint: PreferenceCommit?) = mutate { setCompleted(id, completed, expectedRevision) }
    override suspend fun skipOccurrences(expectedRevisions: Map<String, Long>) = mutate { skipOccurrences(expectedRevisions) }
    override suspend fun setDeclined(id: String, declined: Boolean, checkpoint: PreferenceCommit?) = mutate { setDeclined(id, declined) }
    override suspend fun snooze(id: String, until: Instant) = mutate { snooze(id, until) }
    override suspend fun changeSchedule(id: String, schedule: TaskSchedule, checkpoint: PreferenceCommit?) = mutate { changeSchedule(id, schedule) }
    override suspend fun addList(name: String, color: Int?, view: TaskListView) = mutate { addList(name, color, view) }
    override suspend fun addTag(name: String, color: Int?) = mutate { addTag(name, color) }
    override suspend fun addFilter(name: String, rules: List<FilterRule>, matchAll: Boolean) = mutate { addFilter(name, rules, matchAll) }
    override suspend fun deleteTask(id: String, draft: DraftToken?, checkpoint: PreferenceCommit?) = mutate { deleteTask(id) }
    override suspend fun deleteTasks(ids: Set<String>) = mutate { deleteTasks(ids) }
    override suspend fun restoreTasks(deleted: DeletedTasks) = mutate { restoreTasks(deleted) }
}
