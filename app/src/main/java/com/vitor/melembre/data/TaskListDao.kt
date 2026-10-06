package com.vitor.melembre.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query(
        """
        SELECT * FROM task_lists
        ORDER BY isDefault DESC, createdAt ASC, id ASC
        """,
    )
    fun observeAll(): Flow<List<TaskList>>

    @Query(
        """
        SELECT * FROM task_lists
        ORDER BY isDefault DESC, createdAt ASC, id ASC
        """,
    )
    suspend fun getAll(): List<TaskList>

    @Query("SELECT * FROM task_lists WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskList?

    @Query("SELECT * FROM task_lists WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TaskList?

    @Query("SELECT * FROM task_lists WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByNameInsensitive(name: String): TaskList?

    @Query("SELECT COUNT(*) FROM reminders WHERE listId = :listId")
    suspend fun countReminders(listId: Long): Int

    @Insert
    suspend fun insert(list: TaskList): Long

    @Update
    suspend fun update(list: TaskList)

    @Query("UPDATE reminders SET listId = :targetId WHERE listId = :sourceId")
    suspend fun moveReminders(sourceId: Long, targetId: Long)

    @Query("DELETE FROM task_lists WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun deleteMovingReminders(sourceId: Long, targetId: Long) {
        moveReminders(sourceId, targetId)
        deleteById(sourceId)
    }
}
