package com.android.dialer.keypad.domain

import android.content.Context
import android.provider.Settings
import android.telecom.PhoneAccount
import android.telephony.TelephonyManager
import com.android.dialer.common.LogUtil
import com.android.dialer.telecom.TelecomUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Answers the two questions a long press on the 1 key asks.
 *
 * The keypad, not this class, decides what to do with the answers: call voicemail if it is
 * reachable, otherwise show the airplane-mode error or the not-ready error.
 */
internal interface VoicemailAvailability {

    /** Whether long-pressing 1 should place a voicemail call. */
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
            // The fragment read this from Settings.System, where it was relocated away from years
            // ago and only still resolves through a compatibility shim.
            Settings.Global.AIRPLANE_MODE_ON,
            0,
        ) != 0

    /**
     * Counts as reachable on its own: the call is placed even with no known voicemail number, so
     * that Telecom can show its "Call with" picker rather than the keypad guessing which SIM to use.
     */
    private fun hasMultipleSimsWithoutDefault(): Boolean = try {
        val accounts = TelecomUtil.getSubscriptionPhoneAccounts(context)
        accounts.size > 1 && defaultVoicemailAccount() !in accounts
    } catch (e: SecurityException) {
        LogUtil.w(TAG, "Cannot read the subscription phone accounts: $e")
        false
    }

    private fun hasVoicemailNumber(): Boolean = try {
        val number = when (val account = defaultVoicemailAccount()) {
            // A single-SIM phone has no default outgoing account, so ask telephony directly.
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
