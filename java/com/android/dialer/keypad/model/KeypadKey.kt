package com.android.dialer.keypad.model

import android.media.ToneGenerator
import android.view.KeyEvent
import com.android.dialer.dialpadview.DialpadCharMappings

/**
 * Declaration order is load-bearing: it matches [DialpadCharMappings.getDefaultKeyToCharsMap],
 * where the index is the digit and 10 and 11 are star and pound.
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

    /** Always Latin; a second alphabet is added by the UI. */
    val letters: String
        get() = DialpadCharMappings.getDefaultKeyToCharsMap()[ordinal]
}
