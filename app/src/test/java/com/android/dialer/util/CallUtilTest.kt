package com.android.dialer.util

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Telecom is mocked: Robolectric's does not enforce READ_PHONE_NUMBERS, and the failure these tests
 * are about is exactly that enforcement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
class CallUtilTest {

    private val application: Application = RuntimeEnvironment.getApplication()
    private val telecomManager = mockk<TelecomManager>()
    private val handle = PhoneAccountHandle(ComponentName("com.example", "Sim"), "sim1")

    private val context: Context = object : ContextWrapper(application) {
        override fun getSystemService(name: String): Any? = when (name) {
            TELECOM_SERVICE -> telecomManager
            else -> super.getSystemService(name)
        }
    }

    @Before
    fun setUp() {
        shadowOf(application).grantPermissions(Manifest.permission.READ_PHONE_STATE)
        every { telecomManager.callCapablePhoneAccounts } returns listOf(handle)
    }

    // region video calling

    @Test
    fun videoIsAvailableWhenAnAccountSupportsIt() {
        givenAccountWith(PhoneAccount.CAPABILITY_VIDEO_CALLING)

        assertEquals(CallUtil.VIDEO_CALLING_ENABLED, CallUtil.getVideoCallingAvailability(context))
    }

    @Test
    fun videoIsUnavailableWhenNoAccountSupportsIt() {
        givenAccountWith(PhoneAccount.CAPABILITY_CALL_PROVIDER)

        assertEquals(CallUtil.VIDEO_CALLING_DISABLED, CallUtil.getVideoCallingAvailability(context))
    }

    @Test
    fun videoIsUnavailableRatherThanCrashingWithoutThePhoneNumbersPermission() {
        givenAccountsCannotBeRead()

        assertEquals(CallUtil.VIDEO_CALLING_DISABLED, CallUtil.getVideoCallingAvailability(context))
        assertFalse(CallUtil.isVideoEnabled(context))
    }

    // endregion

    // region call subject

    @Test
    fun aCallSubjectIsSupportedWhenAnAccountSupportsIt() {
        givenAccountWith(PhoneAccount.CAPABILITY_CALL_SUBJECT)

        assertTrue(CallUtil.isCallWithSubjectSupported(context))
    }

    @Test
    fun aCallSubjectIsUnsupportedRatherThanCrashingWithoutThePhoneNumbersPermission() {
        givenAccountsCannotBeRead()

        assertFalse(CallUtil.isCallWithSubjectSupported(context))
    }

    // endregion

    @Test
    fun neitherIsCheckedWithoutThePhoneStatePermission() {
        shadowOf(application).denyPermissions(Manifest.permission.READ_PHONE_STATE)
        givenAccountsCannotBeRead()

        assertEquals(CallUtil.VIDEO_CALLING_DISABLED, CallUtil.getVideoCallingAvailability(context))
        assertFalse(CallUtil.isCallWithSubjectSupported(context))
    }

    private fun givenAccountWith(capabilities: Int) {
        every { telecomManager.getPhoneAccount(handle) } returns
            PhoneAccount.builder(handle, "SIM").setCapabilities(capabilities).build()
    }

    private fun givenAccountsCannotBeRead() {
        every { telecomManager.getPhoneAccount(any()) } throws SecurityException(
            "Neither user 10229 nor current process has android.permission.READ_PHONE_NUMBERS.",
        )
    }
}
