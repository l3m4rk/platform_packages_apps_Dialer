package com.android.dialer.theme.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Wraps a preview in [DialerTheme] on a themed background, so dark previews are legible. */
@Composable
internal fun DialerPreviewTheme(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DialerTheme {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
