package com.android.dialer.keypad.model

import androidx.compose.runtime.Immutable

/**
 * Everything the keypad renders.
 *
 * User-visible strings are deliberately absent: the composables resolve those from resources, so
 * this stays a plain data class that unit tests can assert on without an Android resource table.
 */
@Immutable
internal data class KeypadUiState(
    val digits: String = "",
    /** Where the digits field's cursor is, or its selection when the two differ. */
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val isDeleteEnabled: Boolean = false,
    /** The overflow only appears once there is something for its actions to act on. */
    val isOverflowVisible: Boolean = false,
    /** Shows the "can't make emergency calls over wifi" hint in place of the empty digits field. */
    val showsEmergencyCallWarning: Boolean = false,
    /** The field spells [PseudoEmergency.NUMBER], so the call button pulses. */
    val isPseudoEmergencyNumber: Boolean = false,
    /** Whether the overflow menu offers "Call with a note". */
    val isCallWithNoteAvailable: Boolean = false,
)
