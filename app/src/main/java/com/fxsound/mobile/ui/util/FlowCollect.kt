package com.fxsound.mobile.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun <T, R> StateFlow<T>.collectMappedAsState(transform: (T) -> R): State<R> {
    val mapped = remember(this) { map(transform).distinctUntilChanged() }
    return mapped.collectAsState(initial = transform(value))
}
