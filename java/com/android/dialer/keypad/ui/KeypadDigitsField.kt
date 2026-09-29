package com.android.dialer.keypad.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.awaitCancellation

/**
 * The number, as an editable field: the legacy digits were an `EditText`, and a tap placing the
 * cursor, long-press copy and paste, and typing on a hardware keyboard all came from that.
 *
 * Every edit goes to the view model and comes back through [value] once the keypad's own filter
 * and formatter have run, so what is shown is always what will be dialed.
 *
 * Keys typed on a hardware keyboard do not go through the field's own editing: they come out as
 * [onTyped] and [onDelete], commands applied at the cursor, as the legacy field's key listener
 * applied them. The field would otherwise build each edit from its own copy of the text, which
 * only catches up with the formatted number a frame later; two keys inside one frame then undo the
 * formatter's separators, and the formatter stops for good. Only paste, cut and cursor moves reach
 * [onValueChange], and those never come faster than a frame.
 *
 * @param isCursorVisible the legacy keypad only showed the cursor after the field was touched,
 *   and hid it again when a key was typed at the end; the caller keeps that rule.
 * @param focusRequests take focus whenever this changes, as the legacy fragment did each time the
 *   keypad was shown, so that a hardware keyboard types without a tap first.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun KeypadDigitsField(
    value: TextFieldValue,
    isCursorVisible: Boolean,
    focusRequests: Int,
    onValueChange: (TextFieldValue) -> Unit,
    onTyped: (Char) -> Unit,
    onDelete: () -> Unit,
    onTouched: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnTouched by rememberUpdatedState(onTouched)

    LaunchedEffect(focusRequests) {
        focusRequester.requestFocus()
    }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                currentOnTouched()
            }
        }
    }

    // Never the soft keyboard: the keypad is the keyboard. DigitsEditText turned it off with
    // setShowSoftInputOnFocus(false); refusing the input session does the same, while hardware
    // keys, the cursor and the clipboard do not go through it.
    InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .testTag(KEYPAD_DIGITS_TEST_TAG)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    handleHardwareKey(
                        event = event,
                        onTyped = onTyped,
                        onDelete = onDelete,
                        onEnter = onEnter,
                    )
                },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            cursorBrush = SolidColor(
                if (isCursorVisible) MaterialTheme.colorScheme.primary else Color.Transparent,
            ),
            interactionSource = interactionSource,
        )
    }
}

/**
 * Turns a hardware key into a keypad command, or leaves it to the field: arrows, selection and
 * shortcuts such as Ctrl+V still work as in any text field.
 */
private fun handleHardwareKey(
    event: KeyEvent,
    onTyped: (Char) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
): Boolean {
    val char = event.utf16CodePoint.toChar()
    val command: (() -> Unit)? = when {
        event.isCtrlPressed || event.isMetaPressed -> null
        // Enter dials, as the legacy field's OnKeyListener made it.
        event.key == Key.Enter || event.key == Key.NumPadEnter -> onEnter
        event.key == Key.Backspace -> onDelete
        event.utf16CodePoint != 0 && !char.isISOControl() -> {
            { onTyped(char) }
        }
        else -> null
    }
    if (command != null && event.type == KeyEventType.KeyDown) {
        command()
    }
    // Both edges of a handled key, so the field never sees the key up of a key it did not type.
    return command != null
}
