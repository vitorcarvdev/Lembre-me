package com.vitor.melembre.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query(
        """
        SELECT * FROM reminders
        WHERE triggered = 0 AND scheduledAt > :now
        ORDER BY scheduledAt ASC
        """,
    )
    fun observeUpcoming(now: Long = System.currentTimeMillis()): Flow<List<Reminder>>

    @Query(
        """
        SELECT * FROM reminders
        ORDER BY
          CASE
            WHEN triggered = 0 AND scheduledAt > :now THEN 0
            ELSE 1
          END ASC,
          scheduledAt ASC
        """,
    )
    fun observeHomeList(now: Long = System.currentTimeMillis()): Flow<List<Reminder>>

    @Query(
        """
        SELECT * FROM reminders
        WHERE triggered = 0 AND scheduledAt > :now
        ORDER BY scheduledAt ASC
        """,
    )
    suspend fun getUpcoming(now: Long = System.currentTimeMillis()): List<Reminder>

    @Query(
        """
        SELECT * FROM reminders
        WHERE triggered = 0
        ORDER BY scheduledAt ASC
        """,
    )
    suspend fun getActiveForReschedule(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("UPDATE reminders SET triggered = 1 WHERE id = :id")
    suspend fun markTriggered(id: Long)
}
