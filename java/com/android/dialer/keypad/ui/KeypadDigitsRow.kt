package com.android.dialer.keypad.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadUiState

/** The minimum touch target Material and the accessibility guidelines both ask for. */
// Past the 48dp minimum: after the keys these are the most-hit controls. The legacy backspace was
// 56dp wide too, its 24dp icon padded by 16dp either side; the overflow now matches it.
private val TOUCH_TARGET = 56.dp
private val ICON_SIZE = 24.dp

/**
 * Overflow, the number, then backspace — the order Google's Phone app uses.
 *
 * Overflow and backspace are hidden with alpha rather than removed, so the number stays centered
 * and nothing reflows as the first character is typed or the last one deleted. The View keypad
 * made the same choice, using `INVISIBLE` rather than `GONE`.
 */
@Composable
internal fun KeypadDigitsRow(
    uiState: KeypadUiState,
    digitsField: TextFieldValue,
    isCursorVisible: Boolean,
    focusRequests: Int,
    strings: KeypadStrings,
    onAction: (KeypadAction) -> Unit,
    onDigitsTouched: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OverflowButton(uiState = uiState, strings = strings, onAction = onAction)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KeypadDigitsField(
                value = digitsField,
                isCursorVisible = isCursorVisible,
                focusRequests = focusRequests,
                onValueChange = { value ->
                    onAction(
                        KeypadAction.DigitsEdited(
                            text = value.text,
                            selectionStart = value.selection.start,
                            selectionEnd = value.selection.end,
                        ),
                    )
                },
                onTyped = { char -> onAction(KeypadAction.CharacterTyped(char)) },
                onDelete = { onAction(KeypadAction.DeleteClicked) },
                onTouched = onDigitsTouched,
                onEnter = { onAction(KeypadAction.CallClicked) },
            )
            if (uiState.showsEmergencyCallWarning) {
                Text(
                    text = strings.emergencyCallWarning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(KEYPAD_EMERGENCY_WARNING_TEST_TAG),
                )
            }
        }

        DeleteButton(uiState = uiState, strings = strings, onAction = onAction)
    }
}

/**
 * Backspace. A tap deletes one character, a long press clears the field.
 *
 * A plain `IconButton` has no long press, hence the hand-rolled clickable.
 */
@Composable
private fun DeleteButton(
    uiState: KeypadUiState,
    strings: KeypadStrings,
    onAction: (KeypadAction) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .testTag(KEYPAD_DELETE_TEST_TAG)
            .size(TOUCH_TARGET)
            .alpha(if (uiState.isDeleteEnabled) 1f else 0f)
            .combinedClickable(
                enabled = uiState.isDeleteEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = TOUCH_TARGET / 2),
                onClickLabel = strings.deleteButton,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onAction(KeypadAction.DeleteClicked)
                },
                onLongClick = { onAction(KeypadAction.DeleteLongPressed) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Backspace,
            contentDescription = strings.deleteButton,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ICON_SIZE),
        )
    }
}

/** The overflow menu, whose every item acts on an existing number. */
@Composable
private fun OverflowButton(
    uiState: KeypadUiState,
    strings: KeypadStrings,
    onAction: (KeypadAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // The legacy PopupMenu was dismissed in onPause, so it was never found open on return.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        expanded = false
    }

    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = uiState.isOverflowVisible,
            modifier = Modifier
                .testTag(KEYPAD_OVERFLOW_TEST_TAG)
                .size(TOUCH_TARGET)
                .alpha(if (uiState.isOverflowVisible) 1f else 0f),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = strings.overflowButton,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Out of the keypad's left-to-right pin: the menu is text, and follows the language, as the
        // legacy PopupMenu did.
        CompositionLocalProvider(LocalLayoutDirection provides localeLayoutDirection()) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(text = strings.addPause) },
                    modifier = Modifier.testTag(KEYPAD_OVERFLOW_PAUSE_TEST_TAG),
                    onClick = {
                        expanded = false
                        onAction(KeypadAction.PauseClicked)
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = strings.addWait) },
                    modifier = Modifier.testTag(KEYPAD_OVERFLOW_WAIT_TEST_TAG),
                    onClick = {
                        expanded = false
                        onAction(KeypadAction.WaitClicked)
                    },
                )
                if (uiState.isCallWithNoteAvailable) {
                    DropdownMenuItem(
                        text = { Text(text = strings.callWithNote) },
                        modifier = Modifier.testTag(KEYPAD_OVERFLOW_CALL_WITH_NOTE_TEST_TAG),
                        onClick = {
                            expanded = false
                            onAction(KeypadAction.CallWithNoteClicked)
                        },
                    )
                }
            }
        }
    }
}
