package com.android.dialer.keypad

import android.content.Intent
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.keypad.model.KeypadUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * What the keypad's host may ask of its view model, as Messaging's screens declare theirs.
 *
 * The screen itself never sees this: it takes [KeypadUiState] and an [onAction] lambda, so tests
 * and previews render it from plain state. A single [onAction] rather than a method per
 * interaction keeps that callback surface to one stable lambda.
 */
internal interface KeypadScreenModel {

    val uiState: StateFlow<KeypadUiState>

    val effects: Flow<KeypadScreenEffect>

    /** Acquires the tone generator and refreshes state that can change while away. */
    fun onHostStarted()

    /**
     * Stops any tone and forgets held keys, as DialpadFragment did in onPause: a dialog or split
     * screen can pause the keypad without stopping it, with a finger still on a key.
     */
    fun onHostPaused()

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
