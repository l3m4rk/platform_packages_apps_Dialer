package com.android.dialer.keypad

import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.keypad.model.KeypadUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * What the keypad screen may ask of its view model.
 *
 * Separate from [KeypadViewModel] so that previews and Compose tests can drive the screen with a
 * fake, without Hilt or a real telephony stack behind it.
 */
internal interface KeypadScreenModel {

    val uiState: StateFlow<KeypadUiState>

    val effects: Flow<KeypadScreenEffect>

    /** Acquires the tone generator. Paired with [onHostStopped]. */
    fun onHostStarted()

    fun onHostStopped()

    /**
     * A key went down: its tone starts and its character is typed.
     *
     * Must be paired with [onKeyReleased], which is what stops the tone — a key press starts a tone
     * of unbounded length so that holding a key holds the tone, exactly as a desk phone does.
     */
    fun onKeyPressed(key: KeypadKey)

    fun onKeyReleased(key: KeypadKey)

    /** Long-pressed 1, which calls voicemail when the field holds nothing but that press. */
    fun onVoicemailKeyLongPressed()

    /** Long-pressed 0, which replaces the typed zero with a `+`. */
    fun onPlusKeyLongPressed()

    fun onDeleteClicked()

    /** Long-pressed backspace, which clears the whole field rather than one character. */
    fun onDeleteLongPressed()

    fun onPauseClicked()

    fun onWaitClicked()
}
