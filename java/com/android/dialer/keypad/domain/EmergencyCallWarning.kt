package com.android.dialer.keypad.domain

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import com.android.dialer.common.LogUtil
import com.android.dialer.util.PermissionsUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The "can't make emergency calls over wifi" hint: only some carriers want it, without service. */
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
     * Without `ACCESS_FINE_LOCATION`, `getServiceState` returns null or throws, which crashed an
     * unprivileged install on its first keystroke; an unknown state now means no warning.
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

        /** Hidden carrier config key; [NO_DELAY] means the carrier wants no warning. */
        private const val KEY_EMERGENCY_NOTIFICATION_DELAY_INT = "emergency_notification_delay_int"
        private const val NO_DELAY = -1
    }
}
