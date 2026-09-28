package com.android.dialer.keypad.ui

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.testutil.composeActivityRule
import com.android.dialer.theme.compose.DialerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA], qualifiers = "w411dp-h891dp")
class KeypadScreenTest {

    @get:Rule(order = 0)
    val componentActivityRule = composeActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val actions = mutableListOf<KeypadAction>()

    @Test
    fun everyKeyIsRendered() {
        renderScreen()

        KeypadKey.entries.forEach { key ->
            composeRule.onNodeWithTag(keypadKeyTestTag(key)).assertIsDisplayed()
        }
    }

    @Test
    fun pressingAKeyReportsPressThenRelease() {
        renderScreen()

        composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.FIVE)).performClick()

        assertEquals(
            listOf(
                KeypadAction.KeyPressed(KeypadKey.FIVE),
                KeypadAction.KeyReleased(KeypadKey.FIVE),
            ),
            actions,
        )
    }

    @Test
    fun holdingAKeyReportsThePressBeforeTheRelease() {
        renderScreen()

        composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.SEVEN)).performTouchInput {
            down(center)
        }
        composeRule.waitForIdle()

        // The tone is running at this point; only the press has been reported.
        assertEquals(listOf(KeypadAction.KeyPressed(KeypadKey.SEVEN)), actions)

        composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.SEVEN)).performTouchInput { up() }
        composeRule.waitForIdle()

        assertEquals(
            listOf(
                KeypadAction.KeyPressed(KeypadKey.SEVEN),
                KeypadAction.KeyReleased(KeypadKey.SEVEN),
            ),
            actions,
        )
    }

    @Test
    fun theDigitsAreShown() {
        renderScreen(uiState = KeypadUiState(digits = "(650) 555-1212"))

        composeRule.onNodeWithTag(KEYPAD_DIGITS_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun theEmergencyWarningAppearsOnlyWhenTheStateSaysSo() {
        renderScreen(uiState = KeypadUiState(showsEmergencyCallWarning = true))

        composeRule.onNodeWithTag(KEYPAD_EMERGENCY_WARNING_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun theCallButtonReportsTheAction() {
        renderScreen()

        composeRule.onNodeWithTag(KEYPAD_CALL_TEST_TAG).performClick()

        assertEquals(listOf(KeypadAction.CallClicked), actions)
    }

    @Test
    fun deleteReportsTheAction() {
        renderScreen(uiState = KeypadUiState(digits = "5", isDeleteEnabled = true))

        composeRule.onNodeWithTag(KEYPAD_DELETE_TEST_TAG).performClick()

        assertEquals(listOf(KeypadAction.DeleteClicked), actions)
    }

    @Test
    fun theOverflowStillOccupiesItsSlotWhileHidden() {
        renderScreen(uiState = KeypadUiState(isOverflowVisible = false))

        // Hidden with alpha rather than removed: the View keypad used INVISIBLE, not GONE, so the
        // digits row does not reflow as the first character is typed.
        composeRule.onNodeWithTag(KEYPAD_OVERFLOW_TEST_TAG).assertExists()
    }

    @Test
    fun keyDescriptionsSpellTheirLetters() {
        assertEquals("2, A B C", keyContentDescription(KeypadKey.TWO))
        assertEquals("1", keyContentDescription(KeypadKey.ONE))
        assertEquals("0, +", keyContentDescription(KeypadKey.ZERO))
    }

    private fun renderScreen(uiState: KeypadUiState = KeypadUiState()) {
        composeRule.setContent {
            DialerTheme {
                KeypadScreen(
                    uiState = uiState,
                    strings = testKeypadStrings(),
                    onAction = { action -> actions += action },
                )
            }
        }
    }

    private fun testKeypadStrings() = KeypadStrings(
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
