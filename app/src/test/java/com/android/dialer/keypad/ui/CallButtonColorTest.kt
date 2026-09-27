package com.android.dialer.keypad.ui

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.android.dialer.keypad.model.PseudoEmergency
import com.android.dialer.testutil.composeActivityRule
import com.android.dialer.theme.compose.DialerColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
class CallButtonColorTest {

    @get:Rule(order = 0)
    val componentActivityRule = composeActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private var isPseudoEmergencyNumber by mutableStateOf(false)
    private var color: Color? = null

    @Test
    fun isTheUsualGreenForAnyOtherNumber() {
        render()

        assertEquals(DialerColors.CallContainer, color)
    }

    @Test
    fun pulsesBetweenBlueAndRed() {
        render()
        startPulse()
        assertTrue("starts blue, was $color", color!!.isMostlyBlue())

        // Around the end of the first leg, and of the second.
        advanceFramesBy(PseudoEmergency.PULSE_MS)
        assertTrue("red after one leg, was $color", color!!.isMostlyRed())
        advanceFramesBy(PseudoEmergency.PULSE_MS)
        assertTrue("blue after two, was $color", color!!.isMostlyBlue())
    }

    @Test
    fun blendsThroughTheMiddleOfALeg() {
        render()
        startPulse()

        advanceFramesBy(PseudoEmergency.PULSE_MS / 2)

        val blend = color!!
        assertTrue("both blue and red in $blend", blend.red > MIXED && blend.blue > MIXED)
        assertTrue("no green in $blend", blend.green < MIXED)
    }

    @Test
    fun returnsToGreenOnceThePulseEndsWhileTheNumberStays() {
        render()
        startPulse()
        advanceFramesBy(PseudoEmergency.PULSE_MS * PseudoEmergency.PULSES + 100)

        assertEquals(DialerColors.CallContainer, color)
    }

    @Test
    fun returnsToGreenAtOnceWhenTheNumberIsEdited() {
        render()
        startPulse()
        advanceFramesBy(PseudoEmergency.PULSE_MS / 2)

        setFlag(false)
        advanceFramesBy(FRAME_MS * 2)

        assertEquals(DialerColors.CallContainer, color)
    }

    /** Flips the flag and lets the pulse start. */
    private fun startPulse() {
        composeRule.mainClock.autoAdvance = false
        setFlag(true)
        // One frame starts the effect, the next composes its first color.
        advanceFramesBy(FRAME_MS * 2)
    }

    // Frame by frame: one long jump would hand each leg a single frame, and leave legs unplayed.
    private fun advanceFramesBy(millis: Long) {
        repeat(((millis + FRAME_MS - 1) / FRAME_MS).toInt()) {
            composeRule.mainClock.advanceTimeByFrame()
        }
    }

    // With the clock paused nothing else sends the write to the composition, which would otherwise
    // keep composing the old value.
    private fun setFlag(value: Boolean) {
        isPseudoEmergencyNumber = value
        Snapshot.sendApplyNotifications()
    }

    private fun Color.isMostlyBlue() = blue > DOMINANT && red < 1 - DOMINANT && green == 0f

    private fun Color.isMostlyRed() = red > DOMINANT && blue < 1 - DOMINANT && green == 0f

    private fun render() {
        composeRule.setContent {
            color = callButtonContainerColor(isPseudoEmergencyNumber)
        }
    }

    private companion object {
        private const val FRAME_MS = 16L

        // A frame either side of a leg's end is still well past the blend's midpoint.
        private const val DOMINANT = 0.8f
        private const val MIXED = 0.1f
    }
}
