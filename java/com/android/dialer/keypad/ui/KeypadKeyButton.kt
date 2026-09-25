package com.android.dialer.keypad.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.theme.compose.DialerMotion

private val KEY_MIN_HEIGHT = 64.dp

// Half the minimum height, so a key at rest is a full pill, as in Google's Phone app. A press
// squares it off with an expressive spring.
private val KEY_RESTING_CORNER = 32.dp
private val KEY_PRESSED_CORNER = 16.dp
private val VOICEMAIL_GLYPH_SIZE = 18.dp

/**
 * One key of the dialpad.
 *
 * Press and release are reported separately, not as a click: a key starts a DTMF tone when it goes
 * down and stops it when it comes up, so the two edges must always be paired. [onPress] fires on
 * touch-down and [onRelease] on touch-up *or* cancellation, so sliding a finger off the key still
 * stops its tone.
 */
@Composable
internal fun KeypadKeyButton(
    key: KeypadKey,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    longPressLabel: String? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) KEY_PRESSED_CORNER else KEY_RESTING_CORNER,
        animationSpec = DialerMotion.fastSpatial(),
        label = "keyCorner",
    )

    Surface(
        modifier = modifier
            .testTag(keypadKeyTestTag(key))
            .heightIn(min = KEY_MIN_HEIGHT)
            .keyPressGestures(
                key = key,
                onPress = onPress,
                onRelease = onRelease,
                onLongPress = onLongPress,
            )
            .keySemantics(
                key = key,
                onPress = onPress,
                onRelease = onRelease,
                longPressLabel = longPressLabel,
                onLongPress = onLongPress,
            ),
        shape = RoundedCornerShape(corner),
        // Brighter than the sheet in both light and dark themes, so the keys read as raised.
        color = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = key.char.toString(),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            KeySubtitle(key = key)
        }
    }
}

/** The voicemail glyph under 1, the letters under 2-9 and the `+` under 0; nothing for `*` or `#`. */
@Composable
private fun KeySubtitle(key: KeypadKey) {
    when {
        key == KeypadKey.ONE -> Icon(
            imageVector = Icons.Rounded.Voicemail,
            // Described by the key's long-press label instead; announcing an icon here would only
            // repeat it.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(VOICEMAIL_GLYPH_SIZE),
        )
        key.letters.isNotEmpty() -> Text(
            text = key.letters,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Modifier.keyPressGestures(
    key: KeypadKey,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onLongPress: (() -> Unit)?,
): Modifier = pointerInput(key, onLongPress) {
    detectTapGestures(
        onPress = {
            onPress()
            // Returns false when the gesture is canceled rather than released — a finger sliding
            // off the key. Either way the tone has to stop, so the result is deliberately ignored.
            tryAwaitRelease()
            onRelease()
        },
        onLongPress = onLongPress?.let { { _ -> it() } },
    )
}

/**
 * Explicit semantics, because [detectTapGestures] contributes none.
 *
 * An accessibility activation has to produce *both* edges, or the tone it starts is never stopped.
 * The description reads the digit, pauses, then spells the letters out one by one rather than
 * pronouncing "abc" as a word.
 */
private fun Modifier.keySemantics(
    key: KeypadKey,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    longPressLabel: String?,
    onLongPress: (() -> Unit)?,
): Modifier = semantics(mergeDescendants = true) {
    contentDescription = keyContentDescription(key)
    onClick {
        onPress()
        onRelease()
        true
    }
    if (onLongPress != null) {
        onLongClick(label = longPressLabel) {
            onLongPress()
            true
        }
    }
}

/**
 * The digit, a pause, then the letters separated so a screen reader spells them rather than
 * pronouncing "abc" as a word.
 *
 * The View keypad achieved the spelling with a `VerbatimTtsSpan` over the letters. Compose's
 * `contentDescription` is a plain `String` and carries no annotations, so the spacing does the same
 * job. Whether it actually reads correctly is a real-device question, covered by the instrumented
 * accessibility test rather than here.
 *
 */
internal fun keyContentDescription(key: KeypadKey): String = when {
    key.letters.isEmpty() -> key.char.toString()
    else -> "${key.char}, " + key.letters.toCharArray().joinToString(separator = " ")
}
