package com.android.dialer.keypad.ui

import android.os.Build
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.testutil.composeActivityRule
import com.android.dialer.theme.compose.DialerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA], qualifiers = "w411dp-h891dp")
class KeyEntranceTest {

    @get:Rule(order = 0)
    val componentActivityRule = composeActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val entranceState = KeypadEntranceState()

    @Test
    fun keysStartBelowTheirPlace() {
        render()
        val resting = topOf(KeypadKey.ONE)

        play()

        assertDp(resting + KEY_ENTRANCE_DISTANCE, topOf(KeypadKey.ONE))
    }

    @Test
    fun keysSettleBackInPlace() {
        render()
        val resting = KeypadKey.entries.associateWith(::topOf)

        play()
        advanceFramesBy(ONE_SECOND_MS)

        KeypadKey.entries.forEach { key -> assertDp(resting.getValue(key), topOf(key)) }
    }

    @Test
    fun theFirstKeyMovesBeforeTheLastStarts() {
        render()
        val oneResting = topOf(KeypadKey.ONE)
        val poundResting = topOf(KeypadKey.POUND)

        play()
        // Past 1's delay and well into its move; still inside #'s delay.
        advanceFramesBy(150)

        assertTrue("1 has moved", topOf(KeypadKey.ONE) < oneResting + KEY_ENTRANCE_DISTANCE)
        assertDp(poundResting + KEY_ENTRANCE_DISTANCE, topOf(KeypadKey.POUND))
    }

    @Test
    fun nothingMovesUntilPlayed() {
        render()
        val resting = topOf(KeypadKey.FIVE)

        advanceFramesBy(ONE_SECOND_MS)

        assertDp(resting, topOf(KeypadKey.FIVE))
    }

    @Test
    fun aCompositionCreatedAfterAPlayDoesNotReplayIt() {
        // The fragment keeps its entrance state across a new view, as after rotation.
        entranceState.play()
        // Paused from the start, so a replayed entrance could not finish unseen while composing.
        composeRule.mainClock.autoAdvance = false
        render()
        advanceFramesBy(FRAME_MS * 2)
        val early = topOf(KeypadKey.FIVE)

        advanceFramesBy(ONE_SECOND_MS)

        assertDp(topOf(KeypadKey.FIVE), early)
    }

    @Test
    @Config(qualifiers = "ar-ldrtl-w891dp-h411dp-land")
    fun inARightToLeftLandscapeTheKeysComeFromTheLeft() {
        // The whole screen, which pins its keys left-to-right: the entrance must still follow the
        // language, not the pin.
        composeRule.setContent {
            DialerTheme {
                KeypadScreen(
                    uiState = KeypadUiState(),
                    strings = STRINGS,
                    onAction = {},
                    entranceState = entranceState,
                )
            }
        }
        composeRule.mainClock.autoAdvance = false
        val resting = leftOf(KeypadKey.THREE)

        play()

        assertDp(resting - KEY_ENTRANCE_DISTANCE, leftOf(KeypadKey.THREE))
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun inALeftToRightLandscapeTheKeysComeFromTheRight() {
        composeRule.setContent {
            DialerTheme {
                KeypadScreen(
                    uiState = KeypadUiState(),
                    strings = STRINGS,
                    onAction = {},
                    entranceState = entranceState,
                )
            }
        }
        composeRule.mainClock.autoAdvance = false
        val resting = leftOf(KeypadKey.ONE)

        play()

        assertDp(resting + KEY_ENTRANCE_DISTANCE, leftOf(KeypadKey.ONE))
    }

    private fun render() {
        composeRule.setContent {
            DialerTheme {
                KeypadGrid(strings = STRINGS, onAction = {}, entranceState = entranceState)
            }
        }
        composeRule.mainClock.autoAdvance = false
    }

    /** Requests the entrance and lets its first displaced frame draw. */
    private fun play() {
        entranceState.play()
        Snapshot.sendApplyNotifications()
        advanceFramesBy(FRAME_MS * 2)
    }

    private fun leftOf(key: KeypadKey): Dp =
        composeRule.onNodeWithTag(keypadKeyTestTag(key)).getBoundsInRoot().left

    private fun topOf(key: KeypadKey): Dp =
        composeRule.onNodeWithTag(keypadKeyTestTag(key)).getBoundsInRoot().top

    private fun advanceFramesBy(millis: Long) {
        repeat(((millis + FRAME_MS - 1) / FRAME_MS).toInt()) {
            composeRule.mainClock.advanceTimeByFrame()
        }
    }

    private fun assertDp(expected: Dp, actual: Dp) {
        assertEquals(expected.value, actual.value, TOLERANCE.value)
    }

    private companion object {
        private const val FRAME_MS = 16L
        private const val ONE_SECOND_MS = 1_000L
        private val TOLERANCE = 0.5.dp

        private val STRINGS = KeypadStrings(
            voicemailKeyAction = "call voicemail",
            plusKeyAction = "dial plus",
            deleteButton = "backspace",
            overflowButton = "More options",
            call = "Call",
            emergencyCallWarning = "no emergency calls over wifi",
            addPause = "Add 2-sec pause",
            addWait = "Add wait",
            callWithNote = "Call with a note",
        )
    }
}
