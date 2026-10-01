package com.android.dialer.theme.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The call button stays green whatever the wallpaper. Not `@color/dialer_call_green`: white text on
 * it is 2.2:1, short of WCAG's 4.5:1 for a label; green 700 is 5.0:1.
 */
internal object DialerColors {
    val CallContainer = Color(0xFF188038)
    val OnCallContainer = Color.White

    val CallContainerDark = Color(0xFF81C995)
    val OnCallContainerDark = Color(0xFF202124)

    @Composable
    @ReadOnlyComposable
    fun callContainer(): Color = if (isSystemInDarkTheme()) CallContainerDark else CallContainer

    @Composable
    @ReadOnlyComposable
    fun onCallContainer(): Color =
        if (isSystemInDarkTheme()) OnCallContainerDark else OnCallContainer
}
