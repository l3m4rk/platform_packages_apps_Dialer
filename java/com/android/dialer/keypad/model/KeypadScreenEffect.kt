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

    /** Dial [number]. The host places the call and then dismisses the keypad. */
    data class PlaceCall(
        val number: String,
    ) : KeypadScreenEffect

    /**
     * The number matches `config_prohibited_phone_number_regexp`, a carrier or test-equipment rule
     * against dialing it by hand. The field has already been cleared.
     */
    data object ShowProhibitedNumberError : KeypadScreenEffect

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
