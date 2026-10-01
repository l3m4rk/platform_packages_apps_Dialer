package com.android.dialer.keypad.domain

import android.content.Context
import com.android.dialer.common.LogUtil
import com.android.dialer.di.core.IoDispatcher
import com.android.dialer.util.CallUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface CallWithNoteAvailability {
    suspend fun isAvailable(): Boolean
}

internal class SystemCallWithNoteAvailability @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CallWithNoteAvailability {

    // getPhoneAccount also needs READ_PHONE_NUMBERS, and throws without it.
    override suspend fun isAvailable(): Boolean = withContext(ioDispatcher) {
        try {
            CallUtil.isCallWithSubjectSupported(context)
        } catch (e: SecurityException) {
            LogUtil.w(TAG, "Cannot read the phone accounts: $e")
            false
        }
    }

    private companion object {
        private const val TAG = "CallWithNoteAvailability"
    }
}
