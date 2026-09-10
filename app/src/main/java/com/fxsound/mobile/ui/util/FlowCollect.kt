package com.fxsound.mobile.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Collect a transformed slice of a [StateFlow] as Compose state.
 *
 * The transform is wrapped in [rememberUpdatedState] so the cached mapped flow
 * always uses the freshest lambda, while the cache key itself stays stable
 * (just the receiver [StateFlow]). This avoids recreating the mapped flow on
 * every recomposition when the caller passes a fresh lambda each time.
 */
@Composable
fun <T, R> StateFlow<T>.collectMappedAsState(transform: (T) -> R): State<R> {
    val transformState = rememberUpdatedState(transform)
    val mapped: Flow<R> = remember(this) {
        map { value -> transformState.value(value) }.distinctUntilChanged()
    }
    return mapped.collectAsState(initial = transformState.value(value))
}

/**
 * Convenience overload: collect the [StateFlow] directly when no transform is needed.
 */
@Composable
fun <T> StateFlow<T>.collectMappedAsState(): State<T> = collectAsState()
