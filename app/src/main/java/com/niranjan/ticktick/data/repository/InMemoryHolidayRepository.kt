package com.niranjan.ticktick.data.repository

import com.niranjan.ticktick.domain.model.HolidaySnapshot
import com.niranjan.ticktick.domain.repository.HolidayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Session-only UI store, matching the task and habit repositories' persistence boundary. */
class InMemoryHolidayRepository(initial: HolidaySnapshot) : HolidayRepository {
    private val records = MutableStateFlow(initial)
    override val snapshot = records.asStateFlow()
}
