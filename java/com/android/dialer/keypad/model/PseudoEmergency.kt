package com.android.dialer.keypad.model

/** The IT Crowd easter egg: its number pulses the call button blue and red, with a buzz. */
internal object PseudoEmergency {

    const val NUMBER = "01189998819991197253"

    const val PULSE_MS = 200L

    /** The first leg, then six repeats, each with a buzz. */
    const val PULSES = 7

    const val FINAL_BUZZ_DELAY_MS = 1000L

    fun matches(digits: String): Boolean = digits.filter { char -> char in '0'..'9' } == NUMBER
}
