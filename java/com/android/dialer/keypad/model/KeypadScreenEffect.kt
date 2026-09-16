package com.android.dialer.keypad.model

/**
 * Something the keypad needs done that it cannot do itself.
 *
 * Placing a call and showing a dialog both need an `Activity`, which a `ViewModel` must never hold.
 * The view model decides, emits, and the keypad fragment carries it out.
 */
internal sealed interface KeypadScreenEffect {

    /** Long-pressed 1 with voicemail reachable. */
    data object CallVoicemail : KeypadScreenEffect

    /** Long-pressed 1 with no voicemail, because the radio is off. */
    data object ShowVoicemailAirplaneModeError : KeypadScreenEffect

    /** Long-pressed 1 with no voicemail for some other reason, such as a SIM still provisioning. */
    data object ShowVoicemailNotReadyError : KeypadScreenEffect
}
