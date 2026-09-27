package com.niranjan.ticktick.data.repository

import com.niranjan.ticktick.data.local.*
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.model.resolvedDue
import com.niranjan.ticktick.domain.repository.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Transaction-local reminder state machine. Caller owns the database transaction and write lock. */
internal class ReminderStore(private val dao: ReminderDao, private val clock: Clock) {
    private val now get() = clock.millis()
    private suspend fun preferences(): ReminderPreferencesEntity = dao.preferences() ?: ReminderPreferencesEntity(
        resumeDate = LocalDate.now(clock).plusDays(1).toString(),
    ).also { dao.putPreferences(it) }
    private suspend fun paused() = preferences().resumeDate?.let(LocalDate::parse)?.let { it > LocalDate.now(clock) } == true

    suspend fun importPause(legacy: AlertPause?) {
        val previous = preferences()
        if (!previous.legacyImported) {
            dao.putPreferences(previous.copy(resumeDate = if (legacy != null) legacy.resumeDate?.toString() else previous.resumeDate, legacyImported = true))
            markAllDirty(invalidate = true)
        }
    }

    suspend fun setPause(pause: AlertPause) {
        dao.putPreferences(preferences().copy(resumeDate = pause.resumeDate?.toString(), legacyImported = true))
        markAllDirty(invalidate = true)
    }

    private suspend fun markAllDirty(invalidate: Boolean = false) {
        dao.deliveries().forEach { dao.putDelivery(it.copy(dirty = true, alertAgain = !invalidate && it.alertAgain && it.dirty,
            revision = it.revision + if (invalidate) 1 else 0)) }
    }

    /** Persist desired alarms/cancellations in the same commit as their task mutation. */
    suspend fun sync(snapshot: TaskSnapshot, before: TaskSnapshot? = null, habits: HabitSnapshot = HabitSnapshot(), beforeHabits: HabitSnapshot = habits) {
        val source = snapshot.withHabitReminders(habits, clock)
        val previous = before?.withHabitReminders(beforeHabits, clock)
        preferences()
        val current = dao.occurrences().associateBy { it.taskId }
        val desiredIds = mutableSetOf<String>()
        source.tasks.filter { it.isActive && !it.isNote && it.dueDate != null }.forEach { task ->
            val snooze = snapshot.snoozedUntil[task.id]
            val offsets = task.reminders.offsetsMinutes.distinct().sorted()
            if (offsets.isEmpty() && snooze == null) return@forEach
            desiredIds += task.id
            // Zone is not part of occurrence identity: timezone changes move pending wall-clock alarms,
            // but must never revive an occurrence the user has already dismissed.
            val scheduleFingerprint = listOf(task.dueDate, task.dueTime, task.duration, offsets,
                task.reminders.constant, task.reminders.dateOnlyTime, task.repeat.unit,
                task.repeat.interval, task.repeat.basis, task.repeat.weekdays.sortedBy { it.value },
                task.repeat.dates.sorted(), snooze).joinToString("|")
            val fingerprint = scheduleFingerprint + (task.recurrence?.let { "|occurrence:${it.occurrenceId}" } ?: "")
            val old = current[task.id]
            val occurrence = if (old?.fingerprint == fingerprint) old
            else if (old?.fingerprint == scheduleFingerprint && task.recurrence != null) {
                // Adopt schema-2 delivery state when initializing recurrence identities on upgrade.
                // In particular, a dismissed occurrence must not become a new reminder.
                old.copy(fingerprint = fingerprint).also { dao.putOccurrence(it) }
            } else {
                cancelTask(task.id)
                ReminderOccurrenceEntity(task.id, UUID.randomUUID().toString(), fingerprint).also { dao.putOccurrence(it) }
            }
            val base = task.schedule.resolvedDue(clock.zone)!!.toInstant()
            val target = habitReminderTarget(task.id)
            val habit = target?.let { t -> habits.habits.find { it.id == t.habitId } }
            val triggers = if (habit != null) habit.reminders.map { time -> time.toString() to
                target.date.atTime(time).atZone(clock.zone).withEarlierOffsetAtOverlap().toInstant() }
                .filter { (_, instant) -> instant >= habit.remindersChangedAt }
            else if (snooze != null) listOf("snooze" to snooze) else offsets.map { it.toString() to base.minusSeconds(Math.multiplyExact(it, 60L)) }
            triggers.forEach { (key, time) ->
                val id = "${occurrence.id}:$key"
                val existing = dao.delivery(id)
                if (existing == null) {
                    dao.putDelivery(ReminderDeliveryEntity(id, task.id, occurrence.id, originalAt = time.toEpochMilli(),
                        alarmAt = time.toEpochMilli(), constant = task.reminders.constant))
                } else if (existing.status == "suppressed") {
                    val future = time.toEpochMilli() > now
                    dao.putDelivery(existing.copy(status = if (future) "pending" else "dismissed", originalAt = time.toEpochMilli(),
                        alarmAt = time.toEpochMilli().takeIf { future }, revision = existing.revision + 1, dirty = true, alertAgain = false))
                } else if (existing.status == "pending" && existing.originalAt != time.toEpochMilli()) {
                    dao.putDelivery(existing.copy(originalAt = time.toEpochMilli(), alarmAt = time.toEpochMilli(),
                        revision = existing.revision + 1, dirty = true))
                } else if (previous != null && (previous.tasks.find { it.id == task.id } != task || target != null && beforeHabits.settings != habits.settings)) {
                    dao.putDelivery(existing.copy(dirty = true, alertAgain = false, revision = existing.revision + 1))
                }
            }
        }
        current.keys.filterNot { it in desiredIds }.forEach { taskId ->
            val target = habitReminderTarget(taskId)
            if (target != null && habits.habits.any { it.id == target.habitId } && target.date >= LocalDate.now(clock) && target.date <= LocalDate.now(clock).plusDays(7)) {
                // Preserve dismissal while temporarily achieved/archived; Undo restores only future reminders.
                dao.deliveries().filter { it.taskId == taskId && it.status in setOf("pending", "delivered") }.forEach {
                    dao.putDelivery(it.copy(status = "suppressed", alarmAt = null, revision = it.revision + 1, dirty = true, alertAgain = false))
                }
            } else { cancelTask(taskId); dao.deleteOccurrence(taskId) }
        }
    }

    private suspend fun cancelTask(taskId: String) {
        dao.deliveries().filter { it.taskId == taskId }.forEach { cancel(it) }
    }
    private suspend fun cancel(row: ReminderDeliveryEntity) {
        dao.putDelivery(row.copy(status = "cancelled", alarmAt = null, revision = row.revision + 1, dirty = true, alertAgain = false))
    }

    suspend fun state(): ReminderState {
        val preference = preferences()
        return ReminderState(AlertPause(preference.resumeDate?.let(LocalDate::parse)), if (paused()) emptyList() else
            dao.deliveries().filter { it.status == "delivered" }.map { ReminderAlert(it.id, it.taskId, it.occurrenceId, it.revision, it.missed) })
    }

    suspend fun reconcile(snapshot: TaskSnapshot, force: Boolean, habits: HabitSnapshot = HabitSnapshot()): ReminderBatch {
        sync(snapshot, habits = habits)
        // A notification that could not be posted before interruption/permission denial is catch-up,
        // not a fresh audible alert when access eventually returns.
        dao.deliveries().filter { it.status == "delivered" && it.dirty && it.alertAgain &&
            it.deliveredAt != null && now - it.deliveredAt > 120_000L }.forEach {
            dao.putDelivery(it.copy(missed = true, alarmAt = null, revision = it.revision + 1, alertAgain = false))
        }
        if (force) markAllDirty()
        val isPaused = paused()
        if (!isPaused) {
            // At most one catch-up delivery per task. Older offsets cannot form an alert storm.
            dao.deliveries().filter { it.alarmAt != null && it.alarmAt <= now && it.status in setOf("pending", "delivered") }
                .groupBy { it.taskId }.values.forEach { due ->
                    val newest = due.maxBy { it.originalAt }
                    due.filter { it.id != newest.id }.forEach { cancel(it) }
                    fire(newest.id, newest.revision)
                }
        }
        val rows = dao.deliveries()
        val tasks = snapshot.withHabitReminders(habits, clock).tasks.associateBy { it.id }
        return ReminderBatch(rows.filter { it.dirty }.map { row ->
            val task = tasks[row.taskId]
            val target = habitReminderTarget(row.taskId)
            val habit = target?.let { t -> habits.habits.find { it.id == t.habitId } }
            val valid = task != null && row.status in setOf("pending", "delivered")
            ReminderEffect(row.id, row.taskId, row.revision,
                row.alarmAt?.takeIf { valid && !isPaused }?.let(Instant::ofEpochMilli),
                valid && !isPaused && row.status == "delivered", row.missed, task?.title.orEmpty(),
                task?.dueDate?.toString().orEmpty() + (task?.dueTime?.let { " $it" } ?: ""), row.alertAgain, target, habits.settings.ringtoneUri, habit?.autoPopUp == true,
                habit?.let { habits.configurationOn(it, target!!.date).amount == null || it.recordMode == HabitRecordMode.Auto } ?: true, habits.settings.countInAppBadge)
        }, preferences().resumeDate?.let(LocalDate::parse)?.takeIf { it > LocalDate.now(clock) }
            ?.atStartOfDay(clock.zone)?.toInstant().let { pauseWake ->
                val dayWake = if (habits.habits.any { !it.archived && it.reminders.isNotEmpty() }) LocalDate.now(clock).plusDays(1).atStartOfDay(clock.zone).toInstant() else null
                listOfNotNull(pauseWake, dayWake).minOrNull()
            },
            if (isPaused) 0 else rows.count { it.status == "delivered" && it.missed && habitReminderTarget(it.taskId) == null },
            if (isPaused) 0 else rows.count { it.status == "delivered" && it.missed && habitReminderTarget(it.taskId) != null })
    }

    suspend fun fire(id: String, revision: Long) {
        val row = dao.delivery(id) ?: return
        if (row.revision != revision || paused() || row.status !in setOf("pending", "delivered") || row.alarmAt == null || row.alarmAt > now) return
        val siblings = dao.deliveries().filter { it.taskId == row.taskId && it.status == "delivered" && it.id != id }
        if (siblings.any { it.originalAt > row.originalAt }) { cancel(row); return }
        siblings.forEach { cancel(it) }
        val repeat = if (row.status == "delivered") row.repeatCount + 1 else 0
        val missed = row.missed || now - row.alarmAt > 120_000L
        // Fifteen-minute cadence, at most four follow-ups, and no repeating old catch-up alerts.
        val next = if (row.constant && !missed && repeat < 4 && now - row.originalAt < 86_400_000L) now + 900_000L else null
        dao.putDelivery(row.copy(status = "delivered", deliveredAt = now, alarmAt = next,
            revision = row.revision + 1, repeatCount = repeat, missed = missed, dirty = true, alertAgain = true))
    }

    suspend fun actionable(id: String, revision: Long): ReminderDeliveryEntity? = dao.delivery(id)?.takeIf {
        it.revision == revision && it.status == "delivered" && !paused()
    }

    suspend fun dismiss(row: ReminderDeliveryEntity) {
        dao.putDelivery(row.copy(status = "dismissed", alarmAt = null, revision = row.revision + 1, dirty = true, alertAgain = false))
    }

    suspend fun dismissHabitDay(owner: String) {
        dao.deliveries().filter { it.taskId == owner && it.status == "delivered" }.forEach { dismiss(it) }
    }

    suspend fun acknowledge(id: String, revision: Long) {
        dao.acknowledge(id, revision)
        dao.deleteCancelled(id, revision)
    }
}
