package com.android.dialer.keypad

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.dialer.keypad.domain.CallWithNoteAvailability
import com.android.dialer.keypad.domain.CheckIfNumberIsProhibited
import com.android.dialer.keypad.domain.DialIntentNumber
import com.android.dialer.keypad.domain.DtmfTonePlayer
import com.android.dialer.keypad.domain.EmergencyCallWarning
import com.android.dialer.keypad.domain.LastOutgoingCall
import com.android.dialer.keypad.domain.PhoneNumberFormatting
import com.android.dialer.keypad.domain.Vibration
import com.android.dialer.keypad.domain.VoicemailAvailability
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
@OptIn(ExperimentalCoroutinesApi::class)
// Abstract so that JUnit does not run the base, which has no tests, as a test class of its own.
@Suppress("AbstractClassCanBeConcreteClass")
abstract class BaseKeypadViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher = UnconfinedTestDispatcher())

    internal val tonePlayer = mockk<DtmfTonePlayer>(relaxed = true)
    internal val voicemailAvailability = mockk<VoicemailAvailability>(relaxed = true)
    internal val emergencyCallWarning = mockk<EmergencyCallWarning>()
    internal val phoneNumberFormatting = mockk<PhoneNumberFormatting>()
    internal val lastOutgoingCall = mockk<LastOutgoingCall>()
    internal val checkIfNumberIsProhibited = mockk<CheckIfNumberIsProhibited>()
    internal val dialIntentNumber = mockk<DialIntentNumber>()
    internal val vibration = mockk<Vibration>(relaxed = true)
    internal val callWithNoteAvailability = mockk<CallWithNoteAvailability>()

    @Before
    fun setUpCollaborators() {
        coEvery { emergencyCallWarning.shouldShow() } returns false
        coEvery { phoneNumberFormatting.createWatcher() } returns null
        coEvery { lastOutgoingCall() } returns null
        every { checkIfNumberIsProhibited(any()) } returns false
        coEvery { dialIntentNumber(any()) } returns null
        coEvery { callWithNoteAvailability.isAvailable() } returns false
    }

    internal fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = KeypadViewModel(
        tonePlayer = tonePlayer,
        voicemailAvailability = voicemailAvailability,
        emergencyCallWarning = emergencyCallWarning,
        phoneNumberFormatting = phoneNumberFormatting,
        lastOutgoingCall = lastOutgoingCall,
        checkIfNumberIsProhibited = checkIfNumberIsProhibited,
        dialIntentNumber = dialIntentNumber,
        vibration = vibration,
        callWithNoteAvailability = callWithNoteAvailability,
        savedStateHandle = savedStateHandle,
    )

    internal fun KeypadViewModel.press(vararg keys: KeypadKey) {
        keys.forEach { key ->
            onAction(KeypadAction.KeyPressed(key))
            onAction(KeypadAction.KeyReleased(key))
        }
    }

    internal fun KeypadViewModel.type(number: String) {
        val keys = number.map { char -> KeypadKey.entries.first { key -> key.char == char } }
        press(*keys.toTypedArray())
    }
}
