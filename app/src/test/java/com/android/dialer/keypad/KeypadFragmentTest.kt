package com.android.dialer.keypad

import android.os.Build
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
class KeypadFragmentTest {

    @Test
    fun clearingBeforeTheFragmentIsAttachedDoesNothing() {
        // The host holds the fragment from before its asynchronous commit attaches it, and may
        // clear it in that gap. Without the guard this throws IllegalStateException from
        // requireActivity().
        KeypadFragment().clearDialpad()
    }
}
