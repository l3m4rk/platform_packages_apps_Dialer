package com.android.dialer.keypad.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.awaitCancellation

/**
 * The editable number. Edits come back through [value] after the filter and formatter have run.
 *
 * Hardware keys go out as [onTyped] and [onDelete] commands rather than through the field's own
 * editing: the field's copy of the text trails the formatted one by a frame, and two keys in one
 * frame would undo the formatter's separators. Only paste, cut and cursor moves reach
 * [onValueChange].
 *
 * @param focusRequests take focus whenever this changes, so a hardware keyboard types without a tap.
 */
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

    val style = MaterialTheme.typography.displaySmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fontSize = digitsFontSize(
            text = value.text,
            style = style,
            maxWidthPx = constraints.maxWidth,
            measurer = measurer,
        )
        DigitsTextField(
            value = value,
            style = style.copy(fontSize = fontSize),
            isCursorVisible = isCursorVisible,
            focusRequester = focusRequester,
            interactionSource = interactionSource,
            onValueChange = onValueChange,
            onTyped = onTyped,
            onDelete = onDelete,
            onEnter = onEnter,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DigitsTextField(
    value: TextFieldValue,
    style: TextStyle,
    isCursorVisible: Boolean,
    focusRequester: FocusRequester,
    interactionSource: MutableInteractionSource,
    onValueChange: (TextFieldValue) -> Unit,
    onTyped: (Char) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
) {
    // Never the soft keyboard: the keypad is the keyboard. Hardware keys and the clipboard still work.
    InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
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
            textStyle = style,
            singleLine = true,
            cursorBrush = SolidColor(
                if (isCursorVisible) MaterialTheme.colorScheme.primary else Color.Transparent,
            ),
            interactionSource = interactionSource,
        )
    }
}

// Arrows, selection and shortcuts such as Ctrl+V are left to the field.
private fun handleHardwareKey(
    event: KeyEvent,
    onTyped: (Char) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
): Boolean {
    val char = event.utf16CodePoint.toChar()
    val command: (() -> Unit)? = when {
        event.isCtrlPressed || event.isMetaPressed -> null
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
    // Both edges, so the field never sees the key up of a key it did not type.
    return command != null
}
