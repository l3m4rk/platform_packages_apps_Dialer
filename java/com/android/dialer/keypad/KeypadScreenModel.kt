package com.android.dialer.keypad

import android.content.Intent
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

    /**
     * Empties the field on the host's behalf, as when it closes search. A host command rather than
     * a [KeypadAction], which only ever describes something the user did.
     */
    fun clearDigits()

    /**
     * Shows the number [intent] carries, replacing whatever was typed. Does nothing when it carries
     * none. The host decides which intents are new enough to apply.
     */
    fun fillFromDialIntent(intent: Intent)

    /**
     * Puts the number of the SIM contact that a special code looked up in front of whatever is in
     * the field, which the code has normally just emptied.
     */
    fun insertSimContactNumber(number: String)

    fun onAction(action: KeypadAction)
}
