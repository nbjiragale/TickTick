package com.niranjan.ticktick.core.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Reads the current device zone after a timezone broadcast, without restarting the process. */
class DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)
    override fun instant(): Instant = Instant.now()
}
