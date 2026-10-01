package com.android.dialer.keypad.domain

import android.content.Context
import android.provider.CallLog.Calls
import com.android.dialer.di.core.IoDispatcher
import com.android.dialer.util.PermissionsUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface LastOutgoingCall {
    suspend operator fun invoke(): String?
}

internal class SystemLastOutgoingCall @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LastOutgoingCall {

    override suspend fun invoke(): String? = withContext(ioDispatcher) {
        when {
            !PermissionsUtil.hasCallLogReadPermissions(context) -> null
            // Documented to return "", but returns null when the log cannot be queried.
            else -> Calls.getLastOutgoingCall(context)?.takeIf { it.isNotEmpty() }
        }
    }
}
