package com.niranjan.ticktick.domain.repository

import kotlinx.coroutines.CancellationException

sealed interface TaskStoreState {
    data object Loading : TaskStoreState
    data object Ready : TaskStoreState
    data class Failed(val message: String) : TaskStoreState
}

class TaskConflictException : IllegalStateException("This task changed elsewhere. Reopen it to review the latest version; your unsaved draft is still available.")

/** Keep cancellation structured and never expose database errors/private values to the UI. */
suspend fun <T> taskWriteResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (conflict: TaskConflictException) {
    Result.failure(conflict)
} catch (invalid: IllegalArgumentException) {
    Result.failure(IllegalArgumentException(invalid.message ?: "Check the entered values."))
} catch (_: Exception) {
    Result.failure(IllegalStateException("Couldn't save changes. Please try again."))
}
