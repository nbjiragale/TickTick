package com.niranjan.ticktick.domain.model

enum class AppSound(val label: String, val key: String) {
    TaskCompletion("Task completion", "sound.task_completion"),
    Notification("Notification", "sound.notification"),
    HabitCompletion("Habit completion", "sound.habit_completion"),
    Popup("Reminder popup", "sound.popup"),
}

/** Empty URI follows Android's notification default; null is explicitly silent. */
data class SoundChoice(val uri: String?, val label: String)
