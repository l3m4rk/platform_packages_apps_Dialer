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
 * Material 3 Expressive corner sizes on the classic [Shapes]: `medium` is `large-increased` and
 * `extraLarge` is `extra-large-increased`. material3 1.4.0 keeps the expressive API `internal`;
 * replace this with `MaterialExpressiveTheme` once a stable release exposes it.
 */
private val DialerShapes = Shapes(
    extraSmall = RoundedCornerShape(size = 12.dp),
    small = RoundedCornerShape(size = 16.dp),
    medium = RoundedCornerShape(size = 20.dp),
    large = RoundedCornerShape(size = 28.dp),
    extraLarge = RoundedCornerShape(size = 32.dp),
)

/** Dynamic color and [DialerShapes]; the View screens keep their XML themes. */
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
