package com.android.dialer.keypad.model

/**
 * The pseudo-emergency Easter egg: typing the IT Crowd's "new emergency number" pulses the call
 * button between blue and red and buzzes along with it.
 *
 * Port of `PseudoEmergencyAnimator`, whose timings these are. The view model runs the buzzes and
 * the call button the colors, both from [KeypadUiState.isPseudoEmergencyNumber].
 */
internal object PseudoEmergency {

    const val NUMBER = "01189998819991197253"

    /** One leg of the pulse, blue to red or back; also how long each buzz lasts. */
    const val PULSE_MS = 200L

    /** Legs in all: the first, then six repeats, ending on red. A buzz marks each repeat. */
    const val PULSES = 7

    /** The last buzz comes this long after the pulse ends, or is cut short. */
    const val FINAL_BUZZ_DELAY_MS = 1000L

    /**
     * Whether [digits] spell the number, ignoring formatting. The host normalized the query to its
     * digits before `DialpadFragment` compared it; this does the same.
     */
    fun matches(digits: String): Boolean = digits.filter { char -> char in '0'..'9' } == NUMBER
}
