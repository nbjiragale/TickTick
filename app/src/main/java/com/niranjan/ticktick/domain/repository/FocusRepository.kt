package com.niranjan.ticktick.domain.repository

import com.niranjan.ticktick.domain.model.*
import kotlinx.coroutines.flow.StateFlow
interface FocusRepository {
    val session: StateFlow<FocusSession>
    val history: StateFlow<List<FocusRecord>>
    val storeState: StateFlow<TaskStoreState>
    fun retryLoad()
    suspend fun update(transform: (FocusSession) -> FocusSession)
    suspend fun start()
    suspend fun pause()
    suspend fun stop()
    suspend fun reconcile()
    suspend fun acknowledgeCompletion(id: String)
}
