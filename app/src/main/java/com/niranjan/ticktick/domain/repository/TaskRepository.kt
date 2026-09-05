package com.niranjan.ticktick.domain.repository

import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskSnapshot
import kotlinx.coroutines.flow.StateFlow

interface TaskRepository {
    val snapshot: StateFlow<TaskSnapshot>
    fun save(task: Task)
    fun setCompleted(id: String, completed: Boolean)
    fun addList(name: String): String
    fun deleteTask(id: String)
}
