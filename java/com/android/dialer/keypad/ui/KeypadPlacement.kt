package com.android.dialer.keypad.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration

// The legacy landscape split: search results keep the start 40%, the keypad takes the rest.
private const val LANDSCAPE_SPACER_WEIGHT = 4f
private const val LANDSCAPE_KEYPAD_WEIGHT = 6f

/** The keypad goes beside the search results in landscape, and below them otherwise. */
@Composable
internal fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * Places [keypad] within the host's full-screen container: along the bottom in portrait, and down
 * the end side in landscape, as `dialpad_fragment.xml` and its `layout-land` variant did.
 *
 * The rest of the container is left empty. Empty space handles no touches, so they fall through
 * to the search results underneath, which is what the host relies on.
 */
@Composable
internal fun KeypadPlacement(
    modifier: Modifier = Modifier,
    keypad: @Composable (Modifier) -> Unit,
) {
    if (isLandscape()) {
        // A Row, so that in a right-to-left locale the keypad moves to the left, as the legacy
        // LinearLayout did; its slide-in animation already comes from that side.
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
