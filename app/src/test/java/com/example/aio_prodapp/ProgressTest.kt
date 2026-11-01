package com.example.aio_prodapp

import com.example.aio_prodapp.data.calculateStreak
import com.example.aio_prodapp.viewmodel.initialsOf
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressTest {
    private val today = LocalDate.of(2026, 9, 10)

    @Test fun streakCountsBackFromToday() {
        val days = listOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, calculateStreak(days, today))
    }

    @Test fun streakCountsFromYesterdayWhenTodayHasNoActivity() {
        val days = listOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, calculateStreak(days, today))
    }

    @Test fun missingDayEndsTheStreak() {
        val days = listOf(today, today.minusDays(2), today.minusDays(3))
        assertEquals(1, calculateStreak(days, today))
    }

    @Test fun noActivityMeansZero() {
        assertEquals(0, calculateStreak(emptyList(), today))
        assertEquals(0, calculateStreak(listOf(today.minusDays(5)), today))
    }

    @Test fun initials() {
        assertEquals("SG", initialsOf("Shannah Grande"))
        assertEquals("SG", initialsOf("  shannah   grande  "))
        assertEquals("AL", initialsOf("Ada Byron Lovelace")) // first + last word
        assertEquals("M", initialsOf("Madonna"))
        assertEquals("?", initialsOf("   "))
    }
}
