package com.android.dialer.keypad

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadScreenEffect
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** What reaches the number other than the keys: dial intents, special codes, the text field. */
class KeypadViewModelInputTest : BaseKeypadViewModelTest() {

    // region dial intent

    @Test
    fun aDialIntentReplacesWhatWasTyped() {
        coEvery { dialIntentNumber(any()) } returns "(555) 123-4567"
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.NINE, KeypadKey.NINE)

        viewModel.fillFromDialIntent(Intent(Intent.ACTION_DIAL))

        assertEquals("(555) 123-4567", viewModel.uiState.value.digits)
    }

    @Test
    fun keysTypedAfterADialIntentGoAtTheEnd() {
        coEvery { dialIntentNumber(any()) } returns "555"
        val viewModel = createViewModel()

        viewModel.fillFromDialIntent(Intent(Intent.ACTION_DIAL))
        viewModel.press(KeypadKey.ONE)

        assertEquals("5551", viewModel.uiState.value.digits)
    }

    @Test
    fun aNewerDialIntentWinsOverOneStillBeingRead() {
        val slowContact = CompletableDeferred<String?>()
        val contactIntent = Intent(Intent.ACTION_DIAL)
        val telIntent = Intent(Intent.ACTION_VIEW)
        coEvery { dialIntentNumber(contactIntent) } coAnswers { slowContact.await() }
        coEvery { dialIntentNumber(telIntent) } returns "555-1234"
        val viewModel = createViewModel()

        viewModel.fillFromDialIntent(contactIntent)
        viewModel.fillFromDialIntent(telIntent)
        slowContact.complete("(650) 253-0000")

        assertEquals("555-1234", viewModel.uiState.value.digits)
    }

    @Test
    fun clearingCancelsADialIntentStillBeingRead() {
        val slowContact = CompletableDeferred<String?>()
        coEvery { dialIntentNumber(any()) } coAnswers { slowContact.await() }
        val viewModel = createViewModel()

        viewModel.fillFromDialIntent(Intent(Intent.ACTION_DIAL))
        viewModel.clearDigits()
        slowContact.complete("(650) 253-0000")

        assertEquals("", viewModel.uiState.value.digits)
    }

    @Test
    fun aDialIntentWithoutANumberLeavesTheFieldAlone() {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE, KeypadKey.TWO)

        viewModel.fillFromDialIntent(Intent(Intent.ACTION_DIAL))

        assertEquals("12", viewModel.uiState.value.digits)
    }

    // endregion

    // region special codes

    @Test
    fun everyChangeTheUserTypesIsCheckedForASpecialCode() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.press(KeypadKey.STAR, KeypadKey.POUND)

            assertEquals(KeypadScreenEffect.RunSpecialCode("*"), awaitItem())
            assertEquals(KeypadScreenEffect.RunSpecialCode("*#"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun emptyingTheFieldIsNotCheckedForASpecialCode() = runTest {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE)

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.DeleteClicked)

            expectNoEvents()
        }
    }

    @Test
    fun aNumberFromADialIntentIsNeverRunAsASpecialCode() = runTest {
        coEvery { dialIntentNumber(any()) } returns "*#06#"
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.fillFromDialIntent(Intent(Intent.ACTION_DIAL))
            // Typing onto it does not make it the user's either.
            viewModel.press(KeypadKey.ONE)

            expectNoEvents()
        }
        assertEquals("*#06#1", viewModel.uiState.value.digits)
    }

    @Test
    fun codesRunAgainOnceTheUserEmptiesAFieldADialIntentFilled() = runTest {
        coEvery { dialIntentNumber(any()) } returns "555"
        val viewModel = createViewModel()
        viewModel.fillFromDialIntent(Intent(Intent.ACTION_DIAL))

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.DeleteLongPressed)
            viewModel.press(KeypadKey.STAR)

            assertEquals(KeypadScreenEffect.RunSpecialCode("*"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun aSimContactNumberGoesInFrontOfTheField() {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.NINE)

        viewModel.insertSimContactNumber("555")

        assertEquals("5559", viewModel.uiState.value.digits)
    }

    // endregion

    // region digits field

    @Test
    fun anEditInTheFieldChangesTheNumberAndItsCursor() {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE, KeypadKey.THREE)

        viewModel.onAction(KeypadAction.DigitsEdited("123", selectionStart = 2, selectionEnd = 2))

        assertEquals("123", viewModel.uiState.value.digits)
        assertEquals(2, viewModel.uiState.value.selectionStart)
        assertEquals(2, viewModel.uiState.value.selectionEnd)
    }

    @Test
    fun aCharacterTypedOnAHardwareKeyboardGoesInAtTheCursor() {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE, KeypadKey.THREE)
        viewModel.onAction(KeypadAction.DigitsEdited("13", selectionStart = 1, selectionEnd = 1))

        viewModel.onAction(KeypadAction.CharacterTyped('2'))

        assertEquals("123", viewModel.uiState.value.digits)
        assertEquals(2, viewModel.uiState.value.selectionStart)
    }

    @Test
    fun aLetterTypedOnAHardwareKeyboardBecomesItsKeypadDigit() {
        val viewModel = createViewModel()

        viewModel.onAction(KeypadAction.CharacterTyped('a'))

        assertEquals("2", viewModel.uiState.value.digits)
    }

    @Test
    fun aHardwareKeystrokePlaysNoTone() {
        val viewModel = createViewModel()

        viewModel.onAction(KeypadAction.CharacterTyped('5'))

        verify(exactly = 0) { tonePlayer.play(any(), any()) }
    }

    @Test
    fun keypadTypingMovesTheFieldsCursorAlong() {
        val viewModel = createViewModel()

        viewModel.press(KeypadKey.SEVEN, KeypadKey.EIGHT)

        assertEquals(2, viewModel.uiState.value.selectionStart)
        assertEquals(2, viewModel.uiState.value.selectionEnd)
    }

    @Test
    fun aPastedNumberIsFilteredLikeTypedOne() {
        val viewModel = createViewModel()
        viewModel.onAction(
            KeypadAction.DigitsEdited("1-800-FLOWERS", selectionStart = 13, selectionEnd = 13),
        )

        assertEquals("1-800-3569377", viewModel.uiState.value.digits)
    }

    @Test
    fun aPastedCodeIsCheckedLikeATypedOne() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            // The legacy EditText ran codes on paste too: it checked every change of text.
            viewModel.onAction(
                KeypadAction.DigitsEdited("*#07#", selectionStart = 5, selectionEnd = 5),
            )

            assertEquals(KeypadScreenEffect.RunSpecialCode("*#07#"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region saved state

    @Test
    fun theNumberAndItsCursorAreSaved() {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle)
        viewModel.press(KeypadKey.ONE, KeypadKey.THREE)

        viewModel.onAction(KeypadAction.DigitsEdited("13", selectionStart = 1, selectionEnd = 1))

        assertEquals("13", handle.get<String>(KEY_DIGITS))
        assertEquals(1, handle.get<Int>(KEY_SELECTION_START))
        assertEquals(1, handle.get<Int>(KEY_SELECTION_END))
    }

    @Test
    fun aSavedNumberComesBackWithItsCursor() {
        val handle = savedNumber("555-1234", cursor = 3)

        val viewModel = createViewModel(handle)
        viewModel.onAction(KeypadAction.CharacterTyped('9'))

        assertEquals("5559-1234", viewModel.uiState.value.digits)
    }

    @Test
    fun theIntentFlagIsSavedWhileTheNumberCameFromAnotherApp() {
        coEvery { dialIntentNumber(any()) } returns "555"
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle)

        viewModel.fillFromDialIntent(Intent(Intent.ACTION_DIAL))
        assertEquals(true, handle.get<Boolean>(KEY_FILLED_BY_INTENT))

        viewModel.clearDigits()
        assertEquals(false, handle.get<Boolean>(KEY_FILLED_BY_INTENT))
    }

    @Test
    fun aRestoredNumberFromAnotherAppStillCannotRunACode() = runTest {
        val handle = savedNumber("*#06", cursor = 4, filledByIntent = true)
        val viewModel = createViewModel(handle)

        viewModel.effects.test {
            viewModel.press(KeypadKey.POUND)

            expectNoEvents()
        }
        assertEquals("*#06#", viewModel.uiState.value.digits)
    }

    @Test
    fun aRestoredNumberTheUserTypedRunsCodesAsBefore() = runTest {
        val viewModel = createViewModel(savedNumber("*#06", cursor = 4))

        viewModel.effects.test {
            viewModel.press(KeypadKey.POUND)

            assertEquals(KeypadScreenEffect.RunSpecialCode("*#06#"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    private fun savedNumber(text: String, cursor: Int, filledByIntent: Boolean = false) =
        SavedStateHandle(
            mapOf(
                KEY_DIGITS to text,
                KEY_SELECTION_START to cursor,
                KEY_SELECTION_END to cursor,
                KEY_FILLED_BY_INTENT to filledByIntent,
            ),
        )

    private companion object {
        // The view model's own keys are private. Spelled out again here on purpose: saved state
        // outlives the process, so renaming a key would silently drop what a user had typed.
        private const val KEY_DIGITS = "keypad_digits"
        private const val KEY_SELECTION_START = "keypad_selection_start"
        private const val KEY_SELECTION_END = "keypad_selection_end"
        private const val KEY_FILLED_BY_INTENT = "keypad_filled_by_intent"
    }
}
