package com.android.dialer.keypad.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class KeypadUiState(
    val digits: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val isDeleteEnabled: Boolean = false,
    val isOverflowVisible: Boolean = false,
    val showsEmergencyCallWarning: Boolean = false,
    val isPseudoEmergencyNumber: Boolean = false,
    val isCallWithNoteAvailable: Boolean = false,
    val error: KeypadError? = null,
)
