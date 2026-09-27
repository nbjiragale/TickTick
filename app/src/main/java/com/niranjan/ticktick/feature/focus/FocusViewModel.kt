package com.niranjan.ticktick.feature.focus

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.Serializable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FocusMode { Pomo, Stopwatch }
enum class FocusPhase { Ready, Running, Paused, Finished }

data class FocusSession(
    val mode: FocusMode = FocusMode.Pomo,
    val phase: FocusPhase = FocusPhase.Ready,
    val durationMinutes: Int = 25,
    val elapsedMs: Long = 0,
    val startedAt: Long = 0,
    val expanded: Boolean = false,
    val keepScreenOn: Boolean = false,
    val note: String = "",
    val taskId: String? = null,
    val completedSessions: Int = 0,
    val completedFocusMs: Long = 0,
) : Serializable {
    fun elapsed(now: Long) = elapsedMs + if (phase == FocusPhase.Running) (now - startedAt).coerceAtLeast(0) else 0
}

data class FocusUiState(val session: FocusSession = FocusSession(), val elapsedMs: Long = 0) {
    val durationMs get() = session.durationMinutes * 60_000L
    val displaySeconds get() = if (session.mode == FocusMode.Pomo) ((durationMs - elapsedMs).coerceAtLeast(0) + 999) / 1000 else elapsedMs / 1000
    val progress get() = if (session.mode == FocusMode.Pomo) (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f) else (elapsedMs % 60_000) / 60_000f
}

class FocusViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val session = saved.getStateFlow("focusSession", FocusSession())
    private val ticks = flow { while (true) { emit(SystemClock.elapsedRealtime()); delay(200) } }
    val uiState = combine(session, ticks) { value, now -> FocusUiState(value, value.elapsed(now)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState(session.value, session.value.elapsed(SystemClock.elapsedRealtime())))

    init {
        viewModelScope.launch {
            while (true) {
                val value = session.value
                if (value.phase == FocusPhase.Running && value.mode == FocusMode.Pomo && value.elapsed(SystemClock.elapsedRealtime()) >= value.durationMinutes * 60_000L) {
                    change { it.copy(phase = FocusPhase.Finished, elapsedMs = it.durationMinutes * 60_000L,
                        completedSessions = it.completedSessions + 1, completedFocusMs = it.completedFocusMs + it.durationMinutes * 60_000L) }
                }
                delay(200)
            }
        }
    }

    private fun change(transform: (FocusSession) -> FocusSession) { saved["focusSession"] = transform(session.value) }
    fun mode(mode: FocusMode) { if (session.value.phase == FocusPhase.Ready || session.value.phase == FocusPhase.Finished)
        change { it.copy(mode = mode, phase = FocusPhase.Ready, elapsedMs = 0) } }
    fun start() = change {
        if (it.phase == FocusPhase.Running) it else it.copy(phase = FocusPhase.Running, expanded = true, startedAt = SystemClock.elapsedRealtime(),
            elapsedMs = if (it.phase == FocusPhase.Paused) it.elapsedMs else 0,
            note = if (it.phase == FocusPhase.Finished) "" else it.note)
    }
    fun pause() = change { if (it.phase == FocusPhase.Running) it.copy(phase = FocusPhase.Paused, elapsedMs = it.elapsed(SystemClock.elapsedRealtime())) else it }
    fun stop() = change {
        val stopwatchMs = if (it.mode == FocusMode.Stopwatch) it.elapsed(SystemClock.elapsedRealtime()) else 0
        it.copy(phase = FocusPhase.Ready, elapsedMs = 0, startedAt = 0, expanded = false,
            completedSessions = it.completedSessions + if (stopwatchMs > 0) 1 else 0, completedFocusMs = it.completedFocusMs + stopwatchMs)
    }
    fun expand(expanded: Boolean) = change { it.copy(expanded = expanded) }
    fun toggleScreenOn() = change { it.copy(keepScreenOn = !it.keepScreenOn) }
    fun note(note: String) = change { it.copy(note = note) }
    fun task(id: String?) = change { it.copy(taskId = id) }
    fun duration(minutes: Int) {
        require(minutes in 1..180)
        if (session.value.phase == FocusPhase.Ready || session.value.phase == FocusPhase.Finished)
            change { it.copy(durationMinutes = minutes, phase = FocusPhase.Ready, elapsedMs = 0) }
    }
}
