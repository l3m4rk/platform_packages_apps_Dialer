package com.android.dialer.keypad.model

import android.media.ToneGenerator
import android.view.KeyEvent
import com.android.dialer.dialpadview.DialpadCharMappings

/**
 * The twelve keys of the dialpad.
 *
 * Declaration order is load-bearing: it matches the array order of
 * [DialpadCharMappings.getDefaultKeyToCharsMap], where the index is the digit itself and indices
 * 10 and 11 are star and pound. `KeypadKeyTest` pins that correspondence.
 *
 * [tone] is the DTMF tone the key plays for as long as it is held, and [keyCode] is the key event
 * the View dialpad synthesised into its `EditText`. Both are kept so the Compose keypad produces
 * byte-identical dialling behaviour to the fragment it replaces.
 */
internal enum class KeypadKey(
    val char: Char,
    val keyCode: Int,
    val tone: Int,
) {
    ZERO(char = '0', keyCode = KeyEvent.KEYCODE_0, tone = ToneGenerator.TONE_DTMF_0),
    ONE(char = '1', keyCode = KeyEvent.KEYCODE_1, tone = ToneGenerator.TONE_DTMF_1),
    TWO(char = '2', keyCode = KeyEvent.KEYCODE_2, tone = ToneGenerator.TONE_DTMF_2),
    THREE(char = '3', keyCode = KeyEvent.KEYCODE_3, tone = ToneGenerator.TONE_DTMF_3),
    FOUR(char = '4', keyCode = KeyEvent.KEYCODE_4, tone = ToneGenerator.TONE_DTMF_4),
    FIVE(char = '5', keyCode = KeyEvent.KEYCODE_5, tone = ToneGenerator.TONE_DTMF_5),
    SIX(char = '6', keyCode = KeyEvent.KEYCODE_6, tone = ToneGenerator.TONE_DTMF_6),
    SEVEN(char = '7', keyCode = KeyEvent.KEYCODE_7, tone = ToneGenerator.TONE_DTMF_7),
    EIGHT(char = '8', keyCode = KeyEvent.KEYCODE_8, tone = ToneGenerator.TONE_DTMF_8),
    NINE(char = '9', keyCode = KeyEvent.KEYCODE_9, tone = ToneGenerator.TONE_DTMF_9),
    STAR(char = '*', keyCode = KeyEvent.KEYCODE_STAR, tone = ToneGenerator.TONE_DTMF_S),
    POUND(char = '#', keyCode = KeyEvent.KEYCODE_POUND, tone = ToneGenerator.TONE_DTMF_P),
    ;

    /**
     * The Latin letters printed under the key's glyph, or an empty string for 1, star and pound.
     *
     * Always the Latin mapping, matching `DialpadView.setupKeypad`, which reads the primary letters
     * from `getDefaultKeyToCharsMap()` and never localises them. Key 0's letters are `"+"`.
     */
    val letters: String
        get() = DialpadCharMappings.getDefaultKeyToCharsMap()[ordinal]
}
