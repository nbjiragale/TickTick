package com.niranjan.ticktick.domain.model

import java.time.LocalDate

enum class FilterField(val label: String) { Date("Date"), Priority("Priority"), List("List"), Tag("Tag") }
enum class FilterDate(val label: String) {
    Today("Today"), Tomorrow("Tomorrow"), ThisWeek("This week"), Next7Days("Next 7 days"), Overdue("Overdue"), NoDate("No date")
}
data class FilterRule(val field: FilterField, val value: String) : java.io.Serializable {
    fun matches(task: Task, today: LocalDate): Boolean = when (field) {
        FilterField.Priority -> task.priority.name == value
        FilterField.List -> task.listId == value
        FilterField.Tag -> task.tags.any { it.equals(value, ignoreCase = true) }
        FilterField.Date -> when (FilterDate.entries.find { it.name == value }) {
            FilterDate.Today -> task.dueDate == today
            FilterDate.Tomorrow -> task.dueDate == today.plusDays(1)
            FilterDate.Overdue -> task.dueDate?.let { it < today } == true
            FilterDate.NoDate -> task.dueDate == null
            FilterDate.Next7Days -> task.dueDate?.let { it >= today && it <= today.plusDays(6) } == true
            FilterDate.ThisWeek -> {
                val start = today.minusDays((today.dayOfWeek.value % 7).toLong())
                task.dueDate?.let { it >= start && it < start.plusDays(7) } == true
            }
            null -> false
        }
    }
    fun label(snapshot: TaskSnapshot): String = when (field) {
        FilterField.Date -> FilterDate.entries.find { it.name == value }?.label ?: value
        FilterField.Priority -> if (value == "None") "No Priority" else "$value Priority"
        FilterField.List -> snapshot.lists.find { it.id == value }?.name ?: "Missing list"
        FilterField.Tag -> "#$value"
    }
}
data class SavedTaskFilter(val id: String, val name: String, val rules: List<FilterRule>, val matchAll: Boolean = true) {
    fun matches(task: Task, today: LocalDate) = rules.isNotEmpty() &&
        if (matchAll) rules.all { it.matches(task, today) } else rules.any { it.matches(task, today) }
}
