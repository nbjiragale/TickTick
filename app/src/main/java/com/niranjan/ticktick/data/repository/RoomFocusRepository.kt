package com.niranjan.ticktick.data.repository

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.room.withTransaction
import com.niranjan.ticktick.data.local.*
import com.niranjan.ticktick.data.mapper.*
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.repository.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock

class RoomFocusRepository(private val database: TickTickDatabase, private val context: Context,
    private val scope: CoroutineScope) : FocusRepository {
    private val dao = database.focus()
    private val data = MutableStateFlow(FocusSession())
    override val session = data.asStateFlow()
    private val records = MutableStateFlow<List<FocusRecord>>(emptyList())
    override val history = records.asStateFlow()
    private val status = MutableStateFlow<TaskStoreState>(TaskStoreState.Loading)
    override val storeState = status.asStateFlow()
    private var loading: Job? = null
    private fun boot() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    init { retryLoad() }
    suspend fun reloadAfterRestore() {
        loading?.cancelAndJoin(); loading = null
        status.value = TaskStoreState.Loading
        retryLoad()
        check(storeState.first { it != TaskStoreState.Loading } == TaskStoreState.Ready)
    }
    override fun retryLoad() {
        if (loading?.isActive == true) return
        loading = scope.launch {
            status.value = TaskStoreState.Loading
            try {
                database.invalidationTracker.createFlow("focus_session", "focus_history").collect {
                    database.operationLock.withLock { publish(database.withTransaction { loaded(recover(read())) }) }
                    status.value = TaskStoreState.Ready
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { status.value = TaskStoreState.Failed("Couldn't load Focus sessions. Your history has not been reset. Retry.") }
        }
    }
    private suspend fun read() = dao.session()?.payload?.decodeFocus() ?: FocusSession()
    private suspend fun recover(value: FocusSession): FocusSession {
        if (value.phase != FocusPhase.Running) return value
        val now = SystemClock.elapsedRealtime()
        if (value.bootCount < 0 || value.bootCount != boot() || now < value.startedAt) {
            val paused = value.copy(phase = FocusPhase.Paused, startedAt = 0, completionPending = false)
            dao.putSession(FocusSessionEntity(payload = paused.encodeFocus()))
            return paused
        }
        if (value.mode == FocusMode.Pomo && value.elapsed(now) >= value.durationMinutes * 60_000L) {
            val finished = value.copy(phase = FocusPhase.Finished, elapsedMs = value.durationMinutes * 60_000L,
                startedAt = 0, completionPending = true)
            record(finished, true)
            dao.putSession(FocusSessionEntity(payload = finished.encodeFocus()))
            return finished
        }
        return value
    }
    private suspend fun record(value: FocusSession, completed: Boolean) {
        if (value.id.isNotBlank()) dao.putHistory(FocusHistoryEntity(value.id, value.mode.name, value.elapsedMs,
            value.wallStartedAt, System.currentTimeMillis(), value.note, value.taskId, completed))
    }
    private suspend fun loaded(value: FocusSession): Pair<FocusSession, List<FocusRecord>> {
        val history = dao.history().map { FocusRecord(it.id, FocusMode.valueOf(it.mode), it.elapsedMs, it.startedAt, it.endedAt, it.note, it.taskId, it.completed) }
        val completed = history.filter { it.completed }
        return value.copy(completedSessions = completed.size, completedFocusMs = completed.sumOf { it.elapsedMs }) to history
    }
    private fun publish(value: Pair<FocusSession, List<FocusRecord>>) {
        records.value = value.second; data.value = value.first
    }
    private suspend fun mutate(action: suspend (FocusSession) -> FocusSession) = database.writeLocked {
        check(status.value == TaskStoreState.Ready) { "Focus storage is not ready. Retry." }
        withContext(NonCancellable) {
            val committed = database.withTransaction {
                val old = recover(read())
                var next = action(old)
                require(next.durationMinutes in 1..180 && next.note.length <= 100_000)
                if (next.phase == FocusPhase.Running) {
                    val now = SystemClock.elapsedRealtime()
                    next = next.copy(elapsedMs = next.elapsed(now), startedAt = now)
                }
                if (next != old) dao.putSession(FocusSessionEntity(payload = next.encodeFocus()))
                loaded(next)
            }
            publish(committed)
        }
    }
    override suspend fun update(transform: (FocusSession) -> FocusSession) = mutate { old ->
        val next = transform(old)
        if (next.phase == FocusPhase.Finished && next.id == old.id && (next.note != old.note || next.taskId != old.taskId)) {
            dao.history().firstOrNull { it.id == next.id }?.let { dao.putHistory(it.copy(note = next.note, taskId = next.taskId)) }
        }
        next
    }
    override suspend fun start() = mutate { value ->
        if (value.phase == FocusPhase.Running) value else {
            val resume = value.phase == FocusPhase.Paused
            value.copy(id = if (resume) value.id else UUID.randomUUID().toString(), phase = FocusPhase.Running,
                expanded = true, startedAt = SystemClock.elapsedRealtime(), bootCount = boot(),
                wallStartedAt = if (resume) value.wallStartedAt else System.currentTimeMillis(),
                elapsedMs = if (resume) value.elapsedMs else 0, completionPending = false,
                note = if (value.phase == FocusPhase.Finished) "" else value.note)
        }
    }
    override suspend fun pause() = mutate { if (it.phase == FocusPhase.Running)
        it.copy(phase = FocusPhase.Paused, elapsedMs = it.elapsed(SystemClock.elapsedRealtime()), startedAt = 0) else it }
    override suspend fun stop() = mutate { value ->
        if (value.phase == FocusPhase.Running || value.phase == FocusPhase.Paused) {
            val ended = value.copy(elapsedMs = value.elapsed(SystemClock.elapsedRealtime()))
            if (ended.elapsedMs > 0) record(ended, ended.mode == FocusMode.Stopwatch)
        }
        value.copy(id = "", phase = FocusPhase.Ready, elapsedMs = 0, startedAt = 0, expanded = false, completionPending = false)
    }
    override suspend fun reconcile() = mutate { it }
    override suspend fun acknowledgeCompletion(id: String) = mutate { if (it.id == id) it.copy(completionPending = false) else it }
}
