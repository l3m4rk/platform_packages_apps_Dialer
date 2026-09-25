package com.android.dialer.keypad.ui

import com.android.dialer.keypad.model.KeypadKey

internal const val KEYPAD_DIGITS_TEST_TAG = "keypad_digits"
internal const val KEYPAD_EMERGENCY_WARNING_TEST_TAG = "keypad_emergency_warning"
internal const val KEYPAD_DELETE_TEST_TAG = "keypad_delete"
internal const val KEYPAD_OVERFLOW_TEST_TAG = "keypad_overflow"
internal const val KEYPAD_OVERFLOW_PAUSE_TEST_TAG = "keypad_overflow_pause"
internal const val KEYPAD_OVERFLOW_WAIT_TEST_TAG = "keypad_overflow_wait"
internal const val KEYPAD_CALL_TEST_TAG = "keypad_call"

/** Stable per-key tag, so tests never have to match on a glyph or a localized letter row. */
internal fun keypadKeyTestTag(key: KeypadKey): String = "keypad_key_${key.name.lowercase()}"
