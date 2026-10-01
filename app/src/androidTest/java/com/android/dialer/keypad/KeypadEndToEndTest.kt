package com.android.dialer.keypad

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.ui.KEYPAD_CALL_TEST_TAG
import com.android.dialer.keypad.ui.KEYPAD_DELETE_TEST_TAG
import com.android.dialer.keypad.ui.KEYPAD_DIGITS_TEST_TAG
import com.android.dialer.keypad.ui.KEYPAD_SHEET_TEST_TAG
import com.android.dialer.keypad.ui.keypadKeyTestTag
import com.android.dialer.main.impl.MainActivity
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The keypad in the real app: `MainActivity` opened by a dial intent, its fragment, the Hilt view
 * model and the real formatter, driven by touch.
 *
 * It stops short of pressing Call, which would place a real call.
 */
@RunWith(AndroidJUnit4::class)
class KeypadEndToEndTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun closeActivity() {
        scenario?.close()
    }

    @Test
    fun typedDigitsAreFormattedAndBackspaceClearsThem() {
        openKeypad()
        composeRule.onNodeWithTag(KEYPAD_DELETE_TEST_TAG).assertIsNotEnabled()

        type("5551212")

        composeRule.waitUntil(TIMEOUT_MILLIS) { digits().filter(Char::isDigit) == "5551212" }
        // Grouped by the formatter for the device's country; the digits themselves are exact.
        assertNotEquals("5551212", digits())
        composeRule.onNodeWithTag(KEYPAD_CALL_TEST_TAG).assertIsEnabled()

        composeRule.onNodeWithTag(KEYPAD_DELETE_TEST_TAG).performTouchInput { longClick() }

        composeRule.waitUntil(TIMEOUT_MILLIS) { digits().isEmpty() }
        composeRule.onNodeWithTag(KEYPAD_DELETE_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun aDialIntentFillsTheNumber() {
        openKeypad(number = "5551234")

        composeRule.waitUntil(TIMEOUT_MILLIS) { digits().filter(Char::isDigit) == "5551234" }
        composeRule.onNodeWithTag(KEYPAD_DELETE_TEST_TAG).assertIsEnabled()
    }

    private fun openKeypad(number: String? = null) {
        val intent = Intent(Intent.ACTION_DIAL)
            .setClassName(context, MainActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        number?.let { intent.data = Uri.fromParts("tel", it, null) }
        scenario = ActivityScenario.launch(intent)
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(KEYPAD_SHEET_TEST_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    /** Types each key in turn, as a finger would. */
    private fun type(number: String) {
        number.forEach { char ->
            val key = KeypadKey.entries.first { it.char == char }
            composeRule.onNodeWithTag(keypadKeyTestTag(key)).performTouchInput { click() }
        }
    }

    private fun digits(): String = composeRule.onNodeWithTag(KEYPAD_DIGITS_TEST_TAG)
        .fetchSemanticsNode()
        .config
        .getOrNull(SemanticsProperties.EditableText)
        ?.text
        .orEmpty()

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
