package com.android.dialer.keypad.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.android.dialer.keypad.model.DigitsValue
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.testutil.composeActivityRule
import com.android.dialer.theme.compose.DialerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The number as an editable field, driven through the whole screen. A fake stands in for the view
 * model: it applies each edit as given, where the real one would filter and format it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA], qualifiers = "w411dp-h891dp")
class KeypadDigitsFieldTest {

    @get:Rule(order = 0)
    val componentActivityRule = composeActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val actions = mutableListOf<KeypadAction>()
    private var field by mutableStateOf(DigitsValue())

    @Test
    fun theNumberTakesFocusWhenShown() {
        render()

        composeRule.onNodeWithTag(KEYPAD_DIGITS_TEST_TAG).assertIsFocused()
    }

    @Test
    fun aHardwareKeyIsTypedAsACommandNotAnEdit() {
        render()

        composeRule.onNodeWithTag(KEYPAD_DIGITS_TEST_TAG).performKeyInput { pressKey(Key.Five) }

        // Applied at the cursor by the view model; the field never edits the text itself.
        assertEquals(listOf(KeypadAction.CharacterTyped('5')), actions)
    }

    @Test
    fun keysPressedWithinOneFrameAllArriveInOrder() {
        render()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithTag(KEYPAD_DIGITS_TEST_TAG).performKeyInput {
            pressKey(Key.Six)
            pressKey(Key.Five)
            pressKey(Key.Zero)
        }

        assertEquals(
            listOf('6', '5', '0').map(KeypadAction::CharacterTyped),
            actions,
        )
    }

    @Test
    fun hardwareBackspaceDeletesAsTheKeypadsDoes() {
        field = DigitsValue("13", selectionStart = 1, selectionEnd = 1)
        render()

        composeRule.onNodeWithTag(KEYPAD_DIGITS_TEST_TAG).performKeyInput {
            pressKey(Key.Backspace)
        }

        assertEquals(listOf(KeypadAction.DeleteClicked), actions)
    }

    @Test
    fun enterCalls() {
        field = DigitsValue("555", selectionStart = 3, selectionEnd = 3)
        render()

        composeRule.onNodeWithTag(KEYPAD_DIGITS_TEST_TAG).performKeyInput { pressKey(Key.Enter) }

        assertEquals(KeypadAction.CallClicked, actions.last())
        // Dialing, not a line break.
        assertTrue(actions.none { it is KeypadAction.DigitsEdited })
    }

    @Test
    fun pasteHandsTheClipboardToTheViewModelAsIs() {
        val clipboard = RuntimeEnvironment.getApplication()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("number", "1-800-FLOWERS"))
        render()

        composeRule.onNodeWithTag(KEYPAD_DIGITS_TEST_TAG)
            .performSemanticsAction(SemanticsActions.PasteText)
        composeRule.waitForIdle()

        // Unfiltered here: the letters become digits in the view model, as for any edit.
        assertEquals("1-800-FLOWERS", lastEdit().text)
    }

    private fun lastEdit(): KeypadAction.DigitsEdited =
        actions.filterIsInstance<KeypadAction.DigitsEdited>().last()

    private fun render() {
        composeRule.setContent {
            DialerTheme {
                KeypadScreen(
                    uiState = KeypadUiState(
                        digits = field.text,
                        selectionStart = field.selectionStart,
                        selectionEnd = field.selectionEnd,
                        isDeleteEnabled = field.text.isNotEmpty(),
                        isOverflowVisible = field.text.isNotEmpty(),
                    ),
                    strings = STRINGS,
                    onAction = { action ->
                        actions += action
                        if (action is KeypadAction.DigitsEdited) {
                            field = DigitsValue(
                                action.text,
                                action.selectionStart,
                                action.selectionEnd,
                            )
                        }
                    },
                )
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
