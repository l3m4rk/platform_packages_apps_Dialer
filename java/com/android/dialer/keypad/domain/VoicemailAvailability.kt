package com.android.dialer.keypad.domain

import android.content.Context
import android.provider.Settings
import android.telecom.PhoneAccount
import android.telephony.TelephonyManager
import com.android.dialer.common.LogUtil
import com.android.dialer.telecom.TelecomUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface VoicemailAvailability {

    fun isVoicemailReachable(): Boolean

    fun isAirplaneModeOn(): Boolean
}

internal class SystemVoicemailAvailability @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager,
) : VoicemailAvailability {

    override fun isVoicemailReachable(): Boolean =
        hasMultipleSimsWithoutDefault() || hasVoicemailNumber()

    override fun isAirplaneModeOn(): Boolean =
        Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0,
        ) != 0

    // Reachable without a known number: Telecom then asks which SIM to call with.
    private fun hasMultipleSimsWithoutDefault(): Boolean = try {
        val accounts = TelecomUtil.getSubscriptionPhoneAccounts(context)
        accounts.size > 1 && defaultVoicemailAccount() !in accounts
    } catch (e: SecurityException) {
        LogUtil.w(TAG, "Cannot read the subscription phone accounts: $e")
        false
    }

    private fun hasVoicemailNumber(): Boolean = try {
        val number = when (val account = defaultVoicemailAccount()) {
            null -> telephonyManager.voiceMailNumber
            else -> TelecomUtil.getVoicemailNumber(context, account)
        }
        !number.isNullOrEmpty()
    } catch (e: SecurityException) {
        LogUtil.w(TAG, "Cannot read the voicemail number: $e")
        false
    }

    private fun defaultVoicemailAccount() =
        TelecomUtil.getDefaultOutgoingPhoneAccount(context, PhoneAccount.SCHEME_VOICEMAIL)

    private companion object {
        private const val TAG = "VoicemailAvailability"
    }
}
