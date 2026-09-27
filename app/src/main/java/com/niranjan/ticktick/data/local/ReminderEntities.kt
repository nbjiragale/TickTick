package com.niranjan.ticktick.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity(tableName = "reminder_occurrences")
data class ReminderOccurrenceEntity(@PrimaryKey val taskId: String, val id: String, val fingerprint: String)

// Deliberately not cascade-deleted with tasks: cancellation work must survive task deletion.
@Entity(tableName = "reminder_deliveries", indices = [Index("taskId"), Index("alarmAt"), Index("dirty")])
data class ReminderDeliveryEntity(@PrimaryKey val id: String, val taskId: String, val occurrenceId: String,
    val revision: Long = 1, val originalAt: Long, val alarmAt: Long?, val status: String = "pending",
    val constant: Boolean, val repeatCount: Int = 0, val deliveredAt: Long? = null,
    val missed: Boolean = false, val dirty: Boolean = true, val alertAgain: Boolean = true)

@Entity(tableName = "reminder_preferences")
data class ReminderPreferencesEntity(@PrimaryKey val id: Int = 1, val resumeDate: String?, val legacyImported: Boolean = false)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_occurrences") suspend fun occurrences(): List<ReminderOccurrenceEntity>
    @Query("SELECT * FROM reminder_deliveries ORDER BY originalAt, id") suspend fun deliveries(): List<ReminderDeliveryEntity>
    @Query("SELECT * FROM reminder_deliveries WHERE id = :id") suspend fun delivery(id: String): ReminderDeliveryEntity?
    @Query("SELECT * FROM reminder_preferences WHERE id = 1") suspend fun preferences(): ReminderPreferencesEntity?
    @Upsert suspend fun putOccurrence(row: ReminderOccurrenceEntity)
    @Upsert suspend fun putDelivery(row: ReminderDeliveryEntity)
    @Upsert suspend fun putPreferences(row: ReminderPreferencesEntity)
    @Query("DELETE FROM reminder_occurrences WHERE taskId = :id") suspend fun deleteOccurrence(id: String)
    @Query("DELETE FROM reminder_deliveries WHERE id = :id AND revision = :revision AND status = 'cancelled' AND occurrenceId NOT IN (SELECT id FROM reminder_occurrences)")
    suspend fun deleteCancelled(id: String, revision: Long)
    @Query("UPDATE reminder_deliveries SET dirty = 0, alertAgain = 0 WHERE id = :id AND revision = :revision")
    suspend fun acknowledge(id: String, revision: Long)
}
