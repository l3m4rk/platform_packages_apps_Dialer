package com.android.dialer.keypad.domain

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telephony.TelephonyManager
import com.android.dialer.telecom.TelecomUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
@OptIn(ExperimentalCoroutinesApi::class)
class SystemVoicemailAvailabilityTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val telephonyManager = mockk<TelephonyManager>()
    private val simAccount = mockk<PhoneAccountHandle>()
    private val secondSimAccount = mockk<PhoneAccountHandle>()

    @Before
    fun setUp() {
        mockkStatic(TelecomUtil::class)
        every { TelecomUtil.getSubscriptionPhoneAccounts(any()) } returns emptyList()
        every { TelecomUtil.getDefaultOutgoingPhoneAccount(any(), any()) } returns null
        every { TelecomUtil.getVoicemailNumber(any(), any()) } returns null
        every { telephonyManager.voiceMailNumber } returns null
    }

    @After
    fun tearDown() {
        unmockkStatic(TelecomUtil::class)
    }

    @Test
    fun singleSimIsReachableWhenTelephonyHasAVoicemailNumber() = runTest {
        every { telephonyManager.voiceMailNumber } returns "+15551234567"

        assertTrue(createAvailability().isVoicemailReachable())
    }

    @Test
    fun singleSimIsNotReachableWithoutAVoicemailNumber() = runTest {
        every { telephonyManager.voiceMailNumber } returns null

        assertFalse(createAvailability().isVoicemailReachable())
    }

    @Test
    fun singleSimIsNotReachableWithAnEmptyVoicemailNumber() = runTest {
        every { telephonyManager.voiceMailNumber } returns ""

        assertFalse(createAvailability().isVoicemailReachable())
    }

    @Test
    fun aChosenAccountIsAskedInsteadOfTelephony() = runTest {
        every { TelecomUtil.getDefaultOutgoingPhoneAccount(any(), any()) } returns simAccount
        every { TelecomUtil.getVoicemailNumber(any(), simAccount) } returns "+15557654321"
        // Telephony would answer differently; the chosen account must win.
        every { telephonyManager.voiceMailNumber } returns null

        assertTrue(createAvailability().isVoicemailReachable())
    }

    @Test
    fun multiSimWithoutADefaultIsReachableSoTelecomCanAsk() = runTest {
        every { TelecomUtil.getSubscriptionPhoneAccounts(any()) } returns
            listOf(simAccount, secondSimAccount)
        every { TelecomUtil.getDefaultOutgoingPhoneAccount(any(), any()) } returns null
        every { telephonyManager.voiceMailNumber } returns null

        // No number anywhere, but the call is still placed so Telecom shows its "Call with" picker.
        assertTrue(createAvailability().isVoicemailReachable())
    }

    @Test
    fun multiSimWithADefaultFallsBackToTheNumberCheck() = runTest {
        every { TelecomUtil.getSubscriptionPhoneAccounts(any()) } returns
            listOf(simAccount, secondSimAccount)
        every { TelecomUtil.getDefaultOutgoingPhoneAccount(any(), any()) } returns simAccount
        every { TelecomUtil.getVoicemailNumber(any(), simAccount) } returns null

        assertFalse(createAvailability().isVoicemailReachable())
    }

    @Test
    fun isNotReachableWhenReadingTheAccountsThrows() = runTest {
        every { TelecomUtil.getSubscriptionPhoneAccounts(any()) } throws
            SecurityException("no READ_PHONE_STATE")

        assertFalse(createAvailability().isVoicemailReachable())
    }

    @Test
    fun isNotReachableWhenReadingTheVoicemailNumberThrows() = runTest {
        every { telephonyManager.voiceMailNumber } throws SecurityException("no READ_PHONE_STATE")

        assertFalse(createAvailability().isVoicemailReachable())
    }

    @Test
    fun airplaneModeIsReadFromGlobalSettings() = runTest {
        setAirplaneMode(enabled = true)

        assertTrue(createAvailability().isAirplaneModeOn())
    }

    @Test
    fun airplaneModeIsOffByDefault() = runTest {
        setAirplaneMode(enabled = false)

        assertFalse(createAvailability().isAirplaneModeOn())
    }

    private fun createAvailability() = SystemVoicemailAvailability(
        context = context,
        telephonyManager = telephonyManager,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private fun setAirplaneMode(enabled: Boolean) {
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            if (enabled) 1 else 0,
        )
    }
}
