package com.niranjan.ticktick.feature.taskeditor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.niranjan.ticktick.domain.model.ChecklistItem
import com.niranjan.ticktick.domain.model.TaskAttachment
import com.niranjan.ticktick.domain.model.TaskPriority
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.nlp.TaskDateParser
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

// Versioned editor reconstruction shared by Room drafts and Android saved state; never media bytes.
internal fun encodeEditorState(state: TaskEditorState): String = JSONObject().apply {
    put("version", 2)
    put("id", state.id); put("existing", state.existing); put("fullScreen", state.fullScreen)
    put("schedule", state.schedule.toJson()); put("scheduleOnly", state.scheduleOnly)
    put("datePanel", state.panel == EditorPanel.Date)
    put("title", state.title.text); put("selectionStart", state.title.selection.start); put("selectionEnd", state.title.selection.end)
    put("description", state.description.text); put("descriptionStart", state.description.selection.start); put("descriptionEnd", state.description.selection.end)
    put("list", state.listId); put("priority", state.priority.name)
    put("contextDate", state.contextDate?.toString()); put("date", state.manualDate?.toString()); put("time", state.manualTime?.toString())
    put("manual", state.dateManuallySet); put("suppressed", state.suppressedPrediction); put("nlp", state.nlpEnabled)
    put("note", state.isNote); put("completed", state.completed); put("reminder", state.hasReminder); put("changed", state.changed)
    put("declined", state.declined)
    put("attachmentPending", state.attachmentLoading)
    state.prediction?.let { prediction ->
        put("prediction", JSONObject().put("date", prediction.date.toString()).put("time", prediction.time?.toString())
            .put("fingerprint", prediction.fingerprint).put("spans", JSONArray().apply {
                prediction.spans.forEach { put(JSONObject().put("start", it.start).put("end", it.end)) }
            }))
    }
    put("checklistMode", state.checklistMode); put("tags", JSONArray(state.tags))
    put("checklist", JSONArray().apply { state.checklist.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("done", it.completed)) } })
    put("attachments", JSONArray().apply { state.attachments.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("uri", it.uri).put("mime", it.mimeType).put("size", it.sizeBytes)) } })
}.toString()

internal fun restoreEditorState(json: String?, parser: TaskDateParser, lists: List<TaskList>): TaskEditorState {
    if (json == null) return TaskEditorState()
    return runCatching {
        val data = JSONObject(json)
        require(data.optInt("version", 1) in 1..2)
        val title = data.getString("title")
        val listId = data.optString("list", "inbox")
        val source = titleWithoutListTokens(title, lists.find { it.id == listId })
        val description = data.optString("description")
        val checklist = data.optJSONArray("checklist") ?: JSONArray()
        val attachments = data.optJSONArray("attachments") ?: JSONArray()
        val tags = data.optJSONArray("tags") ?: JSONArray()
        val schedule = data.optJSONObject("schedule")?.let(::scheduleFromJson)
            ?: com.niranjan.ticktick.domain.model.TaskSchedule(reminders = com.niranjan.ticktick.domain.model.ReminderConfig(if (data.optBoolean("reminder")) listOf(0L) else emptyList()))
        TaskEditorState(
            active = true, id = data.getString("id"), existing = data.optBoolean("existing"), fullScreen = data.optBoolean("fullScreen"),
            title = TextFieldValue(title, TextRange(data.optInt("selectionStart").coerceIn(0, title.length), data.optInt("selectionEnd").coerceIn(0, title.length))),
            description = TextFieldValue(description, TextRange(data.optInt("descriptionStart").coerceIn(0, description.length), data.optInt("descriptionEnd").coerceIn(0, description.length))),
            listId = listId, priority = TaskPriority.valueOf(data.optString("priority", "None")),
            contextDate = data.optString("contextDate").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            manualDate = data.optString("date").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            manualTime = data.optString("time").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
            dateManuallySet = data.optBoolean("manual"), prediction = data.optJSONObject("prediction")?.let { p ->
                val spans = p.getJSONArray("spans")
                com.niranjan.ticktick.domain.nlp.ParsedSchedule(LocalDate.parse(p.getString("date")),
                    p.optString("time").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
                    (0 until spans.length()).map { spans.getJSONObject(it).let { s -> com.niranjan.ticktick.domain.nlp.TextSpan(s.getInt("start"), s.getInt("end")) } },
                    p.getString("fingerprint"))
            } ?: parser.parse(source), parsedTags = parser.tags(source),
            suppressedPrediction = data.optString("suppressed").takeIf { it.isNotEmpty() }, nlpEnabled = data.optBoolean("nlp", true),
            isNote = data.optBoolean("note"), completed = data.optBoolean("completed"), declined = data.optBoolean("declined"), changed = data.optBoolean("changed"),
            reminders = schedule.reminders, repeat = schedule.repeat, duration = schedule.duration,
            scheduleOnly = data.optBoolean("scheduleOnly"), panel = if (data.optBoolean("datePanel")) EditorPanel.Date else null,
            error = if (data.optBoolean("attachmentPending")) "An attachment import was interrupted. Please choose the file again." else null,
            checklistMode = data.optBoolean("checklistMode"), tags = (0 until tags.length()).map { tags.getString(it) },
            checklist = (0 until checklist.length()).map { checklist.getJSONObject(it).let { row -> ChecklistItem(row.getString("id"), row.getString("text"), row.getBoolean("done")) } },
            attachments = (0 until attachments.length()).map { attachments.getJSONObject(it).let { row -> TaskAttachment(row.getString("id"), row.getString("name"), row.getString("uri"), row.getString("mime"), if (row.has("size") && !row.isNull("size")) row.getLong("size") else null) } },
        )
    }.getOrElse { TaskEditorState(active = true, recoveryFailed = true,
        error = "This saved draft couldn't be read. It has been kept in storage; you can discard it to start again.") }
}
