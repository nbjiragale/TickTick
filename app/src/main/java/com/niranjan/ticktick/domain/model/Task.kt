package com.niranjan.ticktick.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Task(
    val id: String,
    val title: String,
    val listId: String = "inbox",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val completed: Boolean = false,
)

data class TaskList(val id: String, val name: String, val symbol: ListSymbol)
enum class ListSymbol { Inbox, Work, Personal, Welcome, Custom }
data class TaskSnapshot(val tasks: List<Task> = emptyList(), val lists: List<TaskList> = emptyList())

sealed interface TaskFilter {
    data object Today : TaskFilter
    data class ListFilter(val listId: String) : TaskFilter
}
