package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY scheduledTime ASC")
    fun getActiveRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY id DESC")
    fun getAllRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersList(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Query("SELECT * FROM reminders WHERE parentId = :parentId")
    suspend fun getChildrenReminders(parentId: Int): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)

    // Logs Queries
    @Query("SELECT * FROM confirmation_logs ORDER BY actionedAt DESC")
    fun getAllLogsFlow(): Flow<List<ConfirmationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfirmationLog(log: ConfirmationLog)

    @Query("DELETE FROM confirmation_logs")
    suspend fun clearAllLogs()
}
