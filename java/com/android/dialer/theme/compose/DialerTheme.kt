package com.android.dialer.theme.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * The Material 3 shape scale for Dialer's Compose screens.
 *
 * The corner sizes are Material 3 Expressive's, mapped onto the classic five-slot [Shapes]:
 * `medium` carries the expressive `large-increased` token (20dp) and `extraLarge` carries
 * `extra-large-increased` (32dp).
 *
 * material3 1.4.0 keeps `MaterialExpressiveTheme`, `MotionScheme` and the extra corner tokens
 * `internal` — they are public only in 1.5.0-alphaNN, which this fork does not depend on.
 * Carrying the token values here gets the expressive shape language without an alpha dependency
 * and without regenerating gradle/verification-metadata.xml. Replace this with
 * `MaterialExpressiveTheme` once a stable material3 exposes it.
 */
private val DialerShapes = Shapes(
    extraSmall = RoundedCornerShape(size = 12.dp),
    small = RoundedCornerShape(size = 16.dp),
    medium = RoundedCornerShape(size = 20.dp),
    large = RoundedCornerShape(size = 28.dp),
    extraLarge = RoundedCornerShape(size = 32.dp),
)

/**
 * Wraps [content] in Dialer's Compose theme: the platform's dynamic color scheme for the current
 * light/dark setting, plus [DialerShapes].
 *
 * Typography stays at the material3 default so text keeps following the user's font scale. The XML
 * themes under `com/android/dialer/theme` are untouched and continue to style every View-based
 * screen; this theme applies only inside a `ComposeView`.
 */
@Composable
internal fun DialerTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = when {
        isSystemInDarkTheme() -> dynamicDarkColorScheme(context = context)
        else -> dynamicLightColorScheme(context = context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = DialerShapes,
        content = content,
    )
}
