package com.example.aio_prodapp.debug

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DEBUG implementation (src/debug only, so it is not compiled into release builds).
 *
 * Phase 5 (Focus) reads [shortTimers]: when true, Focus = 10 s, Short Break = 5 s, Long Break = 8 s.
 * The switch is stored in SharedPreferences so it survives process death, which the
 * "process death mid-session" test needs.
 */
object DebugTools {
    const val enabled = true

    private const val PREFS = "aio_debug"
    private const val KEY_SHORT_TIMERS = "short_timers"

    private var prefs: SharedPreferences? = null
    private val _shortTimers = MutableStateFlow(false)

    val shortTimersFlow: StateFlow<Boolean> = _shortTimers.asStateFlow()
    val shortTimers: Boolean get() = _shortTimers.value

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _shortTimers.value = p.getBoolean(KEY_SHORT_TIMERS, false)
    }

    fun setShortTimers(on: Boolean) {
        _shortTimers.value = on
        prefs?.edit()?.putBoolean(KEY_SHORT_TIMERS, on)?.apply()
    }
}
