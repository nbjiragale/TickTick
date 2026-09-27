package com.niranjan.ticktick.domain.repository

import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.HabitSnapshot
import com.niranjan.ticktick.domain.model.HabitSettings
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface HabitRepository {
    val snapshot: StateFlow<HabitSnapshot>
    suspend fun save(habit: Habit, draft: DraftToken? = null)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun delete(id: String)
    suspend fun setProgress(id: String, date: LocalDate, amount: Int, expectedRevision: Long)
    suspend fun updateSettings(settings: HabitSettings)
    suspend fun addSection(name: String): Boolean
    suspend fun moveSection(name: String, direction: Int)
}
