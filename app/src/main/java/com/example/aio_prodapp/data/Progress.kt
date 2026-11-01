package com.example.aio_prodapp.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

// "Today", "this week" and streak days use the device's LOCAL time zone (spec 3.4).
// Date-only reminder fields are different: they stay in UTC and never go through these helpers.

fun startOfTodayMs(zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

/** Start of this week's Sunday, local time. */
fun startOfWeekMs(zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zone)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        .atStartOfDay(zone).toInstant().toEpochMilli()

/** Sunday = 0 ... Saturday = 6, matching the chart columns. */
fun todayWeekIndex(zone: ZoneId = ZoneId.systemDefault()): Int =
    LocalDate.now(zone).dayOfWeek.value % 7

/**
 * Streak rule (spec 3.3): count consecutive days with progress, starting from today if today
 * has activity, otherwise from yesterday. A missing day ends the streak.
 */
fun calculateStreak(activeDays: Collection<LocalDate>, today: LocalDate): Int {
    val days = activeDays.toHashSet()
    var cursor = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (cursor in days) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
