package com.example.aio_prodapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Runs a database write and reports success to the UI, so a screen can keep its dialog open and
 * show "Couldn't save. Try again." when it fails (spec Feature 1 / 2 edge cases).
 * [onResult] is called on the main thread.
 */
internal fun ViewModel.launchWrite(onResult: (Boolean) -> Unit, block: suspend () -> Unit) {
    viewModelScope.launch {
        val ok = try {
            block()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
        onResult(ok)
    }
}

/** Same as [launchWrite] but for writes that produce an id. Result is null on failure. */
internal fun ViewModel.launchWriteForId(onResult: (Int?) -> Unit, block: suspend () -> Int) {
    viewModelScope.launch {
        val id = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        onResult(id)
    }
}
