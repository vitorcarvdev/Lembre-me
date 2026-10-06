package com.vitor.melembre.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_lists",
    indices = [Index(value = ["name"], unique = true)],
)
data class TaskList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
)
