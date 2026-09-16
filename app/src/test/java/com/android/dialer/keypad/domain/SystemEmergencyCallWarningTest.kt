package com.android.dialer.keypad.domain

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private const val EMERGENCY_NOTIFICATION_DELAY_KEY = "emergency_notification_delay_int"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
class SystemEmergencyCallWarningTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val telephonyManager = mockk<TelephonyManager>()

    @Before
    fun setUp() {
        grantReadPhoneState()
        setCarrierDelay(delayMillis = 5_000)
        setServiceState(ServiceState.STATE_OUT_OF_SERVICE)
    }

    @Test
    fun warnsWhenTheCarrierAsksForItAndThereIsNoService() {
        assertTrue(createWarning().shouldShow())
    }

    @Test
    fun warnsWhenTheRadioIsOff() {
        setServiceState(ServiceState.STATE_POWER_OFF)

        assertTrue(createWarning().shouldShow())
    }

    @Test
    fun doesNotWarnWhileInService() {
        setServiceState(ServiceState.STATE_IN_SERVICE)

        assertFalse(createWarning().shouldShow())
    }

    @Test
    fun doesNotWarnWhileEmergencyCallsAreStillPossible() {
        setServiceState(ServiceState.STATE_EMERGENCY_ONLY)

        assertFalse(createWarning().shouldShow())
    }

    @Test
    fun doesNotWarnWhenTheCarrierHasNotAskedForIt() {
        // -1 is the documented "not pertinent for this carrier" value.
        setCarrierDelay(delayMillis = -1)

        assertFalse(createWarning().shouldShow())
    }

    @Test
    fun doesNotWarnWhenTheCarrierConfigIsUnavailable() {
        every { telephonyManager.carrierConfig } returns null

        assertFalse(createWarning().shouldShow())
    }

    @Test
    fun doesNotWarnWithoutTheReadPhoneStatePermission() {
        denyReadPhoneState()

        assertFalse(createWarning().shouldShow())
    }

    @Test
    fun doesNotWarnWhenTheServiceStateIsUnreadable() {
        // getServiceState also needs ACCESS_FINE_LOCATION since API 29 and returns null without it.
        // The fragment dereferenced this unguarded, so an unprivileged install crashed here.
        every { telephonyManager.serviceState } returns null

        assertFalse(createWarning().shouldShow())
    }

    @Test
    fun doesNotWarnWhenReadingTheServiceStateThrows() {
        every { telephonyManager.serviceState } throws SecurityException("no location permission")

        assertFalse(createWarning().shouldShow())
    }

    @Test
    fun doesNotWarnOnAnUnrecognisedServiceState() {
        // The fragment threw AssertionError here, taking the app down on a state it did not know.
        setServiceState(Int.MAX_VALUE)

        assertFalse(createWarning().shouldShow())
    }

    private fun createWarning() = SystemEmergencyCallWarning(
        context = context,
        telephonyManager = telephonyManager,
    )

    private fun setCarrierDelay(delayMillis: Int) {
        val config = PersistableBundle().apply {
            putInt(EMERGENCY_NOTIFICATION_DELAY_KEY, delayMillis)
        }
        every { telephonyManager.carrierConfig } returns config
    }

    private fun setServiceState(state: Int) {
        val serviceState = ServiceState().apply { setState(state) }
        every { telephonyManager.serviceState } returns serviceState
    }

    private fun grantReadPhoneState() {
        shadowOf(context as android.app.Application)
            .grantPermissions(Manifest.permission.READ_PHONE_STATE)
    }

    private fun denyReadPhoneState() {
        shadowOf(context as android.app.Application)
            .denyPermissions(Manifest.permission.READ_PHONE_STATE)
    }
}
