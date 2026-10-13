package com.niranjan.ticktick.platform.backup

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.niranjan.ticktick.data.local.*
import com.niranjan.ticktick.data.mapper.*
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.repository.UiStateRepository
import java.io.File
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

class BackupPreview internal constructor(internal val json: String, val tasks: Int, val habits: Int,
    val focusSessions: Int, val unavailableMedia: Int, val createdAt: String)

/** Local JSON contains metadata, never transferable provider grants or attachment bytes. */
class BackupManager(private val context: Context, private val database: TickTickDatabase,
    private val ui: UiStateRepository, private val beforeReplace: suspend () -> Unit,
    private val afterReplace: suspend (Boolean) -> Unit) {
    private val lock = Mutex()
    val restoring = database.restoring
    val outcome = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val recoveryFile get() = File(context.noBackupFilesDir, "before-restore.json")
    val recoveryAvailable get() = recoveryFile.exists()
    private val tables = listOf("task_lists", "task_tags", "tasks", "task_tag_links", "task_checklist", "task_attachments",
        "task_snoozes", "saved_filters", "filter_rules", "task_store_metadata", "task_recurrence", "task_occurrence_history",
        "reminder_occurrences", "reminder_deliveries", "reminder_preferences", "habit_sections", "habit_settings", "habits",
        "habit_schedule_history", "habit_checkins", "ui_preferences", "editor_drafts", "focus_session", "focus_history")
    private val maxBytes = 32 * 1024 * 1024
    private fun mediaUris(content: JSONObject): Set<String> {
        val media = content.getJSONArray("task_attachments")
        val result = (0 until media.length()).map { media.getJSONObject(it).getString("uri") }.toMutableSet()
        val drafts = content.getJSONArray("editor_drafts")
        for (i in 0 until drafts.length()) {
            val row = drafts.getJSONObject(i)
            if (row.getString("key") == "task" && !row.isNull("payload")) {
                val attachments = JSONObject(row.getString("payload")).getJSONArray("attachments")
                for (j in 0 until attachments.length()) result += attachments.getJSONObject(j).getString("uri")
            }
        }
        return result
    }
    fun recoveryAttachmentUris(): Set<String>? = runCatching {
        if (!recoveryFile.exists()) emptySet() else mediaUris(JSONObject(recoveryFile.readText()).getJSONObject("tables"))
    }.getOrNull()
    suspend fun export(uri: Uri, recovery: Boolean = false) = withContext(Dispatchers.IO) {
        lock.withLock {
            val json = if (recovery) recoveryFile.readText() else {
                ui.flushDrafts()
                database.operationLock.withLock { database.withTransaction { snapshot(database.openHelper.writableDatabase).toString() } }
            }
            require(json.toByteArray(Charsets.UTF_8).size <= maxBytes) { "This backup exceeds the 32 MB limit." }
            requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "Couldn't open the destination." }
                .bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        }
    }
    suspend fun preview(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= maxBytes) { "Backup exceeds the 32 MB limit." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        validate(bytes.toString(Charsets.UTF_8))
    }
    private suspend fun validate(json: String): BackupPreview {
        val root = JSONObject(json)
        require(root.getString("application") == "com.niranjan.ticktick" && root.getInt("format") == 1 && root.getInt("schema") == 6 && !root.getBoolean("mediaIncluded")) {
            "Unsupported backup version. Use a backup exported by this app version."
        }
        Instant.parse(root.getString("createdAt"))
        val content = root.getJSONObject("tables")
        require(content.keys().asSequence().toSet() == tables.toSet()) { "Backup tables are missing or unsupported." }
        val staging = Room.inMemoryDatabaseBuilder(context, TickTickDatabase::class.java).build()
        try {
            staging.withTransaction {
                val sql = staging.openHelper.writableDatabase
                insert(sql, content)
                sql.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "Backup contains broken references." } }
                val snapshot = staging.tasks().readSnapshot()
                require(snapshot.lists.any { it.id == "inbox" } && staging.tasks().metadata()?.initialized == true) { "Backup has no initialized Inbox." }
                staging.tasks().lists().forEach { require(it.id.isNotBlank() && it.name.isNotBlank() && it.nameKey == it.name.storageKey()) }
                staging.tasks().tags().forEach { require(it.name.isNotBlank() && it.nameKey == it.name.storageKey()) }
                staging.tasks().filters().forEach { require(it.id.isNotBlank() && it.name.isNotBlank() && it.nameKey == it.name.storageKey()) }
                snapshot.tasks.forEach { task ->
                    require(task.id.isNotBlank() && task.title.isNotBlank() && task.revision in 0..1_000_000_000_000_000L)
                    require(task.schedule.validationError(Clock.systemDefaultZone(), false) == null) { "Backup contains an invalid task schedule." }
                    task.attachments.forEach { require(it.id.isNotBlank() && (it.sizeBytes == null || it.sizeBytes >= 0)) }
                }
                snapshot.occurrenceHistory.forEach { ZoneId.of(it.zoneId) }
                snapshot.filters.forEach { filter ->
                    require(filter.rules.isNotEmpty())
                    filter.rules.forEach { rule -> when (rule.field) {
                        FilterField.Date -> FilterDate.valueOf(rule.value)
                        FilterField.Priority -> TaskPriority.valueOf(rule.value)
                        FilterField.List -> require(snapshot.lists.any { it.id == rule.value })
                        FilterField.Tag -> require(rule.value.isNotBlank())
                    } }
                }
                val habits = staging.habits().readHabits()
                (habits.habits + habits.scheduleHistory.map { it.configuration }).forEach { habit ->
                    require(habit.id.isNotBlank() && habit.name.isNotBlank() && habit.revision in 0..1_000_000_000_000_000L)
                    require(habit.weekdays.isNotEmpty() && habit.frequencyCount in 1..30)
                    require(habit.frequency != HabitFrequency.Weekly || habit.frequencyCount <= 7)
                    require(habit.amount == null || habit.amount in 1..999999 && habit.unit.isNotBlank())
                    require(habit.recordAmount in 1..999999 && (habit.goalDays == null || habit.goalDays in 1..999))
                    require(habit.reminders.size <= 32 && (!habit.constantReminder || habit.reminders.isNotEmpty()))
                }
                require(habits.habits.all { it.section in habits.sections })
                habits.progress.values.forEach { dates -> require(dates.values.all { it in 0..999999 }) }
                staging.habits().habits().forEach { require(it.id == it.configuration.decodeHabit().id) }
                staging.habits().history().forEach { require(it.habitId == it.configuration.decodeHabit().id) }
                staging.habits().checkIns().forEach { Instant.parse(it.updatedAt) }
                staging.uiState().preferences().forEach { require(it.format == 1); validateSavedPreference(it.key, it.payload) }
                staging.uiState().drafts().forEach { row ->
                    require(row.format == 1 && row.version >= 0 && row.baseRevision >= 0)
                    row.payload?.let { payload ->
                        val j = JSONObject(payload)
                        when (row.key) {
                            "task" -> require(!com.niranjan.ticktick.feature.taskeditor.restoreEditorState(payload,
                                com.niranjan.ticktick.domain.nlp.TaskDateParser(Clock.systemDefaultZone()), snapshot.lists).recoveryFailed)
                            "habit" -> { require(j.getInt("version") == 1); j.getString("habit").decodeHabit() }
                            else -> error("Unsupported draft type.")
                        }
                    }
                }
                staging.focus().session()?.payload?.decodeFocus()
                staging.focus().history().forEach {
                    FocusMode.valueOf(it.mode)
                    require(it.id.isNotBlank() && it.elapsedMs in 0..1_577_880_000_000L && it.startedAt >= 0 && it.endedAt >= 0)
                }
                staging.reminders().preferences()?.resumeDate?.let(LocalDate::parse)
                staging.reminders().deliveries().forEach {
                    require(it.status in setOf("pending", "delivered", "dismissed", "cancelled", "suppressed"))
                    require(it.revision in 0..1_000_000_000_000_000L && it.repeatCount in 0..4)
                }
            }
            val unavailable = mediaUris(content).count { uri ->
                require(Uri.parse(uri).scheme == "content") { "Unsupported attachment address." }
                !runCatching { context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false }.getOrDefault(false)
            }
            return BackupPreview(json, staging.tasks().tasks().size, staging.habits().habits().size,
                staging.focus().history().size, unavailable, root.getString("createdAt"))
        } finally { staging.close() }
    }
    suspend fun restore(preview: BackupPreview) = withContext(Dispatchers.IO + NonCancellable) {
        lock.withLock {
            validate(preview.json)
            ui.flushDrafts()
            var replaced = false
            try {
                database.operationLock.withLock {
                    restoring.value = true
                    val old = database.withTransaction { snapshot(database.openHelper.writableDatabase).toString() }
                    val temporary = File(context.noBackupFilesDir, "before-restore.tmp")
                    temporary.outputStream().use { stream -> stream.write(old.toByteArray(Charsets.UTF_8)); stream.fd.sync() }
                    check(temporary.renameTo(recoveryFile)) { "Couldn't save the recovery backup. Current data was not replaced." }
                    beforeReplace()
                    database.withTransaction {
                        val sql = database.openHelper.writableDatabase
                        tables.asReversed().forEach { sql.execSQL("DELETE FROM `$it`") }
                        insert(sql, JSONObject(preview.json).getJSONObject("tables"))
                        val revision = System.currentTimeMillis()
                        sql.execSQL("UPDATE tasks SET revision = revision + ?", arrayOf(revision))
                        sql.execSQL("UPDATE reminder_deliveries SET revision = revision + ?, dirty = 1, alertAgain = 0", arrayOf(revision))
                        database.habits().putHabits(database.habits().habits().map { row -> row.copy(configuration =
                            row.configuration.decodeHabit().let { it.copy(revision = it.revision + revision).encodeHabit() }) })
                        database.focus().session()?.let { row ->
                            val value = row.payload.decodeFocus()
                            database.focus().putSession(row.copy(payload = value.copy(
                                phase = if (value.phase == FocusPhase.Running) FocusPhase.Paused else value.phase,
                                startedAt = 0, bootCount = -1, completionPending = false).encodeFocus()))
                        }
                        database.uiState().drafts().forEach { database.uiState().putDraft(it.copy(sessionId = UUID.randomUUID().toString())) }
                    }
                    replaced = true; database.generation += 1
                    (ui as? com.niranjan.ticktick.data.repository.RoomUiStateRepository)?.clearPendingDraftsAfterRestore()
                }
            } catch (e: Exception) {
                outcome.value = "Restore failed. Your previous database is intact. Check available storage and retry."
                throw e
            } finally {
                try {
                    afterReplace(replaced)
                    if (replaced) outcome.value = "Backup restored. Running Focus timers are paused. A pre-restore recovery copy is available in Settings."
                } catch (e: Exception) {
                    outcome.value = if (replaced) "Data was restored, but reloading failed. Retry loading or restart the app. Your pre-restore recovery copy is available in Settings."
                        else "Restore failed. Your previous database is intact. Restart the app to reload it."
                    throw e
                } finally { restoring.value = false }
            }
        }
    }
    private fun snapshot(sql: SupportSQLiteDatabase): JSONObject {
        val content = JSONObject()
        tables.forEach { table ->
            val rows = JSONArray()
            sql.query("SELECT * FROM `$table`").use { cursor -> while (cursor.moveToNext()) {
                val row = JSONObject()
                cursor.columnNames.forEachIndexed { i, name -> row.put(name, when (cursor.getType(i)) {
                    android.database.Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                    android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                    android.database.Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                    else -> error("Unsupported backup field.")
                }) }
                if (table == "focus_session") {
                    val value = row.getString("payload").decodeFocus()
                    val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
                    if (value.phase == FocusPhase.Running && value.bootCount >= 0 && value.bootCount == boot) {
                        val now = SystemClock.elapsedRealtime()
                        row.put("payload", value.copy(elapsedMs = value.elapsed(now), startedAt = now).encodeFocus())
                    }
                }
                rows.put(row)
            } }
            content.put(table, rows)
        }
        return JSONObject().put("application", "com.niranjan.ticktick").put("format", 1).put("schema", 6)
            .put("createdAt", Instant.now().toString()).put("mediaIncluded", false).put("tables", content)
    }
    private fun insert(sql: SupportSQLiteDatabase, content: JSONObject) {
        var total = 0
        val booleans = setOf("completed", "declined", "isNote", "pinned", "explicit", "matchAll", "initialized", "constant", "missed", "dirty", "alertAgain", "legacyImported", "sortByCheckInStatus", "showInTodayAndNext7Days", "countInAppBadge")
        tables.forEach { table ->
            val columns = mutableMapOf<String, String>()
            sql.query("PRAGMA table_info(`$table`)").use { cursor -> while (cursor.moveToNext()) {
                columns[cursor.getString(cursor.getColumnIndexOrThrow("name"))] = cursor.getString(cursor.getColumnIndexOrThrow("type"))
            } }
            val rows = content.getJSONArray(table)
            total += rows.length(); require(total <= 100_000) { "Backup contains too many records." }
            val singleton = table in setOf("task_store_metadata", "reminder_preferences", "habit_settings", "focus_session")
            require(!singleton || rows.length() <= 1)
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                require(!singleton || row.getInt("id") == 1)
                require(row.keys().asSequence().toSet() == columns.keys) { "Invalid backup columns." }
                val values: Array<Any?> = columns.map { (key, type) ->
                    val value = row.get(key)
                    when {
                        value == JSONObject.NULL -> null
                        type == "INTEGER" -> {
                            require(value is Int || value is Long)
                            val number = (value as Number).toLong()
                            if (key in booleans) require(number in 0..1)
                            number
                        }
                        type == "TEXT" -> { require(value is String && value.length <= 1_000_000); value }
                        else -> error("Unsupported field type.")
                    }
                }.toTypedArray<Any?>()
                val names = columns.keys.joinToString(",") { "`$it`" }
                sql.execSQL("INSERT INTO `$table` ($names) VALUES (${columns.keys.joinToString(",") { "?" }})", values)
            }
        }
    }
}
