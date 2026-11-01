package com.example.aio_prodapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        ActivityLogEntity::class,
        NotificationEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun activityDao(): ActivityDao
    abstract fun notificationDao(): NotificationDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val DB_NAME = "aio_prodapp.db"

        /**
         * Migration path for later versions. When you bump `version`, add a Migration here
         * (never use fallbackToDestructiveMigration; users would lose their notes and tasks).
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        // Room enables SQLite foreign key enforcement by default; cascade / set-null rely on it.
        private fun build(appContext: Context): AppDatabase =
            Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Ensure the single user_profile row exists.
                        db.execSQL("INSERT OR IGNORE INTO user_profile (id, display_name) VALUES (1, '')")
                    }
                })
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
