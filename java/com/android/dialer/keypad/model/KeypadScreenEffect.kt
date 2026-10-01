package com.android.dialer.keypad.model

/** What needs an `Activity`, which the view model must never hold, so the fragment carries it out. */
internal sealed interface KeypadScreenEffect {

    data object CallVoicemail : KeypadScreenEffect

    data class PlaceCall(
        val number: String,
    ) : KeypadScreenEffect

    data class CallWithNote(
        val number: String,
    ) : KeypadScreenEffect

    /** Typed input that may be a special code such as `*#06#`. */
    data class RunSpecialCode(
        val input: String,
    ) : KeypadScreenEffect
}
