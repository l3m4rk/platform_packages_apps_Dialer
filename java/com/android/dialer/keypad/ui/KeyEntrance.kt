package com.android.dialer.keypad.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadKey.EIGHT
import com.android.dialer.keypad.model.KeypadKey.FIVE
import com.android.dialer.keypad.model.KeypadKey.FOUR
import com.android.dialer.keypad.model.KeypadKey.NINE
import com.android.dialer.keypad.model.KeypadKey.ONE
import com.android.dialer.keypad.model.KeypadKey.POUND
import com.android.dialer.keypad.model.KeypadKey.SEVEN
import com.android.dialer.keypad.model.KeypadKey.SIX
import com.android.dialer.keypad.model.KeypadKey.STAR
import com.android.dialer.keypad.model.KeypadKey.THREE
import com.android.dialer.keypad.model.KeypadKey.TWO
import com.android.dialer.keypad.model.KeypadKey.ZERO

internal val KEY_ENTRANCE_DISTANCE = 100.dp

/**
 * The keys' staggered entrance, hoisted so the fragment can [play] it when the keypad is shown. A
 * composition recreated later, as on rotation, does not replay an old entrance.
 */
@Stable
internal class KeypadEntranceState {

    internal var plays by mutableIntStateOf(0)
        private set

    fun play() {
        plays++
    }
}

@Composable
internal fun rememberKeypadEntranceState(): KeypadEntranceState = remember { KeypadEntranceState() }

internal enum class KeyEntranceLayout {
    PORTRAIT,

    LANDSCAPE_LTR,

    LANDSCAPE_RTL,
}

internal data class KeyEntranceTiming(
    val delayMs: Int,
    val durationMs: Int,
)

// The legacy timing tables, in 33 ms frames, scaled by 0.66 and 0.8 and truncated as before.
internal fun keyEntranceTiming(key: KeypadKey, layout: KeyEntranceLayout): KeyEntranceTiming {
    val delayFrames = DELAY_FRAMES.getValue(layout).getValue(key)
    val durationFrames = DURATION_FRAMES.getValue(layout).getValue(key)
    return KeyEntranceTiming(
        delayMs = (delayFrames * KEY_FRAME_MS * DELAY_MULTIPLIER).toInt(),
        durationMs = (durationFrames * KEY_FRAME_MS * DURATION_MULTIPLIER).toInt(),
    )
}

@Composable
internal fun keyEntranceLayout(): KeyEntranceLayout = when {
    !isLandscape() -> KeyEntranceLayout.PORTRAIT
    // The locale, not LocalLayoutDirection, which the keys pin to left-to-right.
    localeLayoutDirection() == LayoutDirection.Rtl -> KeyEntranceLayout.LANDSCAPE_RTL
    else -> KeyEntranceLayout.LANDSCAPE_LTR
}

/** Pixels along Y in portrait and X in landscape, held at full distance through the key's delay. */
@Composable
internal fun keyEntranceOffset(
    key: KeypadKey,
    layout: KeyEntranceLayout,
    state: KeypadEntranceState,
): Animatable<Float, AnimationVector1D> {
    val offset = remember { Animatable(0f) }
    val playsAtFirstComposition = remember { state.plays }
    val distance = with(LocalDensity.current) { KEY_ENTRANCE_DISTANCE.toPx() }

    LaunchedEffect(state.plays, layout) {
        if (state.plays == playsAtFirstComposition) {
            return@LaunchedEffect
        }
        val timing = keyEntranceTiming(key, layout)
        offset.snapTo(if (layout == KeyEntranceLayout.LANDSCAPE_RTL) -distance else distance)
        offset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = timing.durationMs,
                delayMillis = timing.delayMs,
                easing = FastOutSlowInEasing,
            ),
        )
    }
    return offset
}

private const val KEY_FRAME_MS = 33
private const val DELAY_MULTIPLIER = 0.66
private const val DURATION_MULTIPLIER = 0.8

private val DELAY_FRAMES = mapOf(
    KeyEntranceLayout.PORTRAIT to mapOf(
        ONE to 1, TWO to 2, THREE to 3,
        FOUR to 4, FIVE to 5, SIX to 6,
        SEVEN to 7, EIGHT to 8, NINE to 9,
        STAR to 10, ZERO to 11, POUND to 11,
    ),
    KeyEntranceLayout.LANDSCAPE_LTR to mapOf(
        ONE to 1, FOUR to 2, SEVEN to 3, STAR to 4,
        TWO to 5, FIVE to 6, EIGHT to 7, ZERO to 8,
        THREE to 9, SIX to 10, NINE to 11, POUND to 11,
    ),
    KeyEntranceLayout.LANDSCAPE_RTL to mapOf(
        THREE to 1, SIX to 2, NINE to 3, POUND to 4,
        TWO to 5, FIVE to 6, EIGHT to 7, ZERO to 8,
        ONE to 9, FOUR to 10, SEVEN to 11, STAR to 11,
    ),
)

private val DURATION_FRAMES = mapOf(
    KeyEntranceLayout.PORTRAIT to mapOf(
        ONE to 10, TWO to 10, THREE to 10,
        FOUR to 10, FIVE to 10, SIX to 10,
        SEVEN to 9, EIGHT to 9, NINE to 9,
        STAR to 8, ZERO to 8, POUND to 8,
    ),
    KeyEntranceLayout.LANDSCAPE_LTR to mapOf(
        ONE to 10, TWO to 9, THREE to 8,
        FOUR to 10, FIVE to 9, SIX to 8,
        SEVEN to 10, EIGHT to 9, NINE to 8,
        STAR to 10, ZERO to 9, POUND to 8,
    ),
    KeyEntranceLayout.LANDSCAPE_RTL to mapOf(
        ONE to 8, TWO to 9, THREE to 10,
        FOUR to 8, FIVE to 9, SIX to 10,
        SEVEN to 8, EIGHT to 9, NINE to 10,
        STAR to 8, ZERO to 9, POUND to 10,
    ),
)
