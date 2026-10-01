package com.android.dialer.keypad

import android.content.Intent
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.keypad.model.KeypadUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** What the keypad's host asks of its view model; the screen itself only sees state and [onAction]. */
internal interface KeypadScreenModel {

    val uiState: StateFlow<KeypadUiState>

    val effects: Flow<KeypadScreenEffect>

    fun onHostStarted()

    /** Stops the tone: a dialog or split screen can pause the keypad with a finger still on a key. */
    fun onHostPaused()

    fun onHostStopped()

    fun clearDigits()

    fun fillFromDialIntent(intent: Intent)

    fun insertSimContactNumber(number: String)

    fun onAction(action: KeypadAction)
}
