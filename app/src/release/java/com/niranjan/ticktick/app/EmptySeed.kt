package com.niranjan.ticktick.app

import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.model.TaskSnapshot
import java.time.Clock

const val reminderPreviewEnabled = false

@Suppress("UNUSED_PARAMETER")
fun initialTaskSnapshot(clock: Clock) = TaskSnapshot(
    lists = listOf(TaskList("inbox", "Inbox", ListSymbol.Inbox)),
)
