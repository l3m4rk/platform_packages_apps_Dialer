package com.android.dialer.keypad.model

import android.media.ToneGenerator
import android.os.Build
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
class KeypadKeyTest {

    @Test
    fun keysAreDeclaredInCharMapOrder() {
        assertEquals(
            listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '#'),
            KeypadKey.entries.map { it.char },
        )
    }

    @Test
    fun lettersMatchTheLatinCharMap() {
        // Pins the ordinal-to-array correspondence KeypadKey.letters relies on.
        assertEquals("+", KeypadKey.ZERO.letters)
        assertEquals("", KeypadKey.ONE.letters)
        assertEquals("ABC", KeypadKey.TWO.letters)
        assertEquals("DEF", KeypadKey.THREE.letters)
        assertEquals("GHI", KeypadKey.FOUR.letters)
        assertEquals("JKL", KeypadKey.FIVE.letters)
        assertEquals("MNO", KeypadKey.SIX.letters)
        assertEquals("PQRS", KeypadKey.SEVEN.letters)
        assertEquals("TUV", KeypadKey.EIGHT.letters)
        assertEquals("WXYZ", KeypadKey.NINE.letters)
        assertEquals("", KeypadKey.STAR.letters)
        assertEquals("", KeypadKey.POUND.letters)
    }

    @Test
    fun digitKeysCarryMatchingKeyCodes() {
        assertEquals(KeyEvent.KEYCODE_0, KeypadKey.ZERO.keyCode)
        assertEquals(KeyEvent.KEYCODE_9, KeypadKey.NINE.keyCode)
        assertEquals(KeyEvent.KEYCODE_STAR, KeypadKey.STAR.keyCode)
        assertEquals(KeyEvent.KEYCODE_POUND, KeypadKey.POUND.keyCode)
    }

    @Test
    fun symbolKeysUseTheirOwnDtmfTones() {
        assertEquals(ToneGenerator.TONE_DTMF_0, KeypadKey.ZERO.tone)
        assertEquals(ToneGenerator.TONE_DTMF_9, KeypadKey.NINE.tone)
        assertEquals(ToneGenerator.TONE_DTMF_S, KeypadKey.STAR.tone)
        assertEquals(ToneGenerator.TONE_DTMF_P, KeypadKey.POUND.tone)
    }

    @Test
    fun everyKeyIsDistinct() {
        assertEquals(KeypadKey.entries.size, KeypadKey.entries.map { it.char }.toSet().size)
        assertEquals(KeypadKey.entries.size, KeypadKey.entries.map { it.tone }.toSet().size)
    }
}
