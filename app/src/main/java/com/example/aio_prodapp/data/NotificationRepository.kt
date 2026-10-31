package com.example.aio_prodapp.data

import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {
    val entries: Flow<List<NotificationEntity>> = dao.observeAll()
    val hasUnread: Flow<Boolean> = dao.observeHasUnread()

    /** Writes a history row and trims to the newest [MAX_ENTRIES]. */
    suspend fun add(
        type: NotificationType,
        title: String,
        body: String = "",
        noteId: Int? = null,
        taskId: Int? = null,
        createdAt: Long = System.currentTimeMillis()
    ) {
        dao.insertAndTrim(
            NotificationEntity(
                type = type,
                title = title,
                body = body,
                noteId = noteId,
                taskId = taskId,
                createdAt = createdAt
            ),
            MAX_ENTRIES
        )
    }

    suspend fun markAllRead() = dao.markAllRead()
    suspend fun clearAll() = dao.deleteAll()

    companion object {
        const val MAX_ENTRIES = 100
    }
}
