package com.android.dialer.theme.compose

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.android.dialer.testutil.composeActivityRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
class DialerThemeTest {

    @get:Rule(order = 0)
    val componentActivityRule = composeActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun themeUsesExpressiveCornerSizes() {
        var shapes: Shapes? = null

        composeRule.setContent {
            DialerTheme { shapes = MaterialTheme.shapes }
        }

        composeRule.runOnIdle {
            val actual = requireNotNull(shapes)
            assertEquals(RoundedCornerShape(size = 12.dp), actual.extraSmall)
            assertEquals(RoundedCornerShape(size = 16.dp), actual.small)
            assertEquals(RoundedCornerShape(size = 20.dp), actual.medium)
            assertEquals(RoundedCornerShape(size = 28.dp), actual.large)
            assertEquals(RoundedCornerShape(size = 32.dp), actual.extraLarge)
        }
    }

    @Test
    fun themeResolvesDynamicColorScheme() {
        var colorScheme: ColorScheme? = null

        composeRule.setContent {
            DialerTheme { colorScheme = MaterialTheme.colorScheme }
        }

        composeRule.runOnIdle {
            val actual = requireNotNull(colorScheme)
            assertNotEquals(Color.Unspecified, actual.surface)
            assertNotEquals(actual.surface, actual.onSurface)
            assertNotEquals(actual.primary, actual.onPrimary)
        }
    }

    @Test
    @Config(qualifiers = "+night")
    fun darkThemeInvertsSurfaceLuminance() {
        var colorScheme: ColorScheme? = null

        composeRule.setContent {
            DialerTheme { colorScheme = MaterialTheme.colorScheme }
        }

        composeRule.runOnIdle {
            val actual = requireNotNull(colorScheme)
            assertTrue(actual.surface.luminance() < actual.onSurface.luminance())
        }
    }

    @Test
    fun themedContentComposes() {
        composeRule.setContent {
            DialerTheme { Text(text = "Dialer") }
        }

        composeRule.onNodeWithText("Dialer").assertIsDisplayed()
    }
}
