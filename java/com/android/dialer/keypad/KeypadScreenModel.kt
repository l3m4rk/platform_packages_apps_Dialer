package com.android.dialer.keypad

import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.keypad.model.KeypadUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * What the keypad screen may ask of its view model.
 *
 * Separate from [KeypadViewModel] so that previews and Compose tests can drive the screen with a
 * fake, without Hilt or a real telephony stack behind it. A single [onAction] rather than a method
 * per interaction keeps the screen's callback surface to one stable lambda.
 */
internal interface KeypadScreenModel {

    val uiState: StateFlow<KeypadUiState>

    val effects: Flow<KeypadScreenEffect>

    /** Acquires the tone generator and refreshes state that can change while away. */
    fun onHostStarted()

    fun onHostStopped()

    fun onAction(action: KeypadAction)
}
