package com.android.dialer.keypad.ui

import android.os.Build
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.testutil.composeActivityRule
import com.android.dialer.theme.compose.DialerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The key's press feedback: its ripple and pressed shape both follow these interactions, which
 * detectTapGestures does not report by itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
class KeypadKeyButtonTest {

    @get:Rule(order = 0)
    val componentActivityRule = composeActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val interactionSource = MutableInteractionSource()
    private val interactions = mutableListOf<Interaction>()
    private var released = 0

    @Test
    fun aTapIsReportedAsAPressThenARelease() {
        render()

        composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.FIVE)).performClick()
        composeRule.waitForIdle()

        assertEquals(2, interactions.size)
        val press = interactions[0] as PressInteraction.Press
        val release = interactions[1] as PressInteraction.Release
        assertSame("the release ends that press", press, release.press)
    }

    @Test
    fun aFingerSlidingOffIsReportedAsACancelAndStillReleasesTheKey() {
        render()

        composeRule.onNodeWithTag(keypadKeyTestTag(KeypadKey.FIVE)).performTouchInput {
            down(center)
            moveTo(center.copy(x = center.x + width * 3))
            up()
        }
        composeRule.waitForIdle()

        assertTrue("$interactions", interactions.last() is PressInteraction.Cancel)
        // The tone must stop either way.
        assertEquals(1, released)
    }

    private fun render() {
        composeRule.setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            DialerTheme {
                KeypadKeyButton(
                    key = KeypadKey.FIVE,
                    onPress = {},
                    onRelease = { released++ },
                    interactionSource = interactionSource,
                )
            }
        }
    }
}
