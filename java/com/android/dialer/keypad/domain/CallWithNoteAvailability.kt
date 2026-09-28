package com.android.dialer.keypad.domain

import android.content.Context
import com.android.dialer.common.LogUtil
import com.android.dialer.util.CallUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Whether the overflow menu offers "Call with a note": some call-capable account has to support a
 * call subject, which only some carriers do.
 */
internal fun interface CallWithNoteAvailability {
    fun isAvailable(): Boolean
}

internal class SystemCallWithNoteAvailability @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CallWithNoteAvailability {

    /**
     * `CallUtil` checks `READ_PHONE_STATE` itself, but then reads each account through
     * `TelecomManager.getPhoneAccount`, which also needs `READ_PHONE_NUMBERS` and throws without
     * it. The fragment called it unguarded each time the menu opened; now the item just hides.
     */
    override fun isAvailable(): Boolean = try {
        CallUtil.isCallWithSubjectSupported(context)
    } catch (e: SecurityException) {
        LogUtil.w(TAG, "Cannot read the phone accounts: $e")
        false
    }

    private companion object {
        private const val TAG = "CallWithNoteAvailability"
    }
}
