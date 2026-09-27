package com.android.dialer.keypad.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PseudoEmergencyTest {

    @Test
    fun matchesTheNumber() {
        assertTrue(PseudoEmergency.matches("01189998819991197253"))
    }

    @Test
    fun ignoresFormatting() {
        assertTrue(PseudoEmergency.matches("0118 999 881 999 119 725-3"))
    }

    @Test
    fun ignoresNonDigitsTheHostAlsoDropped() {
        // The host's normalization kept only digits, so these never stopped the match either.
        assertTrue(PseudoEmergency.matches("0118999881999119725,3#"))
    }

    @Test
    fun doesNotMatchAPrefix() {
        assertFalse(PseudoEmergency.matches("0118999881999119725"))
    }

    @Test
    fun doesNotMatchWithAnExtraDigit() {
        assertFalse(PseudoEmergency.matches("011899988199911972531"))
    }

    @Test
    fun doesNotMatchAnEmptyField() {
        assertFalse(PseudoEmergency.matches(""))
    }
}
