package com.android.dialer.keypad.domain

import android.content.Context
import android.os.Build
import com.android.dialer.keypad.model.DialpadDigits
import com.android.dialer.location.GeoUtil
import com.android.dialer.oem.MotorolaUtils
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
@OptIn(ExperimentalCoroutinesApi::class)
class SystemPhoneNumberFormattingTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        mockkStatic(MotorolaUtils::class)
        mockkStatic(GeoUtil::class)
        every { MotorolaUtils.shouldDisablePhoneNumberFormatting(any()) } returns false
        every { GeoUtil.getCurrentCountryIso(any()) } returns "US"
    }

    @After
    fun tearDown() {
        unmockkStatic(MotorolaUtils::class)
        unmockkStatic(GeoUtil::class)
    }

    @Test
    fun buildsAWatcherByDefault() = runTest {
        assertNotNull(createFormatting().createWatcher())
    }

    @Test
    fun buildsNoWatcherWhereTheDeviceDisablesFormatting() = runTest {
        // Some Motorola devices turn as-you-type formatting off entirely.
        every { MotorolaUtils.shouldDisablePhoneNumberFormatting(any()) } returns true

        assertNull(createFormatting().createWatcher())
    }

    @Test
    fun theWatcherFormatsForTheDeviceCountry() = runTest {
        every { GeoUtil.getCurrentCountryIso(any()) } returns "US"

        assertEquals("(650) 555-1212", formatAsTyped("6505551212"))
    }

    @Test
    fun theCountryComesFromTheDeviceRatherThanAHardcodedDefault() = runTest {
        every { GeoUtil.getCurrentCountryIso(any()) } returns "EE"

        // Estonian grouping, not the US grouping asserted above, so the device's country really
        // does reach the watcher instead of a hardcoded default.
        assertEquals("5123 4567", formatAsTyped("51234567"))
    }

    private fun createFormatting() = SystemPhoneNumberFormatting(
        context = context,
        defaultDispatcher = UnconfinedTestDispatcher(),
    )

    private suspend fun formatAsTyped(input: String): String {
        val digits = DialpadDigits()
        createFormatting().createWatcher()?.let(digits::addFormattingWatcher)
        input.forEach { digits.append(it) }
        return digits.text.value
    }
}
