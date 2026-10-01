package com.android.dialer.keypad.domain

import android.app.Application
import android.os.Build
import com.android.dialer.util.CallUtil
import io.mockk.every
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

/**
 * `CallUtil` itself is stubbed: Robolectric registers no phone accounts, so the three outcomes that
 * matter could not be produced by the real thing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
@OptIn(ExperimentalCoroutinesApi::class)
class SystemCallWithNoteAvailabilityTest {

    private val application: Application = RuntimeEnvironment.getApplication()
    private val availability = SystemCallWithNoteAvailability(
        context = application,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Before
    fun setUp() {
        mockkStatic(CallUtil::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(CallUtil::class)
    }

    @Test
    fun availableWhenAnAccountSupportsACallSubject() = runTest {
        every { CallUtil.isCallWithSubjectSupported(any()) } returns true

        assertTrue(availability.isAvailable())
    }

    @Test
    fun unavailableWhenNoAccountDoes() = runTest {
        every { CallUtil.isCallWithSubjectSupported(any()) } returns false

        assertFalse(availability.isAvailable())
    }

    @Test
    fun unavailableRatherThanCrashingWithoutThePhoneNumbersPermission() = runTest {
        every { CallUtil.isCallWithSubjectSupported(any()) } throws
            SecurityException("Neither user nor current process has READ_PHONE_NUMBERS.")

        assertFalse(availability.isAvailable())
    }
}
