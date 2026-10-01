package com.android.dialer.keypad.ui

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// Search results keep the start 40% in landscape.
private const val LANDSCAPE_SPACER_WEIGHT = 4f
private const val LANDSCAPE_KEYPAD_WEIGHT = 6f

@Composable
internal fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/** The language's direction, for what the keypad's left-to-right pin must not affect. */
@Composable
internal fun localeLayoutDirection(): LayoutDirection =
    when (LocalConfiguration.current.layoutDirection) {
        View.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }

// The rest stays empty, so touches fall through to the search results underneath.
@Composable
internal fun KeypadPlacement(
    modifier: Modifier = Modifier,
    keypad: @Composable (Modifier) -> Unit,
) {
    if (isLandscape()) {
        // A Row, so a right-to-left locale moves the keypad to the left, where it slides in from.
        Row(modifier = modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(LANDSCAPE_SPACER_WEIGHT))
            keypad(Modifier.weight(LANDSCAPE_KEYPAD_WEIGHT).fillMaxHeight())
        }
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            keypad(Modifier)
        }
    }
}
