package com.android.dialer.theme.compose

import androidx.compose.ui.graphics.Color

/**
 * Fixed colors that deliberately ignore the dynamic color scheme.
 *
 * The call button stays green whatever the wallpaper, because people find it by color as much as
 * by position. It does not reuse `@color/dialer_call_green` (#00C853): white on that is about 2.2:1,
 * well short of the 4.5:1 WCAG asks of a text label, and the Compose call button carries the word
 * "Call" rather than only an icon. This is Google's green 700, at 5.0:1.
 */
internal object DialerColors {
    val CallContainer = Color(0xFF188038)
    val OnCallContainer = Color.White
}
