package com.niranjan.ticktick.data.repository

import androidx.room.withTransaction
import com.niranjan.ticktick.data.local.*
import com.niranjan.ticktick.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomUiStateRepository(private val database: TickTickDatabase, private val scope: CoroutineScope) : UiStateRepository {
    private val dao = database.uiState()
    private val data = MutableStateFlow(UiSnapshot())
    override val snapshot = data.asStateFlow()
    private val status = MutableStateFlow<TaskStoreState>(TaskStoreState.Loading)
    override val storeState = status.asStateFlow()
    private val error = MutableStateFlow<String?>(null)
    override val draftError = error.asStateFlow()
    private val mutex = Mutex()
    private val desired = mutableMapOf<String, StoredDraft>()
    private val failed = mutableSetOf<String>()
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var loading: Job? = null

    init {
        scope.launch { for (write in queue) write() }
        retryLoad()
    }

    override fun retryLoad() {
        if (loading?.isActive == true) return
        loading = scope.launch {
            status.value = TaskStoreState.Loading
            try {
                database.invalidationTracker.createFlow("ui_preferences", "editor_drafts").collect {
                    mutex.withLock {
                        data.value = database.withTransaction {
                            val preferences = dao.preferences().onEach { require(it.format == 1) { "Unsupported preference format" } }
                            val drafts = dao.drafts().onEach { require(it.format == 1) { "Unsupported draft format" } }
                            UiSnapshot(preferences.associate { it.key to it.payload }, drafts.associate { row ->
                                row.key to StoredDraft(DraftToken(row.key, row.sessionId, row.version), row.ownerId, row.baseRevision, row.payload, row.format)
                            })
                        }
                        status.value = TaskStoreState.Ready
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { status.value = TaskStoreState.Failed("Couldn't load preferences and drafts. Your saved data has not been reset. Please retry.") }
        }
    }

    override suspend fun updatePreference(key: String, transform: (String?) -> String) = mutex.withLock {
        check(status.value == TaskStoreState.Ready)
        withContext(NonCancellable) {
            val next = database.withTransaction {
                val next = transform(dao.preference(key)?.payload)
                dao.putPreference(UiPreferenceEntity(key, payload = next))
                next
            }
            data.value = data.value.copy(preferences = data.value.preferences + (key to next))
        }
    }

    override fun currentDraft(key: String): StoredDraft? = synchronized(desired) { desired[key] ?: data.value.drafts[key] }

    override fun stageDraft(key: String, sessionId: String, ownerId: String, baseRevision: Long, payload: String?, newSession: Boolean): DraftToken = synchronized(desired) {
        val previous = desired[key] ?: data.value.drafts[key]
        // An old editor may finish an in-flight save after its replacement opened. Ignore that publisher.
        if (previous != null && previous.token.sessionId != sessionId && !newSession)
            return@synchronized DraftToken(key, sessionId, previous.token.version)
        val record = StoredDraft(DraftToken(key, sessionId, (previous?.token?.version ?: 0) + 1), ownerId, baseRevision, payload)
        desired[key] = record
        enqueue(record, replaceSession = newSession || previous == null)
        record.token
    }

    private fun enqueue(record: StoredDraft, replaceSession: Boolean) {
        queue.trySend {
            try {
                mutex.withLock {
                    database.withTransaction {
                        val old = dao.draft(record.token.key)
                        require(old == null || old.sessionId == record.token.sessionId || replaceSession) { "Draft session changed" }
                        if (old == null || old.version < record.token.version) {
                            dao.putDraft(EditorDraftEntity(record.token.key, record.token.sessionId, record.token.version,
                                record.ownerId, record.baseRevision, record.format, record.payload))
                        }
                    }
                }
                synchronized(desired) { failed.remove(record.token.key); if (failed.isEmpty()) error.value = null }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                synchronized(desired) { failed.add(record.token.key) }
                error.value = "Your latest draft couldn't be saved for recovery. Keep the app open and retry."
            }
        }
    }

    override suspend fun flushDrafts() {
        val barrier = CompletableDeferred<Unit>()
        queue.send {
            if (synchronized(desired) { failed.isEmpty() }) barrier.complete(Unit)
            else barrier.completeExceptionally(IllegalStateException("Couldn't save the draft. Retry draft storage before continuing."))
        }
        barrier.await()
    }

    override fun retryDrafts() = synchronized(desired) {
        failed.toList().mapNotNull { desired[it] }.forEach { enqueue(it, replaceSession = true) }
    }
}
