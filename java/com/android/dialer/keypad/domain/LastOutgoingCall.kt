package com.android.dialer.keypad.domain

import android.content.Context
import android.provider.CallLog.Calls
import com.android.dialer.di.core.IoDispatcher
import com.android.dialer.util.PermissionsUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The number the user last dialed, or `null` if there is none or the call log cannot be read.
 *
 * Pressing the call button with an empty field recalls this number into the field instead of
 * placing a call.
 */
internal interface LastOutgoingCall {
    suspend operator fun invoke(): String?
}

/**
 * Reads the number from the call log.
 *
 * In the fragment this was not a service at all: it was a `DialpadListener.getLastOutgoingCall`
 * callback that the activity answered from a background executor. Owning the query here removes
 * a method from the host contract and puts the permission check next to the query it guards.
 */
internal class SystemLastOutgoingCall @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LastOutgoingCall {

    override suspend fun invoke(): String? = withContext(ioDispatcher) {
        when {
            !PermissionsUtil.hasCallLogReadPermissions(context) -> null
            // Documented to return an empty string when the log holds no outgoing call, but it is
            // a platform type and does hand back null when the call log cannot be queried at all.
            else -> Calls.getLastOutgoingCall(context)?.takeIf { it.isNotEmpty() }
        }
    }
}
