package com.example.aio_prodapp.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(
    private val profileDao: ProfileDao,
    noteDao: NoteDao,
    private val activityDao: ActivityDao
) {
    val displayName: Flow<String> = profileDao.observe().map { it?.displayName.orEmpty() }
    val noteCount: Flow<Int> = noteDao.observeCount()
    val sessionCount: Flow<Int> = activityDao.observeSessionCount()
    val tasksDoneCount: Flow<Int> = activityDao.observeTasksDoneCount()

    /** Upsert, so it also works if the row was wiped (e.g. by the debug "Clear all data"). */
    suspend fun setDisplayName(name: String) = profileDao.upsert(UserProfileEntity(id = 1, displayName = name.trim()))

    /**
     * Events per weekday, index 0 = Sunday ... 6 = Saturday, for the current week.
     * Call this inside a flow {} / on each collection so "this week" is recomputed.
     */
    fun observeWeekCounts(): Flow<List<Int>> =
        activityDao.observeWeekCounts(startOfWeekMs()).map { rows ->
            val counts = IntArray(7)
            rows.forEach { if (it.dow in 0..6) counts[it.dow] = it.n }
            counts.toList()
        }

    fun observeStreak(): Flow<Int> =
        activityDao.observeActiveDays().map { days ->
            val parsed = days.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            calculateStreak(parsed, LocalDate.now())
        }
}
