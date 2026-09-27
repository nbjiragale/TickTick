package com.niranjan.ticktick.platform.reminders

data class ReminderOpenRequest(val id: String?, val revision: Long, val changeDate: Boolean,
    val requestId: Long = System.nanoTime(), val habitSummary: Boolean = false)
