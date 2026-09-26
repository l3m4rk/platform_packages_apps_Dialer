package com.android.dialer.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

/**
 * Collects one-shot [events] for as long as this composition is alive.
 *
 * [onEvent] is read through [rememberUpdatedState], so a recomposition that passes a new lambda does
 * not restart collection and drop an event in the gap.
 */
@Composable
internal fun <T> CollectEvents(
    events: Flow<T>,
    onEvent: suspend (T) -> Unit,
) {
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(events) {
        events.collect { event -> currentOnEvent(event) }
    }
}
