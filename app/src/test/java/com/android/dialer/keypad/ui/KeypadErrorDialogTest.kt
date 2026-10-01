package com.android.dialer.keypad.ui

import android.os.Build
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.testutil.composeActivityRule
import com.android.dialer.theme.compose.DialerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Only the absence of the dialog runs here. Material 3's AlertDialog reads a string resource of its
 * own when it opens, and unit tests have no resource table; opening it, each message and OK are
 * covered by the instrumented tests, like the overflow menu.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA], qualifiers = "w411dp-h891dp")
class KeypadErrorDialogTest {

    @get:Rule(order = 0)
    val componentActivityRule = composeActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun noDialogWithoutAnError() {
        render()

        composeRule.onNodeWithTag(KEYPAD_ERROR_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    private fun render() {
        composeRule.setContent {
            DialerTheme {
                KeypadScreen(
                    uiState = KeypadUiState(),
                    strings = TEST_KEYPAD_STRINGS,
                    onAction = {},
                )
            }
        }
    }
}
