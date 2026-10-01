package com.android.dialer.keypad.model

internal sealed interface KeypadAction {

    data class KeyPressed(
        val key: KeypadKey,
    ) : KeypadAction

    data class KeyReleased(
        val key: KeypadKey,
    ) : KeypadAction

    data object VoicemailKeyLongPressed : KeypadAction

    data object PlusKeyLongPressed : KeypadAction

    data object DeleteClicked : KeypadAction

    data object DeleteLongPressed : KeypadAction

    data object PauseClicked : KeypadAction

    data object WaitClicked : KeypadAction

    data object CallClicked : KeypadAction

    /** A hardware key, typed at the cursor without a tone. */
    data class CharacterTyped(
        val char: Char,
    ) : KeypadAction

    /** A paste, cut or cursor move in the field; typing arrives as [CharacterTyped]. */
    data class DigitsEdited(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int,
    ) : KeypadAction

    data object ErrorDismissed : KeypadAction

    data object CallWithNoteClicked : KeypadAction
}
