package com.niranjan.ticktick.app

import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.model.TaskSnapshot
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

fun initialTaskSnapshot(clock: Clock): TaskSnapshot {
    val today = LocalDate.now(clock)
    return TaskSnapshot(
        tasks = listOf(
            Task("read-java", "read java", dueDate = today, dueTime = LocalTime.of(21, 0)),
            Task("ping-mahesh", "ping Mahesh", dueDate = today, dueTime = LocalTime.of(22, 0)),
            Task("call-yogya", "CALL yogya", dueDate = today.plusDays(1)),
            Task("groceries", "Bring groceries tomorrow", dueDate = today.plusDays(1)),
        ),
        lists = listOf(
            TaskList("inbox", "Inbox", ListSymbol.Inbox),
            TaskList("work", "Work", ListSymbol.Work),
            TaskList("personal", "Personal", ListSymbol.Personal),
            TaskList("welcome", "Welcome", ListSymbol.Welcome),
        ),
    )
}
