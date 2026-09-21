package com.android.dialer.keypad.model

/** Everything the keypad screen can report a user doing. */
internal sealed interface KeypadAction {

    /**
     * A key went down. Its tone starts and its character is typed; the tone runs until
     * [KeyReleased], so that holding a key holds the tone as a desk phone does.
     */
    data class KeyPressed(
        val key: KeypadKey,
    ) : KeypadAction

    data class KeyReleased(
        val key: KeypadKey,
    ) : KeypadAction

    /** Long-pressed 1, which calls voicemail when the field holds nothing but that press. */
    data object VoicemailKeyLongPressed : KeypadAction

    /** Long-pressed 0, which replaces the typed zero with a `+`. */
    data object PlusKeyLongPressed : KeypadAction

    data object DeleteClicked : KeypadAction

    /** Long-pressed backspace, which clears the whole field rather than one character. */
    data object DeleteLongPressed : KeypadAction

    data object PauseClicked : KeypadAction

    data object WaitClicked : KeypadAction

    /** The call button. Places a call, or recalls the last dialed number into an empty field. */
    data object CallClicked : KeypadAction
}
