package com.niranjan.ticktick.app

import com.niranjan.ticktick.domain.model.Holiday
import com.niranjan.ticktick.domain.model.HolidaySnapshot
import java.time.Clock
import java.time.LocalDate

/**
 * Clearly labeled sample holidays for the UI demo. No real-world holiday dates are claimed and no
 * region is assumed; replace this seed once the user chooses an explicit holiday source/region.
 */
fun initialHolidaySnapshot(clock: Clock): HolidaySnapshot {
    val today = LocalDate.now(clock)
    return HolidaySnapshot(
        holidays = listOf(
            Holiday("sample-founders", "Founders Day (Sample)", today),
            Holiday("sample-harvest", "Harvest Fair (Sample)", today.plusDays(6)),
            Holiday("sample-reading", "Reading Day (Sample)", today.plusDays(17)),
            Holiday("sample-bridge", "Bridge Day (Sample)", today.plusMonths(1).withDayOfMonth(3)),
        ),
        sourceLabel = "Sample holidays",
    )
}
