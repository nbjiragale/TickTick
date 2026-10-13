package com.niranjan.ticktick.domain.model

import java.io.Serializable

enum class FocusMode { Pomo, Stopwatch }
enum class FocusPhase { Ready, Running, Paused, Finished }

data class FocusSession(
    val id: String = "", val mode: FocusMode = FocusMode.Pomo, val phase: FocusPhase = FocusPhase.Ready,
    val durationMinutes: Int = 25, val elapsedMs: Long = 0, val startedAt: Long = 0,
    val bootCount: Int = -1, val wallStartedAt: Long = 0,
    val expanded: Boolean = false, val keepScreenOn: Boolean = false,
    val note: String = "", val taskId: String? = null, val soundUri: String? = null,
    val completionPending: Boolean = false, val completedSessions: Int = 0, val completedFocusMs: Long = 0,
) : Serializable {
    fun elapsed(now: Long): Long = elapsedMs + if (phase == FocusPhase.Running) (now - startedAt).coerceAtLeast(0) else 0
}

data class FocusRecord(val id: String, val mode: FocusMode, val elapsedMs: Long,
    val startedAt: Long, val endedAt: Long, val note: String, val taskId: String?, val completed: Boolean)
