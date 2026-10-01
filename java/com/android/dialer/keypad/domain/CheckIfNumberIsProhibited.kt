package com.android.dialer.keypad.domain

import android.content.Context
import com.android.dialer.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** `config_prohibited_phone_number_regexp`, empty here and overlaid by some carriers. */
internal fun interface CheckIfNumberIsProhibited {
    operator fun invoke(number: String): Boolean
}

internal class CheckIfNumberIsProhibitedImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CheckIfNumberIsProhibited {

    private val regexp: String by lazy {
        context.getString(R.string.config_prohibited_phone_number_regexp)
    }

    override operator fun invoke(number: String): Boolean =
        regexp.isNotEmpty() && number.matches(regexp.toRegex())
}
