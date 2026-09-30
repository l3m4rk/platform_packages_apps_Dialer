package com.android.dialer.keypad.ui

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
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

/** The legacy keypad's haptic ticks, recorded instead of felt. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA], qualifiers = "w411dp-h891dp")
class KeypadHapticsTest {

    @get:Rule(order = 0)
    val componentActivityRule = composeActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val feedback = mutableListOf<HapticFeedbackType>()
    private val recorder = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            feedback += hapticFeedbackType
        }
    }

    @Test
    fun aKeyTicksWhenPressed() {
        render()

        composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.FIVE)).performClick()

        assertEquals(listOf(HapticFeedbackType.VirtualKey), feedback)
    }

    @Test
    fun aKeyTicksWhenAScreenReaderActivatesIt() {
        render()

        composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.FIVE))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf(HapticFeedbackType.VirtualKey), feedback)
    }

    @Test
    fun aLongPressGetsTheLongPressFeedback() {
        render()

        composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.ZERO))
            .performTouchInput { longClick() }

        assertTrue("$feedback", HapticFeedbackType.LongPress in feedback)
    }

    @Test
    fun deleteTicks() {
        render(KeypadUiState(digits = "5", isDeleteEnabled = true, isOverflowVisible = true))

        composeRule.onNodeWithTag(KEYPAD_DELETE_TEST_TAG).performClick()

        assertEquals(listOf(HapticFeedbackType.VirtualKey), feedback)
    }

    @Test
    fun theCallButtonTicks() {
        render()

        composeRule.onNodeWithTag(KEYPAD_CALL_TEST_TAG).performClick()

        assertEquals(listOf(HapticFeedbackType.VirtualKey), feedback)
    }

    private fun render(uiState: KeypadUiState = KeypadUiState()) {
        composeRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides recorder) {
                DialerTheme {
                    KeypadScreen(uiState = uiState, strings = STRINGS, onAction = {})
                }
            }
        }
    }

    private companion object {
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
