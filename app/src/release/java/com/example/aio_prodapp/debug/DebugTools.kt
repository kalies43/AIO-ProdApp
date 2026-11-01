package com.example.aio_prodapp.debug

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * RELEASE stub. The real implementation lives in src/debug. Same API so main code compiles in
 * both build types, but nothing here can seed data, clear data, or shorten timers.
 */
object DebugTools {
    const val enabled = false
    val shortTimersFlow: StateFlow<Boolean> = MutableStateFlow(false)
    val shortTimers: Boolean get() = false
    fun init(context: Context) {}
}
