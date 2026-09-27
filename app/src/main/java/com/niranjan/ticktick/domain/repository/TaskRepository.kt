package com.niranjan.ticktick.domain.repository

import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskSnapshot
import com.niranjan.ticktick.domain.model.TaskSchedule
import com.niranjan.ticktick.domain.model.TaskListView
import com.niranjan.ticktick.domain.model.FilterRule
import java.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface TaskRepository {
    val snapshot: StateFlow<TaskSnapshot>
    val storeState: StateFlow<TaskStoreState>
    fun retryLoad()
    suspend fun save(task: Task, draft: DraftToken? = null): Task
    suspend fun setCompleted(id: String, completed: Boolean, expectedRevision: Long, checkpoint: PreferenceCommit? = null)
    suspend fun skipOccurrences(expectedRevisions: Map<String, Long>)
    suspend fun setDeclined(id: String, declined: Boolean, checkpoint: PreferenceCommit? = null)
    suspend fun snooze(id: String, until: Instant)
    suspend fun changeSchedule(id: String, schedule: TaskSchedule, checkpoint: PreferenceCommit? = null)
    suspend fun addList(name: String, color: Int? = null, view: TaskListView = TaskListView.List): String
    suspend fun addTag(name: String, color: Int? = null)
    suspend fun addFilter(name: String, rules: List<FilterRule>, matchAll: Boolean): String
    suspend fun deleteTask(id: String, draft: DraftToken? = null, checkpoint: PreferenceCommit? = null)
    suspend fun saveTasks(tasks: List<Task>)
    suspend fun deleteTasks(ids: Set<String>): DeletedTasks
    suspend fun restoreTasks(deleted: DeletedTasks)
}

data class DeletedTasks(val tasks: List<Pair<Int, Task>>, val snoozedUntil: Map<String, Instant>,
    val history: List<com.niranjan.ticktick.domain.model.TaskOccurrenceHistory> = emptyList())
