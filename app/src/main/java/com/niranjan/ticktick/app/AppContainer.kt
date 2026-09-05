package com.niranjan.ticktick.app

import android.app.Application
import com.niranjan.ticktick.data.repository.InMemoryTaskRepository
import com.niranjan.ticktick.domain.repository.TaskRepository
import java.time.Clock

class AppContainer {
    val clock: Clock = Clock.systemDefaultZone()
    val taskRepository: TaskRepository = InMemoryTaskRepository(initialTaskSnapshot(clock))
}

class TickTickApplication : Application() {
    val container by lazy { AppContainer() }
}
