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

    /**
     * A character typed on a hardware keyboard, to go in at the cursor as a keypad key would, but
     * without a tone: the legacy field's key listener did not play one either.
     */
    data class CharacterTyped(
        val char: Char,
    ) : KeypadAction

    /**
     * The digits field changed the number or moved its cursor: a paste, a cut, a tap or a drag.
     * Typing never arrives this way; see [CharacterTyped].
     */
    data class DigitsEdited(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int,
    ) : KeypadAction

    /** The error dialog's OK, or a tap outside it. */
    data object ErrorDismissed : KeypadAction

    /** The overflow menu's "Call with a note". */
    data object CallWithNoteClicked : KeypadAction
}
