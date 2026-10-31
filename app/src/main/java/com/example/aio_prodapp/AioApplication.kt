package com.example.aio_prodapp

import android.app.Application
import android.content.Context
import com.example.aio_prodapp.debug.DebugTools
import com.example.aio_prodapp.data.AppDatabase
import com.example.aio_prodapp.data.FocusRepository
import com.example.aio_prodapp.data.NoteRepository
import com.example.aio_prodapp.data.NotificationRepository
import com.example.aio_prodapp.data.ProfileRepository
import com.example.aio_prodapp.data.TaskRepository

/** Registered in AndroidManifest.xml as android:name=".AioApplication". */
class AioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugTools.init(this) // real in debug builds, empty stub in release
    }

    val container: AppContainer by lazy { AppContainer(this) }
}

/**
 * Hand-rolled dependency container (no Hilt: spec section 2 forbids extra libraries).
 * Building the database does not touch disk; Room opens it on the first query, off the main thread.
 */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)

    val noteRepository = NoteRepository(database.noteDao())
    val taskRepository = TaskRepository(database)
    val focusRepository = FocusRepository(database)
    val profileRepository = ProfileRepository(database.profileDao(), database.noteDao(), database.activityDao())
    val notificationRepository = NotificationRepository(database.notificationDao())
}
