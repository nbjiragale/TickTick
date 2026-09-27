package com.niranjan.ticktick.feature.taskeditor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.ticktick.domain.model.ChecklistItem
import com.niranjan.ticktick.domain.model.TaskAttachment
import com.niranjan.ticktick.domain.model.TaskPriority
import com.niranjan.ticktick.domain.model.TaskSchedule
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.nlp.TaskDateParser
import com.niranjan.ticktick.domain.nlp.TextSpan
import com.niranjan.ticktick.domain.repository.TaskRepository
import com.niranjan.ticktick.domain.repository.taskWriteResult
import com.niranjan.ticktick.domain.repository.UiStateRepository
import com.niranjan.ticktick.domain.repository.DraftToken
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskEditorViewModel(
    private val repository: TaskRepository,
    val clock: Clock,
    private val savedState: SavedStateHandle,
    private val drafts: UiStateRepository,
) : ViewModel() {
    private val parser = TaskDateParser(clock)
    private val restoredDraft = drafts.currentDraft("task")
    private var draftSession = restoredDraft?.token?.sessionId ?: UUID.randomUUID().toString()
    private var newDraftSession = restoredDraft == null
    private var draftToken: DraftToken? = restoredDraft?.token
    private val state = MutableStateFlow(restoreEditorState(if (restoredDraft != null) restoredDraft.payload else savedState.get<String>("editorDraft"), parser, repository.snapshot.value.lists))
    val uiState = state.asStateFlow()
    val lists = repository.snapshot
    private var saveJob: Job? = null
    private var writeJob: Job? = null
    private var closeAfterSave = false
    private var editVersion = 0L
    private var baseRevision: Long = restoredDraft?.baseRevision ?: savedState.get<Long>("editorBaseRevision") ?: 0L
    private var lastDraftPayload: String? = restoredDraft?.payload
    private var lastDraftBase: Long = baseRevision
    private var lastDraftSession = draftSession
    private fun setBaseRevision(value: Long) { baseRevision = value; savedState["editorBaseRevision"] = value }
    private var attachmentJob: Job? = null
    private var observedTask: Task? = repository.snapshot.value.tasks.find { it.id == state.value.id }

    init {
        // A crash after saving an older draft may leave newer typed text. Preserve it and the
        // old base revision; a conflict must be reviewed, never converted into a duplicate task.
        if (state.value.active && repository.snapshot.value.tasks.any { it.id == state.value.id })
            state.value = state.value.copy(existing = true)
        if (state.value.active) publish(state.value)
        viewModelScope.launch { repository.snapshot.collect { reconcileReminderChanges() } }
    }

    // Reminder actions own external schedule/status changes. Keep typed fields and their selection intact.
    private fun reconcileReminderChanges() {
        val current = state.value
        if (!current.active || !current.existing || current.saving) return
        val latest = repository.snapshot.value.tasks.find { it.id == current.id } ?: return
        val previous = observedTask?.takeIf { it.id == latest.id }
        observedTask = latest
        if (previous == null) return
        // Reminder actions and pinning own these fields. Merge them without rebasing over
        // another editor's content changes; those still fail the repository revision check.
        val externalFields = previous.copy(dueDate = latest.dueDate, dueTime = latest.dueTime,
            duration = latest.duration, reminders = latest.reminders, repeat = latest.repeat,
            completed = latest.completed, declined = latest.declined, pinned = latest.pinned,
            modifiedAt = latest.modifiedAt, revision = latest.revision, recurrence = latest.recurrence,
            checklist = if (previous.recurrence?.occurrenceId != latest.recurrence?.occurrenceId) latest.checklist else previous.checklist)
        // A completion gesture belongs to the version shown when it was tapped, not a newly advanced occurrence.
        val pendingStatusChange = current.completed != previous.completed || current.declined != previous.declined
        if (!pendingStatusChange && previous.revision == baseRevision && externalFields == latest) setBaseRevision(latest.revision)
        var next = current
        if (previous.schedule != latest.schedule) next = next.copy(
            manualDate = latest.dueDate, manualTime = latest.dueTime, dateManuallySet = true, contextDate = null,
            reminders = latest.reminders, repeat = latest.repeat, duration = latest.duration,
            panel = current.panel.takeUnless { it == EditorPanel.Date },
        )
        if (previous.completed != latest.completed || previous.declined != latest.declined)
            next = next.copy(completed = latest.completed, declined = latest.declined)
        if (previous.recurrence?.occurrenceId != latest.recurrence?.occurrenceId)
            next = next.copy(checklist = next.checklist.map { it.copy(completed = false) })
        if (next != current) publish(next)
    }

    fun beginNew(listId: String, contextDate: LocalDate?) {
        if (state.value.saving) return
        setBaseRevision(0)
        observedTask = null
        draftSession = UUID.randomUUID().toString()
        newDraftSession = true
        saveJob?.cancel()
        attachmentJob?.cancel()
        publish(TaskEditorState(active = true, id = UUID.randomUUID().toString(), listId = listId, contextDate = contextDate))
    }

    fun beginExisting(id: String) {
        if (state.value.saving) return
        val task = repository.snapshot.value.tasks.find { it.id == id } ?: return
        observedTask = task
        draftSession = UUID.randomUUID().toString()
        newDraftSession = true
        setBaseRevision(task.revision)
        val inlineTags = parser.tags(titleWithoutListTokens(task.title, repository.snapshot.value.lists.find { it.id == task.listId }))
        saveJob?.cancel()
        attachmentJob?.cancel()
        publish(TaskEditorState(
            active = true, id = id, existing = true, fullScreen = true,
            title = TextFieldValue(task.title), description = TextFieldValue(task.description), listId = task.listId,
            priority = task.priority, manualDate = task.dueDate, manualTime = task.dueTime, dateManuallySet = task.dueDate != null,
            parsedTags = inlineTags,
            tags = task.tags.filterNot { tag -> inlineTags.any { it.name.equals(tag, ignoreCase = true) } },
            checklist = task.checklist, checklistMode = task.checklist.isNotEmpty(), attachments = task.attachments,
            isNote = task.isNote, completed = task.completed, declined = task.declined, reminders = task.reminders, repeat = task.repeat, duration = task.duration,
        ))
    }

    private fun publish(next: TaskEditorState) {
        state.value = next
        if (next.recoveryFailed) return
        savedState["editorDraft"] = if (next.active) encodeEditorState(next) else null
        val payload = if (next.active && (!next.existing || next.changed || next.attachmentLoading)) encodeEditorState(next) else null
        if (payload != lastDraftPayload || baseRevision != lastDraftBase || draftSession != lastDraftSession) {
            draftToken = drafts.stageDraft("task", draftSession, next.id, baseRevision, payload, newSession = newDraftSession)
            newDraftSession = false
            lastDraftPayload = payload
            lastDraftBase = baseRevision
            lastDraftSession = draftSession
        }
    }

    private fun edit(transform: (TaskEditorState) -> TaskEditorState) {
        editVersion++
        publish(transform(state.value).copy(changed = true, error = null))
        saveJob?.cancel()
        if (state.value.existing) saveJob = viewModelScope.launch {
            delay(350)
            if (state.value.title.text.isNotBlank() && !state.value.attachmentLoading) requestSave(false)
        }
    }

    fun setTitle(value: TextFieldValue) {
        val previous = state.value
        if (value.text == previous.title.text) { publish(previous.copy(title = value)); return }
        edit { withTitle(it, value) }
        val cursor = value.selection.start
        if (value.selection.collapsed && cursor > 0 && value.text[cursor - 1] == '~' &&
            (cursor == 1 || value.text[cursor - 2].isWhitespace()) &&
            (previous.title.selection.start != cursor || previous.title.text.getOrNull(cursor - 1) != '~')) {
            panel(EditorPanel.Lists)
        }
    }

    private fun withTitle(current: TaskEditorState, value: TextFieldValue, listId: String = current.listId): TaskEditorState {
        val source = titleWithoutListTokens(value.text, repository.snapshot.value.lists.find { it.id == listId })
        val prediction = parser.parse(source)?.let { parsed ->
            current.prediction?.takeIf { it.fingerprint == parsed.fingerprint }
                ?.let { parsed.copy(date = it.date, time = it.time) } ?: parsed
        }
        return current.copy(title = value, listId = listId, prediction = prediction, parsedTags = parser.tags(source),
            suppressedPrediction = current.suppressedPrediction.takeIf { prediction?.fingerprint == current.prediction?.fingerprint })
    }

    fun beginTagEntry(name: String? = null) {
        if (name != null && parser.tags("#$name").singleOrNull()?.name != name) {
            reportError("This saved tag can't be edited inline yet. Its saved value has been kept.")
            return
        }
        val current = state.value
        val cursor = current.title.selection.max
        val existing = current.parsedTags.firstOrNull {
            if (name != null) it.name.equals(name, ignoreCase = true) else cursor in it.span.start..it.span.end
        }
        if (existing != null) {
            val selection = if (name != null) TextRange(existing.span.start + 1, existing.span.end) else TextRange(existing.span.end)
            publish(current.copy(title = current.title.copy(selection = selection, composition = null), panel = null))
        } else {
            val value = insertTitleToken(if (name == null) "#" else "#$name")
            setTitle(if (name == null) value else value.copy(selection = TextRange(value.selection.end - name.length, value.selection.end)))
            if (name != null) edit { it.copy(tags = it.tags.filterNot { tag -> tag.equals(name, ignoreCase = true) }) }
            panel(null)
        }
    }

    fun beginListEntry() {
        val current = state.value
        val token = repository.snapshot.value.lists.find { it.id == current.listId }?.tokenSpans(current.title.text)?.firstOrNull()
        if (token != null) publish(current.copy(title = current.title.copy(selection = TextRange(token.end), composition = null)))
        else setTitle(insertTitleToken("~"))
        panel(EditorPanel.Lists)
    }

    // Insert after the selection, preserving source text; separate tokens from surrounding words.
    private fun insertTitleToken(token: String): TextFieldValue {
        val title = state.value.title
        val cursor = title.selection.max
        if (token.length == 1 && cursor > 0 && title.text[cursor - 1] == token.single() &&
            (cursor == 1 || title.text[cursor - 2].isWhitespace())) {
            return title.copy(selection = TextRange(cursor), composition = null)
        }
        val before = title.text.substring(0, cursor)
        val after = title.text.substring(cursor)
        val prefix = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
        val suffix = if (after.isNotEmpty() && !after.first().isWhitespace()) " " else ""
        return TextFieldValue(before + prefix + token + suffix + after, TextRange(cursor + prefix.length + token.length))
    }

    fun setDescription(value: TextFieldValue) {
        if (value.text == state.value.description.text) publish(state.value.copy(description = value))
        else edit { it.copy(description = value) }
    }

    fun beginSchedule(id: String) {
        beginExisting(id)
        if (state.value.id == id) publish(state.value.copy(scheduleOnly = true, panel = EditorPanel.Date))
    }
    fun cancelSchedule() { if (state.value.scheduleOnly) discard() else panel(null) }
    fun applySchedule(schedule: TaskSchedule) {
        schedule.validationError(clock, requireFutureReminders = schedule != state.value.toTask().schedule)?.let { reportError(it); return }
        edit { it.copy(manualDate = schedule.date, manualTime = schedule.time, dateManuallySet = true,
            contextDate = null, reminders = schedule.reminders, repeat = schedule.repeat, duration = schedule.duration, panel = null) }
        if (state.value.scheduleOnly) saveAndClose()
    }
    fun tapDate() {
        val current = state.value
        if (current.activePrediction != null) edit { it.copy(suppressedPrediction = current.activePrediction!!.fingerprint, contextDate = null) }
        else panel(EditorPanel.Date)
    }
    fun setPriority(value: TaskPriority) = edit { it.copy(priority = value, panel = null) }
    fun setList(id: String) {
        val list = repository.snapshot.value.lists.find { it.id == id } ?: return
        val current = state.value
        var title = current.title
        val oldTokens = repository.snapshot.value.lists.find { it.id == current.listId }?.tokenSpans(title.text).orEmpty()
        val cursor = title.selection.max
        val activeToken = oldTokens.firstOrNull { cursor in it.start..it.end }
        val marker = Regex("(?<!\\S)~\\S*").findAll(title.text)
            .firstOrNull { cursor in (it.range.first + 1)..(it.range.last + 1) }
            ?.let { TextSpan(it.range.first, it.range.last + 1) }
        var target = activeToken ?: marker ?: oldTokens.firstOrNull()
        if (target == null && !current.fullScreen) {
            title = insertTitleToken("~")
            target = TextSpan(title.selection.end - 1, title.selection.end)
        }
        if (target == null) {
            // The detail header changes metadata directly unless there is an inline token to update.
            edit { it.copy(listId = id, panel = null) }
            return
        }
        var start = target.start
        var end = target.end
        var text = title.text
        oldTokens.filterNot { it == target }.sortedByDescending { it.start }.forEach { old ->
            text = text.removeRange(old.start, old.end)
            if (old.end <= start) { start -= old.end - old.start; end -= old.end - old.start }
        }
        val after = text.substring(end)
        val separator = if (after.startsWith(" ")) "" else " "
        val updated = TextFieldValue(text.substring(0, start) + list.editorToken + separator + after,
            TextRange(start + list.editorToken.length + 1))
        edit { withTitle(it, updated, id).copy(panel = null) }
    }
    fun setNlpEnabled(enabled: Boolean) = edit { it.copy(nlpEnabled = enabled) }
    fun toggleNote() = edit { it.copy(isNote = !it.isNote, panel = null) }
    fun showFullScreen() { publish(state.value.copy(fullScreen = true, panel = null)) }
    fun panel(panel: EditorPanel?) { publish(state.value.copy(panel = panel)) }
    fun keepEditing() { publish(state.value.copy(confirmDiscard = false)) }
    fun requestDiscard() { if (!state.value.saving) publish(state.value.copy(error = null, panel = null, confirmDiscard = true)) }
    fun reportError(message: String) { publish(state.value.copy(error = message)) }
    fun clearError() { publish(state.value.copy(error = null)) }

    fun toggleChecklist() = edit { it.copy(checklistMode = !it.checklistMode, fullScreen = true) }
    fun addChecklistItem() = edit { it.copy(checklist = it.checklist + ChecklistItem(UUID.randomUUID().toString(), "")) }
    fun editChecklistItem(id: String, text: String) = edit { it.copy(checklist = it.checklist.map { item -> if (item.id == id) item.copy(text = text) else item }) }
    fun toggleChecklistItem(id: String) = edit { it.copy(checklist = it.checklist.map { item -> if (item.id == id) item.copy(completed = !item.completed) else item }) }
    fun removeChecklistItem(id: String) = edit { it.copy(checklist = it.checklist.filterNot { item -> item.id == id }) }
    fun moveChecklistItem(id: String, direction: Int) = edit {
        val items = it.checklist.toMutableList()
        val from = items.indexOfFirst { item -> item.id == id }
        val to = from + direction
        if (from >= 0 && to in items.indices) items.add(to, items.removeAt(from))
        it.copy(checklist = items)
    }
    fun importAttachment(editorId: String?, load: suspend () -> TaskAttachment) {
        if (!state.value.active || state.value.id != editorId || state.value.attachmentLoading) return
        publish(state.value.copy(attachmentLoading = true, panel = null))
        attachmentJob = viewModelScope.launch {
            try {
                val attachment = load()
                if (state.value.active && state.value.id == editorId) {
                    edit { it.copy(attachments = it.attachments + attachment, attachmentLoading = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (state.value.active && state.value.id == editorId) reportError("Couldn't attach that file. Please choose it again.")
            } finally {
                if (state.value.active && state.value.id == editorId) publish(state.value.copy(attachmentLoading = false))
            }
        }
    }
    fun removeAttachment(id: String) = edit { it.copy(attachments = it.attachments.filterNot { attachment -> attachment.id == id }) }
    fun applyTemplate(name: String, items: List<String>) = edit {
        val title = it.title.takeIf { value -> value.text.isNotBlank() } ?: TextFieldValue(name, TextRange(name.length))
        it.copy(title = title, checklistMode = true, checklist = it.checklist + items.map { text -> ChecklistItem(UUID.randomUUID().toString(), text) }, panel = null)
    }

    private suspend fun save(): Boolean {
        if (state.value.recoveryFailed) return false
        reconcileReminderChanges()
        val current = state.value
        if (current.attachmentLoading) { reportError("Please wait for the attachment to finish loading."); return false }
        if (current.title.text.isBlank()) { reportError("Enter a task title"); return false }
        if (current.existing && repository.snapshot.value.tasks.none { it.id == current.id }) {
            reportError("This task was removed. Your draft is still available."); return false
        }
        if (current.existing && !current.changed) return true
        val version = editVersion
        val task = current.toTask().copy(pinned = observedTask?.pinned ?: false, revision = baseRevision)
        publish(current.copy(saving = true))
        return try {
            val token = draftToken
            taskWriteResult { drafts.flushDrafts(); repository.save(task, token) }.fold(
                onSuccess = { saved ->
                    setBaseRevision(saved.revision)
                    observedTask = saved
                    var nextState = state.value.copy(existing = true, changed = editVersion != version, error = null)
                    if (saved.schedule != task.schedule || saved.completed != task.completed || saved.declined != task.declined) {
                        nextState = nextState.copy(manualDate = saved.dueDate, manualTime = saved.dueTime, dateManuallySet = true,
                            contextDate = null, duration = saved.duration, reminders = saved.reminders, repeat = saved.repeat,
                            completed = saved.completed, declined = saved.declined,
                            checklist = nextState.checklist.map { it.copy(completed = false) })
                    }
                    publish(nextState)
                    true
                },
                onFailure = { reportError(it.message ?: "Couldn't save this task. Please try again."); false },
            )
        } finally { publish(state.value.copy(saving = false)) }
    }

    private fun requestSave(close: Boolean) {
        closeAfterSave = closeAfterSave || close
        if (writeJob?.isActive == true) return
        writeJob = viewModelScope.launch {
            var success = false
            try {
                do { success = save() } while (success && closeAfterSave && state.value.changed)
                if (success && closeAfterSave && !state.value.changed) discard()
            } finally {
                closeAfterSave = false
                if (success && state.value.active && state.value.existing && state.value.changed && state.value.error == null) {
                    saveJob?.cancel()
                    saveJob = viewModelScope.launch { delay(350); requestSave(false) }
                }
            }
        }
    }

    fun saveAndClose() { saveJob?.cancel(); requestSave(true) }
    fun toggleComplete() {
        if (state.value.saving) return
        editVersion++
        publish(state.value.copy(completed = !state.value.completed && !state.value.declined, declined = false, changed = true))
        if (state.value.existing) saveAndClose()
    }
    fun back() {
        val current = state.value
        when {
            current.panel != null -> panel(null)
            current.fullScreen && !current.existing -> publish(current.copy(fullScreen = false))
            current.existing -> saveAndClose()
            current.changed -> publish(current.copy(confirmDiscard = true))
            else -> discard()
        }
    }
    fun discard() { if (state.value.saving) return; setBaseRevision(0); saveJob?.cancel(); attachmentJob?.cancel(); observedTask = null; publish(TaskEditorState()) }
    fun delete() {
        if (state.value.saving || writeJob?.isActive == true) return
        saveJob?.cancel()
        val id = state.value.id
        publish(state.value.copy(saving = true))
        writeJob = viewModelScope.launch {
            try {
                val token = draftToken
                val result = taskWriteResult { drafts.flushDrafts(); repository.deleteTask(id, token) }
                publish(state.value.copy(saving = false))
                result.fold(onSuccess = { discard() }, onFailure = { reportError(it.message ?: "Couldn't delete this task.") })
            } finally { publish(state.value.copy(saving = false)) }
        }
    }
}
