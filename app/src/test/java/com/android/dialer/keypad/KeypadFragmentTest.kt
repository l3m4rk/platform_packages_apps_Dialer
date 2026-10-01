package com.android.dialer.keypad

import android.content.Intent
import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun theInCallScreensAddCallOpensAnEmptyKeypad() {
        assertTrue(KeypadFragment.isAddCallMode(addCall(Intent.ACTION_DIAL)))
        assertTrue(KeypadFragment.isAddCallMode(addCall(Intent.ACTION_VIEW)))
    }

    @Test
    fun anOrdinaryDialIntentIsNotAddCall() {
        assertFalse(KeypadFragment.isAddCallMode(Intent(Intent.ACTION_DIAL)))
        assertFalse(KeypadFragment.isAddCallMode(Intent(Intent.ACTION_DIAL).putExtra(EXTRA, false)))
    }

    @Test
    fun onlyDialAndViewCanBeAddCall() {
        assertFalse(KeypadFragment.isAddCallMode(addCall(Intent.ACTION_CALL)))
        assertFalse(KeypadFragment.isAddCallMode(null))
    }

    private fun addCall(action: String) = Intent(action).putExtra(EXTRA, true)

    private companion object {
        // The extra Telecom sets; spelled out so that a rename on our side shows up here.
        private const val EXTRA = "add_call_mode"
    }
}
