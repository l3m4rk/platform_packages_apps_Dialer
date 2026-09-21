package com.android.dialer.keypad

import android.media.ToneGenerator
import android.os.Build
import app.cash.turbine.test
import com.android.dialer.dialpadview.DialerPhoneNumberFormattingTextWatcher
import com.android.dialer.keypad.domain.CheckIfNumberIsProhibited
import com.android.dialer.keypad.domain.DtmfTonePlayer
import com.android.dialer.keypad.domain.EmergencyCallWarning
import com.android.dialer.keypad.domain.LastOutgoingCall
import com.android.dialer.keypad.domain.PhoneNumberFormatting
import com.android.dialer.keypad.domain.TONE_LENGTH_INFINITE
import com.android.dialer.keypad.domain.TONE_LENGTH_MS
import com.android.dialer.keypad.domain.VoicemailAvailability
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.testutil.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
@OptIn(ExperimentalCoroutinesApi::class)
class KeypadViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher = UnconfinedTestDispatcher())

    private val tonePlayer = mockk<DtmfTonePlayer>(relaxed = true)
    private val voicemailAvailability = mockk<VoicemailAvailability>(relaxed = true)
    private val emergencyCallWarning = mockk<EmergencyCallWarning>()
    private val phoneNumberFormatting = mockk<PhoneNumberFormatting>()
    private val lastOutgoingCall = mockk<LastOutgoingCall>()
    private val checkIfNumberIsProhibited = mockk<CheckIfNumberIsProhibited>()

    @Before
    fun setUp() {
        every { emergencyCallWarning.shouldShow() } returns false
        coEvery { phoneNumberFormatting.createWatcher() } returns null
        coEvery { lastOutgoingCall() } returns null
        every { checkIfNumberIsProhibited(any()) } returns false
    }

    // region typing

    @Test
    fun pressingKeysTypesThem() {
        val viewModel = createViewModel()

        viewModel.press(KeypadKey.FIVE, KeypadKey.STAR, KeypadKey.POUND)

        assertEquals("5*#", viewModel.uiState.value.digits)
    }

    @Test
    fun aKeyPressStartsAToneOfUnboundedLength() {
        val viewModel = createViewModel()

        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.SEVEN))

        verify(exactly = 1) {
            tonePlayer.play(ToneGenerator.TONE_DTMF_7, TONE_LENGTH_INFINITE)
        }
        verify(exactly = 0) { tonePlayer.stop() }
    }

    @Test
    fun releasingTheOnlyHeldKeyStopsTheTone() {
        val viewModel = createViewModel()

        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.ONE))
        viewModel.onAction(KeypadAction.KeyReleased(KeypadKey.ONE))

        verify(exactly = 1) { tonePlayer.stop() }
    }

    @Test
    fun releasingOneOfTwoHeldKeysKeepsTheTonePlaying() {
        val viewModel = createViewModel()

        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.ONE))
        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.TWO))
        viewModel.onAction(KeypadAction.KeyReleased(KeypadKey.ONE))

        verify(exactly = 0) { tonePlayer.stop() }

        viewModel.onAction(KeypadAction.KeyReleased(KeypadKey.TWO))

        verify(exactly = 1) { tonePlayer.stop() }
    }

    @Test
    fun releasingAKeyThatWasNeverHeldDoesNotStopAnotherKeysTone() {
        val viewModel = createViewModel()

        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.ONE))
        viewModel.onAction(KeypadAction.KeyReleased(KeypadKey.NINE))

        verify(exactly = 0) { tonePlayer.stop() }
    }

    // endregion

    // region delete

    @Test
    fun deleteRemovesTheLastCharacter() {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE, KeypadKey.TWO, KeypadKey.THREE)

        viewModel.onAction(KeypadAction.DeleteClicked)

        assertEquals("12", viewModel.uiState.value.digits)
    }

    @Test
    fun longPressingDeleteClearsEverything() {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE, KeypadKey.TWO, KeypadKey.THREE)

        viewModel.onAction(KeypadAction.DeleteLongPressed)

        assertEquals("", viewModel.uiState.value.digits)
    }

    @Test
    fun deleteAndOverflowFollowWhetherTheFieldHasContent() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isDeleteEnabled)
        assertFalse(viewModel.uiState.value.isOverflowVisible)

        viewModel.press(KeypadKey.ONE)

        assertTrue(viewModel.uiState.value.isDeleteEnabled)
        assertTrue(viewModel.uiState.value.isOverflowVisible)

        viewModel.onAction(KeypadAction.DeleteClicked)

        assertFalse(viewModel.uiState.value.isDeleteEnabled)
        assertFalse(viewModel.uiState.value.isOverflowVisible)
    }

    // endregion

    // region long press 0

    @Test
    fun longPressingZeroReplacesTheTypedZeroWithPlus() {
        val viewModel = createViewModel()

        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.ZERO))
        viewModel.onAction(KeypadAction.PlusKeyLongPressed)

        assertEquals("+", viewModel.uiState.value.digits)
    }

    @Test
    fun longPressingZeroStopsTheTone() {
        val viewModel = createViewModel()

        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.ZERO))
        viewModel.onAction(KeypadAction.PlusKeyLongPressed)

        verify(exactly = 1) { tonePlayer.stop() }
    }

    @Test
    fun longPressingZeroWithoutAPressTypesPlusWithoutRemovingAnything() {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE, KeypadKey.ZERO)

        // An accessibility service can deliver a long press with no key held.
        viewModel.onAction(KeypadAction.PlusKeyLongPressed)

        assertEquals("10+", viewModel.uiState.value.digits)
    }

    @Test
    fun longPressingZeroLeavesEarlierDigitsAlone() {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE, KeypadKey.TWO)

        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.ZERO))
        viewModel.onAction(KeypadAction.PlusKeyLongPressed)

        assertEquals("12+", viewModel.uiState.value.digits)
    }

    // endregion

    // region long press 1

    @Test
    fun longPressingOneOnAnEmptyFieldCallsVoicemail() = runTest {
        every { voicemailAvailability.isVoicemailReachable() } returns true
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.VoicemailKeyLongPressed)

            assertEquals(KeypadScreenEffect.CallVoicemail, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun longPressingOneRemovesTheDigitsThePressTyped() = runTest {
        every { voicemailAvailability.isVoicemailReachable() } returns true
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE)

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.VoicemailKeyLongPressed)

            assertEquals(KeypadScreenEffect.CallVoicemail, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("", viewModel.uiState.value.digits)
    }

    @Test
    fun longPressingOneAfterTouchExplorationTypedTwoOnesStillCallsVoicemail() = runTest {
        every { voicemailAvailability.isVoicemailReachable() } returns true
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE, KeypadKey.ONE)

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.VoicemailKeyLongPressed)

            assertEquals(KeypadScreenEffect.CallVoicemail, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("", viewModel.uiState.value.digits)
    }

    @Test
    fun longPressingOneWhileDiallingANumberDoesNothing() = runTest {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.FIVE, KeypadKey.ONE)

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.VoicemailKeyLongPressed)

            expectNoEvents()
        }
        assertEquals("51", viewModel.uiState.value.digits)
    }

    @Test
    fun longPressingOneWithoutVoicemailInAirplaneModeExplainsWhy() = runTest {
        every { voicemailAvailability.isVoicemailReachable() } returns false
        every { voicemailAvailability.isAirplaneModeOn() } returns true
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.VoicemailKeyLongPressed)

            assertEquals(KeypadScreenEffect.ShowVoicemailAirplaneModeError, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun longPressingOneWithoutVoicemailOtherwiseReportsItIsNotReady() = runTest {
        every { voicemailAvailability.isVoicemailReachable() } returns false
        every { voicemailAvailability.isAirplaneModeOn() } returns false
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.VoicemailKeyLongPressed)

            assertEquals(KeypadScreenEffect.ShowVoicemailNotReadyError, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region pause and wait

    @Test
    fun pauseAndWaitAreAppendedToTheNumber() {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE, KeypadKey.TWO)

        viewModel.onAction(KeypadAction.PauseClicked)
        viewModel.onAction(KeypadAction.WaitClicked)

        assertEquals("12,;", viewModel.uiState.value.digits)
    }

    @Test
    fun pauseIsRejectedAsTheFirstCharacter() {
        val viewModel = createViewModel()

        viewModel.onAction(KeypadAction.PauseClicked)

        assertEquals("", viewModel.uiState.value.digits)
    }

    // endregion

    // region call button

    @Test
    fun theCallButtonPlacesACallWithTheTypedNumber() = runTest {
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.FIVE, KeypadKey.FIVE, KeypadKey.FIVE)

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.CallClicked)

            assertEquals(KeypadScreenEffect.PlaceCall("555"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aProhibitedNumberIsRefusedAndTheFieldCleared() = runTest {
        every { checkIfNumberIsProhibited("555") } returns true
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.FIVE, KeypadKey.FIVE, KeypadKey.FIVE)

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.CallClicked)

            assertEquals(KeypadScreenEffect.ShowProhibitedNumberError, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("", viewModel.uiState.value.digits)
    }

    @Test
    fun theCallButtonOnAnEmptyFieldRecallsTheLastDialedNumber() {
        coEvery { lastOutgoingCall() } returns "5551234"
        val viewModel = createViewModel()
        viewModel.onHostStarted()

        viewModel.onAction(KeypadAction.CallClicked)

        assertEquals("5551234", viewModel.uiState.value.digits)
    }

    @Test
    fun recallingTheLastNumberDoesNotPlaceACall() = runTest {
        coEvery { lastOutgoingCall() } returns "5551234"
        val viewModel = createViewModel()
        viewModel.onHostStarted()

        viewModel.effects.test {
            viewModel.onAction(KeypadAction.CallClicked)

            expectNoEvents()
        }
    }

    @Test
    fun theCallButtonOnAnEmptyFieldWithNoHistoryPlaysTheErrorTone() {
        coEvery { lastOutgoingCall() } returns null
        val viewModel = createViewModel()
        viewModel.onHostStarted()

        viewModel.onAction(KeypadAction.CallClicked)

        verify(exactly = 1) {
            tonePlayer.play(ToneGenerator.TONE_PROP_NACK, TONE_LENGTH_MS)
        }
        assertEquals("", viewModel.uiState.value.digits)
    }

    @Test
    fun theRecalledNumberCanBeEditedFromItsEnd() {
        coEvery { lastOutgoingCall() } returns "5551234"
        val viewModel = createViewModel()
        viewModel.onHostStarted()
        viewModel.onAction(KeypadAction.CallClicked)

        // The cursor must sit past the end of the recalled number, so backspace trims it rather
        // than doing nothing.
        viewModel.onAction(KeypadAction.DeleteClicked)

        assertEquals("555123", viewModel.uiState.value.digits)
    }

    // endregion

    // region lifecycle and hint

    @Test
    fun theToneGeneratorFollowsTheHostLifecycle() {
        val viewModel = createViewModel()

        viewModel.onHostStarted()
        verify(exactly = 1) { tonePlayer.acquire() }

        viewModel.onHostStopped()
        verify(exactly = 1) { tonePlayer.release() }
    }

    @Test
    fun stoppingTheHostForgetsHeldKeysSoTheNextPressStillStops() {
        val viewModel = createViewModel()
        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.ONE))

        viewModel.onHostStopped()
        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.TWO))
        viewModel.onAction(KeypadAction.KeyReleased(KeypadKey.TWO))

        verify(exactly = 1) { tonePlayer.stop() }
    }

    @Test
    fun theEmergencyWarningShowsOnlyWhileTheFieldIsEmpty() {
        every { emergencyCallWarning.shouldShow() } returns true
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.showsEmergencyCallWarning)

        viewModel.press(KeypadKey.ONE)

        assertFalse(viewModel.uiState.value.showsEmergencyCallWarning)
    }

    @Test
    fun theEmergencyWarningIsRefreshedWhenTheHostReturns() {
        every { emergencyCallWarning.shouldShow() } returns false
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.showsEmergencyCallWarning)

        // Airplane mode went on while the keypad was away; the digits never changed, so nothing
        // else would prompt a re-read.
        every { emergencyCallWarning.shouldShow() } returns true
        viewModel.onHostStarted()

        assertTrue(viewModel.uiState.value.showsEmergencyCallWarning)
    }

    @Test
    fun theEmergencyWarningIsRefreshedWhenTheFieldEmptiesAgain() {
        every { emergencyCallWarning.shouldShow() } returns false
        val viewModel = createViewModel()
        viewModel.press(KeypadKey.ONE)

        every { emergencyCallWarning.shouldShow() } returns true
        viewModel.onAction(KeypadAction.DeleteClicked)

        assertTrue(viewModel.uiState.value.showsEmergencyCallWarning)
    }

    @Test
    fun theEmergencyWarningIsNotLookedUpWhileTheFieldHasContent() {
        val viewModel = createViewModel()
        // Forget the lookup done on construction, while the field was still empty.
        clearMocks(emergencyCallWarning, answers = false)

        viewModel.press(KeypadKey.ONE)

        verify(exactly = 0) { emergencyCallWarning.shouldShow() }
    }

    // endregion

    // region formatting

    @Test
    fun theNumberIsFormattedAsItIsTyped() {
        coEvery { phoneNumberFormatting.createWatcher() } returns
            DialerPhoneNumberFormattingTextWatcher("US")
        val viewModel = createViewModel()

        viewModel.press(
            KeypadKey.SIX, KeypadKey.FIVE, KeypadKey.ZERO,
            KeypadKey.FIVE, KeypadKey.FIVE, KeypadKey.FIVE,
            KeypadKey.ONE, KeypadKey.TWO, KeypadKey.ONE, KeypadKey.TWO,
        )

        assertEquals("(650) 555-1212", viewModel.uiState.value.digits)
    }

    @Test
    fun typingStillWorksWhenFormattingIsUnavailable() {
        coEvery { phoneNumberFormatting.createWatcher() } returns null
        coEvery { lastOutgoingCall() } returns null
        every { checkIfNumberIsProhibited(any()) } returns false
        val viewModel = createViewModel()

        viewModel.press(KeypadKey.SIX, KeypadKey.FIVE, KeypadKey.ZERO)

        assertEquals("650", viewModel.uiState.value.digits)
    }

    // endregion

    private fun createViewModel() = KeypadViewModel(
        tonePlayer = tonePlayer,
        voicemailAvailability = voicemailAvailability,
        emergencyCallWarning = emergencyCallWarning,
        phoneNumberFormatting = phoneNumberFormatting,
        lastOutgoingCall = lastOutgoingCall,
        checkIfNumberIsProhibited = checkIfNumberIsProhibited,
    )

    private fun KeypadViewModel.press(vararg keys: KeypadKey) {
        keys.forEach { key ->
            onAction(KeypadAction.KeyPressed(key))
            onAction(KeypadAction.KeyReleased(key))
        }
    }
}
