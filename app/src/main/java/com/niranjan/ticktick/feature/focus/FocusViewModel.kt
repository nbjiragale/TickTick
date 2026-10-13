package com.niranjan.ticktick.feature.focus

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.ticktick.domain.repository.FocusRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

typealias FocusMode = com.niranjan.ticktick.domain.model.FocusMode
typealias FocusPhase = com.niranjan.ticktick.domain.model.FocusPhase
typealias FocusSession = com.niranjan.ticktick.domain.model.FocusSession

data class FocusUiState(val session: FocusSession = FocusSession(), val elapsedMs: Long = 0) {
    val durationMs get() = session.durationMinutes * 60_000L
    val displaySeconds get() = if (session.mode == FocusMode.Pomo) ((durationMs - elapsedMs).coerceAtLeast(0) + 999) / 1000 else elapsedMs / 1000
    val progress get() = if (session.mode == FocusMode.Pomo) (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f) else (elapsedMs % 60_000) / 60_000f
}
class FocusViewModel(private val repository: FocusRepository) : ViewModel() {
    private val ticks = flow { while (true) { emit(SystemClock.elapsedRealtime()); delay(200) } }
    val uiState = combine(repository.session, ticks) { value, now -> FocusUiState(value, value.elapsed(now)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState(repository.session.value))
    val history = repository.history
    private val error = MutableStateFlow<String?>(null)
    val writeError = error.asStateFlow()
    private val working = MutableStateFlow(false)
    val busy = working.asStateFlow()
    fun clearError() { error.value = null }
    private fun write(action: suspend () -> Unit) {
        if (working.value) return
        working.value = true
        viewModelScope.launch {
            try { action(); error.value = null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error.value = "Couldn't save the Focus change. Your previous session is safe. Try again." }
            finally { working.value = false }
        }
    }
    init {
        viewModelScope.launch {
            while (true) {
                val value = repository.session.value
                if (value.phase == FocusPhase.Running && value.mode == FocusMode.Pomo && value.elapsed(SystemClock.elapsedRealtime()) >= value.durationMinutes * 60_000L) {
                    try { repository.reconcile() } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { error.value = "Couldn't save Focus completion. Please retry." }
                }
                delay(200)
            }
        }
    }
    private fun change(transform: (FocusSession) -> FocusSession) = write { repository.update(transform) }
    fun mode(mode: FocusMode) = change { if (it.phase in setOf(FocusPhase.Ready, FocusPhase.Finished))
        it.copy(mode = mode, phase = FocusPhase.Ready, elapsedMs = 0, completionPending = false) else it }
    fun start() = write { repository.start() }
    fun pause() = write { repository.pause() }
    fun stop() = write { repository.stop() }
    fun expand(expanded: Boolean) = change { it.copy(expanded = expanded) }
    fun toggleScreenOn() = change { it.copy(keepScreenOn = !it.keepScreenOn) }
    fun task(id: String?) = change { it.copy(taskId = id) }
    fun sound(uri: String?) = change { it.copy(soundUri = uri) }
    suspend fun saveNote(note: String): String? = try {
        repository.update { it.copy(note = note) }; null
    } catch (e: CancellationException) { throw e } catch (_: Exception) { "Couldn't save this note. Try again." }
    suspend fun saveDuration(minutes: Int): String? = try {
        repository.update {
            check(it.phase in setOf(FocusPhase.Ready, FocusPhase.Finished))
            it.copy(durationMinutes = minutes, phase = FocusPhase.Ready, elapsedMs = 0, completionPending = false)
        }; null
    } catch (e: CancellationException) { throw e } catch (_: Exception) { "Couldn't change the duration. Stop the active session or retry." }
}
