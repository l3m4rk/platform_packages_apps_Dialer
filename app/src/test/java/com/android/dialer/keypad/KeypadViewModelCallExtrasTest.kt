package com.android.dialer.keypad

import app.cash.turbine.test
import com.android.dialer.dialpadview.DialerPhoneNumberFormattingTextWatcher
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.keypad.model.PseudoEmergency
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Call with a note, and the pseudo-emergency Easter egg on the call button. */
@OptIn(ExperimentalCoroutinesApi::class)
class KeypadViewModelCallExtrasTest : BaseKeypadViewModelTest() {

    // region call with a note

    @Test
    fun callWithANoteIsOfferedWhenAnAccountSupportsIt() {
        every { callWithNoteAvailability.isAvailable() } returns true
        val viewModel = createViewModel()

        viewModel.onHostStarted()

        assertTrue(viewModel.uiState.value.isCallWithNoteAvailable)
    }

    @Test
    fun callWithANoteIsNotOfferedOtherwise() {
        val viewModel = createViewModel()

        viewModel.onHostStarted()

        assertFalse(viewModel.uiState.value.isCallWithNoteAvailable)
    }

    @Test
    fun callWithANoteIsReCheckedEachTimeTheKeypadStarts() {
        val viewModel = createViewModel()
        viewModel.onHostStarted()
        viewModel.onHostStopped()

        // The SIM or carrier changed while the keypad was away.
        every { callWithNoteAvailability.isAvailable() } returns true
        viewModel.onHostStarted()

        assertTrue(viewModel.uiState.value.isCallWithNoteAvailable)
    }

    @Test
    fun callingWithANoteHandsOverTheNumberAsShown() = runTest {
        coEvery { phoneNumberFormatting.createWatcher() } returns
            DialerPhoneNumberFormattingTextWatcher("US")
        val viewModel = createViewModel()
        viewModel.type("6502530000")

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.CallWithNoteClicked)

            assertEquals(KeypadScreenEffect.CallWithNote("(650) 253-0000"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region pseudo-emergency

    @Test
    fun spellingThePseudoEmergencyNumberFlagsIt() {
        val viewModel = createViewModel()

        viewModel.type(PseudoEmergency.NUMBER)

        assertTrue(viewModel.uiState.value.isPseudoEmergencyNumber)
    }

    @Test
    fun theFlagClearsAsSoonAsTheNumberIsEdited() {
        val viewModel = createViewModel()
        viewModel.type(PseudoEmergency.NUMBER)

        viewModel.onAction(KeypadAction.DeleteClicked)

        assertFalse(viewModel.uiState.value.isPseudoEmergencyNumber)
    }

    @Test
    fun thePulseBuzzesAtEveryRepeatAndOnceMoreASecondAfterItEnds() {
        val viewModel = createViewModel()

        viewModel.type(PseudoEmergency.NUMBER)

        // Six repeats, one every leg.
        repeat(6) { index ->
            verify(exactly = index) { vibration.vibrate(PseudoEmergency.PULSE_MS) }
            advanceTimeBy(PseudoEmergency.PULSE_MS)
        }
        verify(exactly = 6) { vibration.vibrate(PseudoEmergency.PULSE_MS) }

        // The seventh leg ends the pulse; the last buzz follows a second later.
        advanceTimeBy(PseudoEmergency.PULSE_MS + PseudoEmergency.FINAL_BUZZ_DELAY_MS - 1)
        verify(exactly = 6) { vibration.vibrate(PseudoEmergency.PULSE_MS) }
        advanceTimeBy(1)
        verify(exactly = 7) { vibration.vibrate(PseudoEmergency.PULSE_MS) }

        advanceTimeBy(ONE_MINUTE_MS)
        verify(exactly = 7) { vibration.vibrate(any()) }
    }

    @Test
    fun editingTheNumberMidPulseStopsItButStillBuzzesOnceMore() {
        val viewModel = createViewModel()

        viewModel.type(PseudoEmergency.NUMBER)
        advanceTimeBy(PseudoEmergency.PULSE_MS * 2)
        verify(exactly = 2) { vibration.vibrate(any()) }

        viewModel.onAction(KeypadAction.DeleteClicked)
        advanceTimeBy(PseudoEmergency.FINAL_BUZZ_DELAY_MS)

        verify(exactly = 3) { vibration.vibrate(any()) }
        advanceTimeBy(ONE_MINUTE_MS)
        verify(exactly = 3) { vibration.vibrate(any()) }
    }

    @Test
    fun editingTheNumberAfterThePulseDoesNotBuzzAgain() {
        val viewModel = createViewModel()
        viewModel.type(PseudoEmergency.NUMBER)
        advanceTimeBy(PseudoEmergency.PULSE_MS * PseudoEmergency.PULSES)

        viewModel.onAction(KeypadAction.DeleteClicked)
        advanceTimeBy(ONE_MINUTE_MS)

        // Six repeats and the one after the end; the edit adds nothing.
        verify(exactly = 7) { vibration.vibrate(any()) }
    }

    @Test
    fun otherNumbersNeverBuzz() {
        val viewModel = createViewModel()

        viewModel.type("0118999881999119725")
        advanceTimeBy(ONE_MINUTE_MS)

        verify(exactly = 0) { vibration.vibrate(any()) }
    }

    // endregion

    private fun advanceTimeBy(millis: Long) {
        mainDispatcherRule.testDispatcher.scheduler.apply {
            advanceTimeBy(millis)
            runCurrent()
        }
    }

    private companion object {
        private const val ONE_MINUTE_MS = 60_000L
    }
}
