package com.niranjan.ticktick.domain.model

import java.time.LocalDate

/** A display-only calendar holiday from an explicitly labeled source. */
data class Holiday(val id: String, val name: String, val date: LocalDate)

data class HolidaySnapshot(
    val holidays: List<Holiday> = emptyList(),
    /** Explicit provenance shown in the calendar legend; never an assumed region. */
    val sourceLabel: String = "No holiday source configured",
)
