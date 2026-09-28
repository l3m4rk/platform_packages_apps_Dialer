package com.android.dialer.keypad.ui

import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.ui.KeyEntranceLayout.LANDSCAPE_LTR
import com.android.dialer.keypad.ui.KeyEntranceLayout.LANDSCAPE_RTL
import com.android.dialer.keypad.ui.KeyEntranceLayout.PORTRAIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyEntranceTimingTest {

    @Test
    fun everyKeyHasATimingInEveryLayout() {
        KeyEntranceLayout.entries.forEach { layout ->
            KeypadKey.entries.forEach { key ->
                val timing = keyEntranceTiming(key, layout)
                assertTrue("$key in $layout: $timing", timing.durationMs > 0)
            }
        }
    }

    @Test
    fun scalesTheLegacyFramesAndTruncatesTheSameWay() {
        // 1 frame * 33 ms * 0.66 = 21.78, and 10 frames * 33 ms * 0.8 = 264.
        assertEquals(KeyEntranceTiming(delayMs = 21, durationMs = 264), timing(KeypadKey.ONE))
        // 11 * 33 * 0.66 = 239.58, and 8 * 33 * 0.8 = 211.2.
        assertEquals(KeyEntranceTiming(delayMs = 239, durationMs = 211), timing(KeypadKey.POUND))
    }

    @Test
    fun portraitGoesRowByRowAndEndsWithZeroAndPoundTogether() {
        val order = listOf(
            KeypadKey.ONE, KeypadKey.TWO, KeypadKey.THREE,
            KeypadKey.FOUR, KeypadKey.FIVE, KeypadKey.SIX,
            KeypadKey.SEVEN, KeypadKey.EIGHT, KeypadKey.NINE,
            KeypadKey.STAR, KeypadKey.ZERO,
        )
        assertStrictlyLater(order, PORTRAIT)
        assertEquals(timing(KeypadKey.ZERO).delayMs, timing(KeypadKey.POUND).delayMs)
    }

    @Test
    fun portraitLowerRowsMoveFaster() {
        assertTrue(timing(KeypadKey.FOUR).durationMs > timing(KeypadKey.SEVEN).durationMs)
        assertTrue(timing(KeypadKey.SEVEN).durationMs > timing(KeypadKey.ZERO).durationMs)
    }

    @Test
    fun landscapeLeftToRightStartsFromTheLeftColumn() {
        assertStrictlyLater(
            listOf(KeypadKey.ONE, KeypadKey.FOUR, KeypadKey.SEVEN, KeypadKey.STAR, KeypadKey.TWO),
            LANDSCAPE_LTR,
        )
        assertStrictlyLater(listOf(KeypadKey.ZERO, KeypadKey.THREE), LANDSCAPE_LTR)
    }

    @Test
    fun landscapeRightToLeftStartsFromTheRightColumn() {
        assertStrictlyLater(
            listOf(KeypadKey.THREE, KeypadKey.SIX, KeypadKey.NINE, KeypadKey.POUND, KeypadKey.TWO),
            LANDSCAPE_RTL,
        )
        assertStrictlyLater(listOf(KeypadKey.ZERO, KeypadKey.ONE), LANDSCAPE_RTL)
    }

    @Test
    fun landscapeDirectionsMirrorEachOthersDurations() {
        assertEquals(
            timing(KeypadKey.ONE, LANDSCAPE_LTR).durationMs,
            timing(KeypadKey.THREE, LANDSCAPE_RTL).durationMs,
        )
        assertEquals(
            timing(KeypadKey.THREE, LANDSCAPE_LTR).durationMs,
            timing(KeypadKey.ONE, LANDSCAPE_RTL).durationMs,
        )
    }

    @Test
    fun theWholeEntranceIsOverInUnderHalfASecond() {
        KeyEntranceLayout.entries.forEach { layout ->
            val longest = KeypadKey.entries.maxOf { key ->
                keyEntranceTiming(key, layout).let { it.delayMs + it.durationMs }
            }
            assertTrue("$layout ends at $longest ms", longest < 500)
        }
    }

    private fun timing(key: KeypadKey, layout: KeyEntranceLayout = PORTRAIT) =
        keyEntranceTiming(key, layout)

    private fun assertStrictlyLater(keys: List<KeypadKey>, layout: KeyEntranceLayout) {
        keys.zipWithNext().forEach { (earlier, later) ->
            assertTrue(
                "$later should start after $earlier in $layout",
                timing(later, layout).delayMs > timing(earlier, layout).delayMs,
            )
        }
    }
}
