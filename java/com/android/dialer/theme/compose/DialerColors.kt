package com.android.dialer.theme.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Fixed colors that deliberately ignore the dynamic color scheme.
 *
 * The call button stays green whatever the wallpaper, because people find it by color as much as
 * by position. It does not reuse `@color/dialer_call_green` (#00C853): white on that is about 2.2:1,
 * well short of the 4.5:1 WCAG asks of a text label, and the Compose call button carries the word
 * "Call" rather than only an icon. This is Google's green 700, at 5.0:1.
 *
 * In the dark theme the button turns light with dark content, as Google's Phone app's does: green
 * 300 with grey 900, at about 9:1.
 */
internal object DialerColors {
    val CallContainer = Color(0xFF188038)
    val OnCallContainer = Color.White

    val CallContainerDark = Color(0xFF81C995)
    val OnCallContainerDark = Color(0xFF202124)

    /** The call button's green for the current theme. */
    @Composable
    @ReadOnlyComposable
    fun callContainer(): Color = if (isSystemInDarkTheme()) CallContainerDark else CallContainer

    /** The call button's icon and label color for the current theme. */
    @Composable
    @ReadOnlyComposable
    fun onCallContainer(): Color =
        if (isSystemInDarkTheme()) OnCallContainerDark else OnCallContainer
}
