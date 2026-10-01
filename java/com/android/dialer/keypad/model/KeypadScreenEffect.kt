package com.android.dialer.keypad.model

/**
 * Something the keypad needs done that it cannot do itself.
 *
 * Placing a call needs an `Activity`, which a `ViewModel` must never hold. The view model decides,
 * emits, and the keypad fragment carries it out. The keypad's own error dialogs are state instead,
 * [KeypadUiState.error], drawn by the screen.
 */
internal sealed interface KeypadScreenEffect {

    /** Long-pressed 1 with voicemail reachable. */
    data object CallVoicemail : KeypadScreenEffect

    /** Dial [number]. The host places the call and then dismisses the keypad. */
    data class PlaceCall(
        val number: String,
    ) : KeypadScreenEffect

    /**
     * Open the call subject dialog for [number], which places the call itself, and dismiss the
     * keypad as for any placed call.
     */
    data class CallWithNote(
        val number: String,
    ) : KeypadScreenEffect

    /**
     * The user typed [input]; if it is a special code such as `*#06#`, an MMI code or a SIM
     * contact's position, act on it and then clear the field. Most codes need an `Activity`, to
     * show a dialog or start one, which is why the fragment rather than the view model runs them.
     */
    data class RunSpecialCode(
        val input: String,
    ) : KeypadScreenEffect
}
