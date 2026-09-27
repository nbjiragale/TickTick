package com.niranjan.ticktick.data.repository

import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate

/** Explicit demo-only adapter. Production uses the Room transaction coordinator. */
class InMemoryHabitRepository(initial: HabitSnapshot, private val clock: Clock = Clock.systemDefaultZone()) : HabitRepository {
    private val records = MutableStateFlow(initial)
    private val mutex = Mutex()
    override val snapshot = records.asStateFlow()
    private suspend fun <T> mutate(action: HabitMutation.() -> T): T = mutex.withLock {
        val mutation = HabitMutation(records.value, clock)
        action(mutation).also { records.value = mutation.snapshot }
    }
    override suspend fun save(habit: Habit, draft: com.niranjan.ticktick.domain.repository.DraftToken?) = mutate { save(habit) }
    override suspend fun setArchived(id: String, archived: Boolean) = mutate { setArchived(id, archived) }
    override suspend fun delete(id: String) = mutate { delete(id) }
    override suspend fun setProgress(id: String, date: LocalDate, amount: Int, expectedRevision: Long) = mutate { setProgress(id, date, amount, expectedRevision) }
    override suspend fun updateSettings(settings: HabitSettings) = mutate { updateSettings(settings) }
    override suspend fun addSection(name: String) = mutate { addSection(name) }
    override suspend fun moveSection(name: String, direction: Int) = mutate { moveSection(name, direction) }
}
