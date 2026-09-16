package com.android.dialer.keypad.domain

import android.Manifest
import android.app.Application
import android.os.Build
import android.provider.CallLog.Calls
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The call log itself is stubbed rather than populated: Robolectric does not register the CallLog
 * provider, so `Calls.getLastOutgoingCall` returns null there no matter what is inserted. Stubbing
 * the static keeps the three results that matter under test — a number, the documented empty
 * string, and the null a real device returns when the log cannot be queried.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
@OptIn(ExperimentalCoroutinesApi::class)
class SystemLastOutgoingCallTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        shadowOf(application).grantPermissions(Manifest.permission.READ_CALL_LOG)
        mockkStatic(Calls::class)
        every { Calls.getLastOutgoingCall(any()) } returns ""
    }

    @After
    fun tearDown() {
        unmockkStatic(Calls::class)
    }

    @Test
    fun returnsTheMostRecentOutgoingNumber() = runTest {
        every { Calls.getLastOutgoingCall(any()) } returns "+15551234567"

        assertEquals("+15551234567", createLastOutgoingCall().invoke())
    }

    @Test
    fun returnsNullWhenTheCallLogHasNoOutgoingCall() = runTest {
        every { Calls.getLastOutgoingCall(any()) } returns ""

        assertNull(createLastOutgoingCall().invoke())
    }

    @Test
    fun returnsNullWhenTheCallLogCannotBeQueried() = runTest {
        every { Calls.getLastOutgoingCall(any()) } returns null

        assertNull(createLastOutgoingCall().invoke())
    }

    @Test
    fun doesNotTouchTheCallLogWithoutPermission() = runTest {
        shadowOf(application).denyPermissions(Manifest.permission.READ_CALL_LOG)

        assertNull(createLastOutgoingCall().invoke())

        verify(exactly = 0) { Calls.getLastOutgoingCall(any()) }
    }

    private fun createLastOutgoingCall() = SystemLastOutgoingCall(
        context = application,
        ioDispatcher = UnconfinedTestDispatcher(),
    )
}
