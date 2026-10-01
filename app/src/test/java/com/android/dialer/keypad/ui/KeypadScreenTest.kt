package com.android.dialer.keypad.ui

import android.os.Build
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.testutil.composeActivityRule
import com.android.dialer.theme.compose.DialerTheme
import java.util.Locale
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
    fun backspaceStaysVisibleButDisabledWithNothingToDelete() {
        renderScreen(uiState = KeypadUiState(isDeleteEnabled = false))

        composeRule.onNodeWithTag(KEYPAD_DELETE_TEST_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .performClick()

        assertEquals(emptyList<KeypadAction>(), actions)
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
        // As DialpadView described it: the + is announced by the long-press label instead.
        assertEquals("0", keyContentDescription(KeypadKey.ZERO))
    }

    @Test
    fun aSecondAlphabetShowsUnderTheLatinLetters() {
        renderScreen(strings = TEST_KEYPAD_STRINGS.copy(keyLabels = RUSSIAN_LABELS))

        composeRule.onNode(textInKey(KeypadKey.TWO, "ABC"), useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNode(textInKey(KeypadKey.TWO, "АБВГ"), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun keysInARowStayTheSameHeightWithASecondAlphabet() {
        renderScreen(strings = TEST_KEYPAD_STRINGS.copy(keyLabels = RUSSIAN_LABELS))

        // 1 has no letters at all, 2 has two rows of them.
        val one = composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.ONE)).getBoundsInRoot()
        val two = composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.TWO)).getBoundsInRoot()
        assertEquals((two.bottom - two.top).value, (one.bottom - one.top).value, 0.5f)
    }

    @Test
    fun aPersianKeyShowsAndReadsItsPersianDigit() {
        val persian = KeypadKeyLabels.of(Locale.forLanguageTag("fa"), secondaryKeyToChars = null)
        renderScreen(strings = TEST_KEYPAD_STRINGS.copy(keyLabels = persian))

        composeRule.onNode(textInKey(KeypadKey.TWO, "۲"), useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.TWO))
            .assertContentDescriptionEquals("۲, A B C")
    }

    @Test
    fun theOverflowAndBackspaceHaveLargeTouchTargets() {
        renderScreen(KeypadUiState(digits = "5", isDeleteEnabled = true, isOverflowVisible = true))

        listOf(KEYPAD_OVERFLOW_TEST_TAG, KEYPAD_DELETE_TEST_TAG).forEach { tag ->
            composeRule.onNodeWithTag(tag)
                .assertWidthIsAtLeast(56.dp)
                .assertHeightIsAtLeast(56.dp)
        }
    }

    private fun textInKey(key: KeypadKey, text: String) =
        hasText(text) and hasAnyAncestor(hasTestTag(keypadKeyTestTag(key)))

    private fun renderScreen(
        uiState: KeypadUiState = KeypadUiState(),
        strings: KeypadStrings = TEST_KEYPAD_STRINGS,
    ) {
        composeRule.setContent {
            DialerTheme {
                KeypadScreen(
                    uiState = uiState,
                    strings = strings,
                    onAction = { action -> actions += action },
                )
            }
        }
    }

    private companion object {
        private val RUSSIAN_LABELS = KeypadKeyLabels.of(
            locale = Locale.forLanguageTag("ru"),
            secondaryKeyToChars = arrayOf(
                "", "", "АБВГ", "ДЕЁЖЗ", "ИЙКЛ", "МНОП", "РСТУ", "ФХЦЧ", "ШЩЪЫ", "ЬЭЮЯ", "", "",
            ),
        )
    }
}
