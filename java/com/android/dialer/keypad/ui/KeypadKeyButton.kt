package com.android.dialer.keypad.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.theme.compose.DialerMotion

private val KEY_MIN_HEIGHT = 64.dp

// Four rows, the number and the call button must fit a landscape phone's height.
private val COMPACT_KEY_MIN_HEIGHT = 40.dp
private val COMPACT_SUBTITLE_SPACING = 6.dp

// Half the minimum height: a full pill at rest, squared off by a press.
private val KEY_RESTING_CORNER = 32.dp
private val KEY_PRESSED_CORNER = 16.dp
private val VOICEMAIL_GLYPH_SIZE = 18.dp

/**
 * Reports press and release rather than a click, as a key's tone runs between them. [onRelease]
 * also fires on cancellation, so a finger sliding off still stops the tone.
 */
@Composable
internal fun KeypadKeyButton(
    key: KeypadKey,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    digit: String = key.char.toString(),
    secondaryLetters: String? = null,
    longPressLabel: String? = null,
    onLongPress: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val onPressWithFeedback = haptics.before(HapticFeedbackType.VirtualKey, onPress)
    val onLongPressWithFeedback = onLongPress?.let { longPress ->
        haptics.before(HapticFeedbackType.LongPress, longPress)
    }
    // The pressed shape recomposes the key mid-press; this keeps the gesture running.
    val currentOnPress by rememberUpdatedState(onPressWithFeedback)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnLongPress by rememberUpdatedState(onLongPressWithFeedback)
    val corner by animateDpAsState(
        targetValue = if (isPressed) KEY_PRESSED_CORNER else KEY_RESTING_CORNER,
        animationSpec = DialerMotion.fastSpatial(),
        label = "keyCorner",
    )

    Surface(
        modifier = modifier
            .testTag(keypadKeyTestTag(key))
            .heightIn(min = if (isCompact) COMPACT_KEY_MIN_HEIGHT else KEY_MIN_HEIGHT)
            .keyPressGestures(
                key = key,
                interactionSource = interactionSource,
                onPress = { currentOnPress() },
                onRelease = { currentOnRelease() },
                onLongPress = onLongPress?.let { { currentOnLongPress?.invoke() } },
            )
            .keySemantics(
                description = keyContentDescription(key, digit),
                onPress = onPressWithFeedback,
                onRelease = onRelease,
                longPressLabel = longPressLabel,
                onLongPress = onLongPressWithFeedback,
            ),
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        // Inside the surface, so the ripple is clipped to the key's changing shape.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .indication(interactionSource = interactionSource, indication = ripple()),
            contentAlignment = Alignment.Center,
        ) {
            KeyLabel(
                key = key,
                digit = digit,
                secondaryLetters = secondaryLetters,
                isCompact = isCompact,
            )
        }
    }
}

@Composable
private fun KeyLabel(
    key: KeypadKey,
    digit: String,
    secondaryLetters: String?,
    isCompact: Boolean,
) {
    if (isCompact) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = COMPACT_SUBTITLE_SPACING,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KeyDigit(digit = digit)
            KeySubtitle(key = key, secondaryLetters = secondaryLetters)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            KeyDigit(digit = digit)
            KeySubtitle(key = key, secondaryLetters = secondaryLetters)
        }
    }
}

@Composable
private fun KeyDigit(digit: String) {
    Text(
        text = digit,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun KeySubtitle(key: KeypadKey, secondaryLetters: String?) {
    when {
        key == KeypadKey.ONE -> Icon(
            imageVector = Icons.Rounded.Voicemail,
            // The key's long-press label already says it.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(VOICEMAIL_GLYPH_SIZE),
        )
        key.letters.isNotEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val style = when (secondaryLetters) {
                null -> MaterialTheme.typography.bodyMedium
                else -> MaterialTheme.typography.labelSmall
            }
            KeyLetters(letters = key.letters, style = style)
            secondaryLetters?.let { letters -> KeyLetters(letters = letters, style = style) }
        }
    }
}

@Composable
private fun KeyLetters(letters: String, style: TextStyle) {
    Text(
        text = letters,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private fun HapticFeedback.before(type: HapticFeedbackType, action: () -> Unit): () -> Unit = {
    performHapticFeedback(type)
    action()
}

// detectTapGestures emits no interactions, so they are emitted here for the ripple and shape.
private fun Modifier.keyPressGestures(
    key: KeypadKey,
    interactionSource: MutableInteractionSource,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onLongPress: (() -> Unit)?,
): Modifier = pointerInput(key, interactionSource, onLongPress != null) {
    detectTapGestures(
        onPress = { offset ->
            val press = PressInteraction.Press(offset)
            interactionSource.emit(press)
            onPress()
            val released = tryAwaitRelease()
            interactionSource.emit(
                if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
            )
            onRelease()
        },
        onLongPress = onLongPress?.let { { _ -> it() } },
    )
}

// detectTapGestures contributes no semantics. An activation must send both edges, or its tone never
// stops.
private fun Modifier.keySemantics(
    description: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    longPressLabel: String?,
    onLongPress: (() -> Unit)?,
): Modifier = semantics(mergeDescendants = true) {
    contentDescription = description
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
 * "2, A B C": spaced letters are spelled out rather than read as a word, as `contentDescription`
 * takes no `TtsSpan`.
 */
internal fun keyContentDescription(key: KeypadKey, digit: String = key.char.toString()): String =
    when {
        // 0's + is announced by its long-press label.
        key.letters.isEmpty() || key == KeypadKey.ZERO -> digit
        else -> "$digit, " + key.letters.toCharArray().joinToString(separator = " ")
    }
