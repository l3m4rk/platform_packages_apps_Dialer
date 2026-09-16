package com.android.dialer.keypad.domain

import android.content.Context
import android.text.TextWatcher
import com.android.dialer.di.core.DefaultDispatcher
import com.android.dialer.dialpadview.DialerPhoneNumberFormattingTextWatcher
import com.android.dialer.location.GeoUtil
import com.android.dialer.oem.MotorolaUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Builds the as-you-type formatter for the digits field, or returns `null` where formatting is
 * turned off for the device.
 */
internal interface PhoneNumberFormatting {
    suspend fun createWatcher(): TextWatcher?
}

internal class SystemPhoneNumberFormatting @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : PhoneNumberFormatting {

    override suspend fun createWatcher(): TextWatcher? = when {
        MotorolaUtils.shouldDisablePhoneNumberFormatting(context) -> null
        // Off the main thread deliberately: the watcher builds a libphonenumber AsYouTypeFormatter,
        // which cannot be initialized on it. The fragment used a background executor for the same
        // reason, and attached the watcher only once it was ready.
        else -> withContext(defaultDispatcher) {
            DialerPhoneNumberFormattingTextWatcher(GeoUtil.getCurrentCountryIso(context))
        }
    }
}
