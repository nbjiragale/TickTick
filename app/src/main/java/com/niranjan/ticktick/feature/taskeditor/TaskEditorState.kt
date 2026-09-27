package com.niranjan.ticktick.feature.taskeditor

import androidx.compose.ui.text.input.TextFieldValue
import com.niranjan.ticktick.domain.model.ChecklistItem
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskAttachment
import com.niranjan.ticktick.domain.model.TaskPriority
import com.niranjan.ticktick.domain.model.ReminderConfig
import com.niranjan.ticktick.domain.model.RepeatRule
import com.niranjan.ticktick.domain.model.TaskDuration
import com.niranjan.ticktick.domain.model.TaskSchedule
import com.niranjan.ticktick.domain.nlp.ParsedSchedule
import com.niranjan.ticktick.domain.nlp.ParsedTag
import java.time.LocalDate
import java.time.LocalTime

enum class EditorPanel { More, Date, Priority, Lists, Attachments, ScanDocuments, Settings, Templates, Voice, Delete }

data class TaskEditorState(
    val active: Boolean = false,
    val id: String = "",
    val existing: Boolean = false,
    val fullScreen: Boolean = false,
    val title: TextFieldValue = TextFieldValue(),
    val description: TextFieldValue = TextFieldValue(),
    val listId: String = "inbox",
    val priority: TaskPriority = TaskPriority.None,
    val contextDate: LocalDate? = null,
    val manualDate: LocalDate? = null,
    val manualTime: LocalTime? = null,
    val dateManuallySet: Boolean = false,
    val prediction: ParsedSchedule? = null,
    val suppressedPrediction: String? = null,
    val nlpEnabled: Boolean = true,
    val parsedTags: List<ParsedTag> = emptyList(),
    val tags: List<String> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
    val checklistMode: Boolean = false,
    val attachments: List<TaskAttachment> = emptyList(),
    val attachmentLoading: Boolean = false,
    val isNote: Boolean = false,
    val completed: Boolean = false,
    val declined: Boolean = false,
    val reminders: ReminderConfig = ReminderConfig(),
    val repeat: RepeatRule = RepeatRule(),
    val duration: TaskDuration? = null,
    val scheduleOnly: Boolean = false,
    val changed: Boolean = false,
    val panel: EditorPanel? = null,
    val confirmDiscard: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
    val recoveryFailed: Boolean = false,
) {
    val activePrediction get() = prediction?.takeIf { nlpEnabled && !dateManuallySet && it.fingerprint != suppressedPrediction }
    val dueDate get() = if (dateManuallySet) manualDate else activePrediction?.date ?: contextDate
    val dueTime get() = if (dateManuallySet) manualTime else activePrediction?.time
    val allTags get() = (tags + parsedTags.map { it.name }).distinctBy { it.lowercase() }
    val hasReminder get() = dueDate != null && reminders.offsetsMinutes.isNotEmpty()
    val schedule get() = TaskSchedule(dueDate, dueTime, duration, reminders, repeat)
    fun toTask() = Task(
        id = id, title = title.text, listId = listId, dueDate = dueDate, dueTime = dueTime,
        completed = completed, declined = declined, description = description.text, priority = priority, tags = allTags,
        checklist = checklist, attachments = attachments, isNote = isNote,
        reminders = reminders, repeat = repeat, duration = duration,
    )
}
