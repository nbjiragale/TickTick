package com.niranjan.ticktick.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class DraftToken(val key: String, val sessionId: String, val version: Long)
data class PreferenceCommit(val key: String, val payload: String)
data class StoredDraft(val token: DraftToken, val ownerId: String, val baseRevision: Long, val payload: String?, val format: Int = 1)
data class UiSnapshot(val preferences: Map<String, String> = emptyMap(), val drafts: Map<String, StoredDraft> = emptyMap())

interface UiStateRepository {
    val snapshot: StateFlow<UiSnapshot>
    val storeState: StateFlow<TaskStoreState>
    val draftError: StateFlow<String?>
    fun retryLoad()
    suspend fun updatePreference(key: String, transform: (String?) -> String)
    fun currentDraft(key: String): StoredDraft?
    /** Application-owned ordered writes survive disposal of the editor's ViewModel/composition. */
    fun stageDraft(key: String, sessionId: String, ownerId: String, baseRevision: Long, payload: String?, newSession: Boolean = false): DraftToken
    suspend fun flushDrafts()
    fun retryDrafts()
}
