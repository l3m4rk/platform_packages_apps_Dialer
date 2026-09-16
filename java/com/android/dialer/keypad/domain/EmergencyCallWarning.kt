package com.android.dialer.keypad.domain

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import com.android.dialer.common.LogUtil
import com.android.dialer.util.PermissionsUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Whether to show the "can't make emergency calls over wifi" hint above the keypad.
 *
 * Only some carriers ask for it, and only while the device has no cellular service.
 */
internal fun interface EmergencyCallWarning {
    fun shouldShow(): Boolean
}

internal class SystemEmergencyCallWarning @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager,
) : EmergencyCallWarning {

    override fun shouldShow(): Boolean {
        if (!PermissionsUtil.hasReadPhoneStatePermissions(context)) {
            return false
        }
        return isRequiredByCarrier() && isWithoutService()
    }

    @SuppressLint("MissingPermission")
    private fun isRequiredByCarrier(): Boolean {
        val config = telephonyManager.carrierConfig
        return config != null &&
            config.getInt(KEY_EMERGENCY_NOTIFICATION_DELAY_INT, NO_DELAY) != NO_DELAY
    }

    /**
     * Emergency calling is unavailable without service whatever the wifi state, so the service
     * state alone decides this. The wifi checks the platform offers are hidden API, and
     * `getVoiceNetworkType` does not reliably report `NETWORK_TYPE_IWLAN` over wifi.
     *
     * Two departures from the fragment, both of which crashed an unprivileged install. Since API 29
     * `getServiceState` returns null without `ACCESS_FINE_LOCATION` and can throw, and the keypad
     * calls this on every change to an empty digits field, so a null was a guaranteed NPE on the
     * first keystroke. The fragment also threw `AssertionError` on any state outside the four it
     * knew; an unrecognized state now simply means no warning.
     */
    private fun isWithoutService(): Boolean {
        val state = try {
            telephonyManager.serviceState?.state
        } catch (e: SecurityException) {
            LogUtil.w(TAG, "Cannot read the service state: $e")
            null
        }
        return state == ServiceState.STATE_OUT_OF_SERVICE || state == ServiceState.STATE_POWER_OFF
    }

    private companion object {
        private const val TAG = "EmergencyCallWarning"

        /**
         * Hidden carrier config key: how long to wait before warning during a wifi call. A value of
         * [NO_DELAY] means the carrier does not want the warning at all.
         */
        private const val KEY_EMERGENCY_NOTIFICATION_DELAY_INT = "emergency_notification_delay_int"
        private const val NO_DELAY = -1
    }
}
