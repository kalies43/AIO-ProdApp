package com.example.aio_prodapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aio_prodapp.data.NotificationEntity
import com.example.aio_prodapp.data.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class NotificationsViewModel(private val repo: NotificationRepository) : ViewModel() {

    /** Newest first. Grouping into Today / Yesterday / Few days ago is done in Phase 8. */
    val entries: StateFlow<List<NotificationEntity>> =
        repo.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Drives the small dot on the bell icon in the top bar. */
    val hasUnread: StateFlow<Boolean> =
        repo.hasUnread.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun markAllRead() = launchWrite({}) { repo.markAllRead() }

    fun clearAll(onResult: (Boolean) -> Unit = {}) = launchWrite(onResult) { repo.clearAll() }
}
