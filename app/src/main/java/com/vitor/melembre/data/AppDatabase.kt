package com.vitor.melembre.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Reminder::class, TaskList::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    abstract fun taskListDao(): TaskListDao

    companion object {
        private const val NAME = "me_lembre.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_lists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_task_lists_name` ON `task_lists` (`name`)",
                )
                seedDefaultTaskLists(db)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reminders_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `message` TEXT NOT NULL,
                        `scheduledAt` INTEGER,
                        `triggered` INTEGER NOT NULL,
                        `recurrence` TEXT NOT NULL,
                        `listId` INTEGER NOT NULL,
                        FOREIGN KEY(`listId`) REFERENCES `task_lists`(`id`)
                            ON UPDATE CASCADE ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `reminders_new` (`id`, `message`, `scheduledAt`, `triggered`, `recurrence`, `listId`)
                    SELECT `id`, `message`, `scheduledAt`, `triggered`, `recurrence`,
                           (SELECT `id` FROM `task_lists` WHERE `name` = '${TaskListNames.REMINDERS}' LIMIT 1)
                    FROM `reminders`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `reminders`")
                db.execSQL("ALTER TABLE `reminders_new` RENAME TO `reminders`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reminders_listId` ON `reminders` (`listId`)",
                )
            }
        }

        private val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedDefaultTaskLists(db)
            }
        }

        fun seedDefaultTaskLists(db: SupportSQLiteDatabase) {
            val base = System.currentTimeMillis()
            TaskListNames.defaults.forEachIndexed { index, name ->
                db.execSQL(
                    """
                    INSERT INTO task_lists (name, isDefault, createdAt)
                    SELECT ?, 1, ?
                    WHERE NOT EXISTS (
                        SELECT 1 FROM task_lists WHERE LOWER(name) = LOWER(?)
                    )
                    """.trimIndent(),
                    arrayOf(name, base + index, name),
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext, NAME).also { instance = it }
            }
        }

        fun build(context: Context, name: String, allowMainThread: Boolean = false): AppDatabase {
            val builder = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(seedCallback)
            if (allowMainThread) builder.allowMainThreadQueries()
            return builder.build()
        }

        fun inMemory(context: Context, allowMainThread: Boolean = false): AppDatabase {
            val builder = Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java)
                .addCallback(seedCallback)
            if (allowMainThread) builder.allowMainThreadQueries()
            return builder.build()
        }
    }
}
