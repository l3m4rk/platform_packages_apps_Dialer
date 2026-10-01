package com.android.dialer.keypad.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.PhoneAccount
import android.telephony.PhoneNumberUtils
import com.android.dialer.common.LogUtil
import com.android.dialer.di.core.IoDispatcher
import com.android.dialer.location.GeoUtil
import com.android.dialer.phonenumberutil.PhoneNumberHelper
import com.android.dialer.util.PermissionsUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface DialIntentNumber {
    suspend operator fun invoke(intent: Intent): String?
}

/** Reads a `tel:` link, or a legacy Contacts item the manifest still accepts. */
internal class SystemDialIntentNumber @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DialIntentNumber {

    override suspend fun invoke(intent: Intent): String? = withContext(ioDispatcher) {
        val uri = intent.data
        when {
            intent.action != Intent.ACTION_DIAL && intent.action != Intent.ACTION_VIEW -> null
            uri == null -> null
            uri.scheme == PhoneAccount.SCHEME_TEL -> numberFromTelUri(uri)
            intent.type !in CONTACT_ITEM_TYPES -> null
            !PermissionsUtil.hasContactsReadPermissions(context) -> null
            else -> numberFromContact(uri)
        }?.takeIf { number -> number.isNotEmpty() }
    }

    private fun numberFromTelUri(uri: Uri): String {
        val converted = PhoneNumberUtils.convertKeypadLettersToDigits(
            PhoneNumberUtils.replaceUnicodeDigits(uri.schemeSpecificPart.orEmpty()),
        )
        return format(dialString = converted, normalizedNumber = null)
    }

    // Another app's URI may point at a provider this app cannot read, or at nothing.
    private fun numberFromContact(uri: Uri): String? = try {
        context.contentResolver
            .query(uri, arrayOf(COLUMN_NUMBER, COLUMN_NUMBER_KEY), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    format(dialString = cursor.getString(0), normalizedNumber = cursor.getString(1))
                } else {
                    null
                }
            }
    } catch (e: SecurityException) {
        LogUtil.w(TAG, "Cannot read the contact: $e")
        null
    } catch (e: IllegalArgumentException) {
        LogUtil.w(TAG, "Cannot read the contact: $e")
        null
    }

    // Keeps any pause or wait suffix as typed.
    private fun format(dialString: String?, normalizedNumber: String?): String {
        val number = PhoneNumberUtils.extractNetworkPortion(dialString)
        val postDial = PhoneNumberUtils.extractPostDialPortion(dialString).orEmpty()
        return when {
            number.isNullOrEmpty() -> postDial
            else -> PhoneNumberHelper.formatNumber(
                context,
                number,
                normalizedNumber,
                GeoUtil.getCurrentCountryIso(context),
            ) + postDial
        }
    }

    private companion object {
        private const val TAG = "SystemDialIntentNumber"

        // The deprecated android.provider.Contacts types the DIAL intent filter declares.
        private val CONTACT_ITEM_TYPES = setOf(
            "vnd.android.cursor.item/person",
            "vnd.android.cursor.item/phone",
        )
        private const val COLUMN_NUMBER = "number"
        private const val COLUMN_NUMBER_KEY = "number_key"
    }
}
