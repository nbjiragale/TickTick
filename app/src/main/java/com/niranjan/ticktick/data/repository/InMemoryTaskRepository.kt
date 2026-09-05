package com.niranjan.ticktick.data.repository

import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.model.TaskSnapshot
import com.niranjan.ticktick.domain.repository.TaskRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Session-only UI data source. Replace this binding with Room in the persistence phase. */
class InMemoryTaskRepository(initial: TaskSnapshot) : TaskRepository {
    private val mutableSnapshot = MutableStateFlow(initial)
    override val snapshot = mutableSnapshot.asStateFlow()

    override fun save(task: Task) {
        require(task.title.isNotBlank())
        mutableSnapshot.update { current ->
            require(current.lists.any { it.id == task.listId })
            val exists = current.tasks.any { it.id == task.id }
            current.copy(tasks = if (exists) {
                current.tasks.map { if (it.id == task.id) task else it }
            } else current.tasks + task)
        }
    }

    override fun setCompleted(id: String, completed: Boolean) {
        mutableSnapshot.update { current ->
            current.copy(tasks = current.tasks.map { if (it.id == id) it.copy(completed = completed) else it })
        }
    }

    override fun addList(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        val id = UUID.randomUUID().toString()
        mutableSnapshot.update { current ->
            current.copy(lists = current.lists + TaskList(id, trimmed, ListSymbol.Custom))
        }
        return id
    }

    override fun deleteTask(id: String) {
        mutableSnapshot.update { it.copy(tasks = it.tasks.filterNot { task -> task.id == id }) }
    }
}
