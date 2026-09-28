package com.android.dialer.keypad.ui

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.testutil.composeActivityRule
import com.android.dialer.theme.compose.DialerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A landscape phone, the height that used to push the call button off the screen. */
@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    sdk = [Build.VERSION_CODES.BAKLAVA],
    qualifiers = "w891dp-h411dp-land",
)
class KeypadPlacementTest {

    @get:Rule(order = 0)
    val componentActivityRule = composeActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun everythingFitsOnTheScreenInLandscape() {
        render()
        val screen = composeRule.onRoot().getBoundsInRoot()

        val tags = KeypadKey.entries.map(::keypadKeyTestTag) +
            listOf(KEYPAD_DIGITS_TEST_TAG, KEYPAD_DELETE_TEST_TAG, KEYPAD_CALL_TEST_TAG)
        // Unclipped: the clipped bounds of something pushed off the screen would still look inside.
        tags.forEach { tag ->
            assertInside(tag, composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot(), screen)
        }
    }

    @Test
    fun nothingIsSqueezedToFitInLandscape() {
        render()

        // Out of room, a column does not push its last children off the screen; it squeezes them,
        // down to a call button of no height at all. Being inside the screen proves nothing then.
        KeypadKey.entries.forEach { key ->
            assertAtLeast(keypadKeyTestTag(key), MIN_KEY_HEIGHT)
        }
        assertAtLeast(KEYPAD_CALL_TEST_TAG, MIN_TOUCH_TARGET)
    }

    @Test
    fun theKeypadTakesTheEndSixTenthsInLandscape() {
        render()
        val screen = composeRule.onRoot().getBoundsInRoot()
        val sheet = composeRule.onNodeWithTag(KEYPAD_SHEET_TEST_TAG).getBoundsInRoot()

        assertDp(screen.left + (screen.right - screen.left) * 0.4f, sheet.left)
        assertDp(screen.right, sheet.right)
        assertDp(screen.top, sheet.top)
        assertDp(screen.bottom, sheet.bottom)
    }

    @Test
    fun theKeypadMovesToTheLeftInARightToLeftLocale() {
        render(layoutDirection = LayoutDirection.Rtl)
        val screen = composeRule.onRoot().getBoundsInRoot()
        val sheet = composeRule.onNodeWithTag(KEYPAD_SHEET_TEST_TAG).getBoundsInRoot()

        assertDp(screen.left, sheet.left)
        assertDp(screen.left + (screen.right - screen.left) * 0.6f, sheet.right)
    }

    @Test
    fun theKeysStillReadLeftToRightInARightToLeftLocale() {
        render(layoutDirection = LayoutDirection.Rtl)

        assertLeftToRight(KeypadKey.ONE, KeypadKey.TWO, KeypadKey.THREE)
        assertLeftToRight(KeypadKey.STAR, KeypadKey.ZERO, KeypadKey.POUND)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun theKeysStillReadLeftToRightInARightToLeftLocaleInPortrait() {
        render(layoutDirection = LayoutDirection.Rtl)

        assertLeftToRight(KeypadKey.ONE, KeypadKey.TWO, KeypadKey.THREE)
    }

    @Test
    fun backspaceStaysOnTheRightInARightToLeftLocale() {
        render(layoutDirection = LayoutDirection.Rtl)

        val digits = composeRule.onNodeWithTag(KEYPAD_DIGITS_TEST_TAG).getBoundsInRoot()
        val delete = composeRule.onNodeWithTag(KEYPAD_DELETE_TEST_TAG).getBoundsInRoot()
        val overflow = composeRule.onNodeWithTag(KEYPAD_OVERFLOW_TEST_TAG).getBoundsInRoot()

        assertTrue("backspace right of the number", delete.left >= digits.right)
        assertTrue("overflow left of the number", overflow.right <= digits.left)
    }

    @Test
    @Config(qualifiers = "ar-ldrtl-w411dp-h891dp-port")
    fun theCallButtonFollowsTheLanguageInARightToLeftLocale() {
        render(layoutDirection = LayoutDirection.Rtl)

        // The icon leads, so on the right; the label follows to its left.
        assertTrue("label left of centre", callLabelCentre() < callButtonCentre())
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun theCallButtonLeadsWithItsIconInALeftToRightLocale() {
        render()

        assertTrue("label right of centre", callLabelCentre() > callButtonCentre())
    }

    @Test
    fun landscapeKeysPutTheLettersBesideTheDigit() {
        render()
        val key = keypadKeyTestTag(KeypadKey.TWO)

        val digit = textIn(key, "2").getBoundsInRoot()
        val letters = textIn(key, "ABC").getBoundsInRoot()

        assertTrue("letters start after the digit", letters.left >= digit.right)
        assertDp(
            expected = (digit.top + digit.bottom) / 2,
            actual = (letters.top + letters.bottom) / 2,
            tolerance = 4.dp,
        )
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun theKeypadSpansTheBottomInPortrait() {
        render()
        val screen = composeRule.onRoot().getBoundsInRoot()
        val sheet = composeRule.onNodeWithTag(KEYPAD_SHEET_TEST_TAG).getBoundsInRoot()

        assertDp(screen.left, sheet.left)
        assertDp(screen.right, sheet.right)
        assertDp(screen.bottom, sheet.bottom)
        assertTrue("leaves room above for search results", sheet.top > screen.top)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun portraitKeysPutTheLettersUnderTheDigit() {
        render()
        val key = keypadKeyTestTag(KeypadKey.TWO)

        val digit = textIn(key, "2").getBoundsInRoot()
        val letters = textIn(key, "ABC").getBoundsInRoot()

        assertTrue("letters below the digit", letters.top >= digit.bottom)
    }

    private fun render(layoutDirection: LayoutDirection = LayoutDirection.Ltr) {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                DialerTheme {
                    KeypadPlacement { placement ->
                        KeypadScreen(
                            uiState = KeypadUiState(
                                digits = "650-253-0000",
                                isDeleteEnabled = true,
                                isOverflowVisible = true,
                            ),
                            strings = STRINGS,
                            onAction = {},
                            modifier = placement,
                        )
                    }
                }
            }
        }
    }

    private fun callButtonCentre(): Dp = composeRule
        .onNodeWithTag(KEYPAD_CALL_TEST_TAG)
        .getBoundsInRoot()
        .let { (it.left + it.right) / 2 }

    private fun callLabelCentre(): Dp = composeRule
        .onNode(
            matcher = hasText(STRINGS.call) and hasAnyAncestor(hasTestTag(KEYPAD_CALL_TEST_TAG)),
            useUnmergedTree = true,
        )
        .getBoundsInRoot()
        .let { (it.left + it.right) / 2 }

    private fun assertLeftToRight(vararg keys: KeypadKey) {
        keys.toList().zipWithNext().forEach { (left, right) ->
            val leftBounds = composeRule.onNodeWithTag(keypadKeyTestTag(left)).getBoundsInRoot()
            val rightBounds = composeRule.onNodeWithTag(keypadKeyTestTag(right)).getBoundsInRoot()
            assertTrue("$left left of $right", leftBounds.right <= rightBounds.left)
        }
    }

    private fun textIn(keyTag: String, text: String): SemanticsNodeInteraction =
        composeRule.onNode(
            matcher = hasText(text) and hasAnyAncestor(hasTestTag(keyTag)),
            useUnmergedTree = true,
        )

    private fun assertAtLeast(tag: String, height: Dp) {
        val bounds = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        val actual = bounds.bottom - bounds.top
        assertTrue("$tag is $actual tall, under $height", actual >= height - TOLERANCE)
    }

    private fun assertInside(what: String, inner: DpRect, outer: DpRect) {
        assertTrue(
            "$what at $inner is not inside $outer",
            inner.left >= outer.left - TOLERANCE &&
                inner.top >= outer.top - TOLERANCE &&
                inner.right <= outer.right + TOLERANCE &&
                inner.bottom <= outer.bottom + TOLERANCE,
        )
    }

    private fun assertDp(
        expected: Dp,
        actual: Dp,
        tolerance: Dp = TOLERANCE,
    ) {
        assertEquals(expected.value, actual.value, tolerance.value)
    }

    private companion object {
        private val TOLERANCE = 1.dp
        private val MIN_KEY_HEIGHT = 40.dp
        private val MIN_TOUCH_TARGET = 48.dp

        private val STRINGS = KeypadStrings(
            voicemailKeyAction = "call voicemail",
            plusKeyAction = "dial plus",
            deleteButton = "backspace",
            overflowButton = "More options",
            call = "Call",
            emergencyCallWarning = "no emergency calls over wifi",
            addPause = "Add 2-sec pause",
            addWait = "Add wait",
            callWithNote = "Call with a note",
        )
    }
}
