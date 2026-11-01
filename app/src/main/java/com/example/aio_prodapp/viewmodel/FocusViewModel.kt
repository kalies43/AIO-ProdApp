package com.example.aio_prodapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aio_prodapp.data.FocusRepository
import com.example.aio_prodapp.data.startOfTodayMs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Phase 1 only provides the data side. The timer itself (modes, absolute end time, Reset,
 * persistence across process death) is added in Phase 5.
 */
class FocusViewModel(private val repo: FocusRepository) : ViewModel() {

    /** "N sessions completed today". "Start of today" is recomputed every time the screen is shown. */
    val sessionsToday: StateFlow<Int> = flow { emitAll(repo.observeSessionsSince(startOfTodayMs())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Call when a FOCUS (not break) timer reaches 0. */
    fun recordFocusCompleted(
        currentTask: String?,
        occurredAt: Long = System.currentTimeMillis(),
        onResult: (Boolean) -> Unit = {}
    ) = launchWrite(onResult) { repo.recordFocusCompleted(currentTask, occurredAt) }
}
