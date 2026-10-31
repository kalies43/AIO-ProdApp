package com.example.aio_prodapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aio_prodapp.data.ProfileRepository
import com.example.aio_prodapp.data.todayWeekIndex
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val displayName: String = "",
    val initials: String = "?",
    val notesCount: Int = 0,
    val sessionsCount: Int = 0,
    val tasksDoneCount: Int = 0,
    /** Index 0 = Sun ... 6 = Sat. */
    val weekCounts: List<Int> = List(7) { 0 },
    /** Which chart column is dark green. */
    val todayIndex: Int = 0,
    val streak: Int = 0
) {
    /** Flame icon only from a 3-day streak (spec Feature 4). */
    val showFlame: Boolean get() = streak >= 3
}

/** First letter of the first and last word, uppercase. "?" when empty, one letter for one word. */
fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when (parts.size) {
        0 -> "?"
        1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

class ProfileViewModel(private val repo: ProfileRepository) : ViewModel() {

    private val counts = combine(repo.noteCount, repo.sessionCount, repo.tasksDoneCount) { n, s, t ->
        Triple(n, s, t)
    }

    // flow { } re-runs on every new collection, so "this week" and "today" are recomputed
    // each time the Profile screen is shown again.
    private val progress = flow {
        emitAll(combine(repo.observeWeekCounts(), repo.observeStreak()) { week, streak -> week to streak })
    }

    val uiState: StateFlow<ProfileUiState> = combine(repo.displayName, counts, progress) { name, c, p ->
        ProfileUiState(
            displayName = name,
            initials = initialsOf(name),
            notesCount = c.first,
            sessionsCount = c.second,
            tasksDoneCount = c.third,
            weekCounts = p.first,
            todayIndex = todayWeekIndex(),
            streak = p.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun updateDisplayName(name: String, onResult: (Boolean) -> Unit = {}) =
        launchWrite(onResult) { repo.setDisplayName(name) }
}
