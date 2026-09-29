package com.android.dialer.keypad.model

/**
 * The number with its cursor, or its selection when [selectionStart] and [selectionEnd] differ,
 * as the digits field shows and edits it.
 */
internal data class DigitsValue(
    val text: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
)
