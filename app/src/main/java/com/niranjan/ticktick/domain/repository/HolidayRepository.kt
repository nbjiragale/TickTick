package com.niranjan.ticktick.domain.repository

import com.niranjan.ticktick.domain.model.HolidaySnapshot
import kotlinx.coroutines.flow.StateFlow

/** Read-only during the UI phase; mutations arrive with a real region source. */
interface HolidayRepository {
    val snapshot: StateFlow<HolidaySnapshot>
}
