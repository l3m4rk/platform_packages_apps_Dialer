package com.android.dialer.keypad.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.theme.compose.DialerColors

private val SHEET_CORNER = 28.dp
private val SHEET_HORIZONTAL_PADDING = 8.dp
private val SHEET_VERTICAL_PADDING = 12.dp
private val SECTION_SPACING = 12.dp
private val CALL_BUTTON_MIN_HEIGHT = 56.dp

// Measured from Google's Phone app.
private val CALL_BUTTON_HORIZONTAL_PADDING = 20.dp

private val CALL_ICON_SIZE = 24.dp
private val CALL_ICON_LABEL_SPACING = 12.dp

@Composable
internal fun KeypadScreen(
    uiState: KeypadUiState,
    strings: KeypadStrings,
    onAction: (KeypadAction) -> Unit,
    modifier: Modifier = Modifier,
    entranceState: KeypadEntranceState = rememberKeypadEntranceState(),
    focusRequests: Int = 0,
) {
    val isLandscape = isLandscape()
    Surface(
        modifier = modifier
            .testTag(KEYPAD_SHEET_TEST_TAG)
            .fillMaxWidth()
            .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier),
        shape = if (isLandscape) {
            RoundedCornerShape(topStart = SHEET_CORNER, bottomStart = SHEET_CORNER)
        } else {
            RoundedCornerShape(topStart = SHEET_CORNER, topEnd = SHEET_CORNER)
        },
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        // A keypad reads 1 2 3 in every language; the sheet around it still follows the language.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            KeypadContent(
                uiState = uiState,
                focusRequests = focusRequests,
                strings = strings,
                onAction = onAction,
                isLandscape = isLandscape,
                entranceState = entranceState,
            )
        }
    }

    uiState.error?.let { error ->
        KeypadErrorDialog(
            error = error,
            strings = strings,
            onDismiss = { onAction(KeypadAction.ErrorDismissed) },
        )
    }
}

@Composable
private fun KeypadContent(
    uiState: KeypadUiState,
    focusRequests: Int,
    strings: KeypadStrings,
    onAction: (KeypadAction) -> Unit,
    isLandscape: Boolean,
    entranceState: KeypadEntranceState,
) {
    // The cursor shows once the number is touched, until it empties or a key is typed at its end.
    val digitsField = TextFieldValue(
        text = uiState.digits,
        selection = TextRange(uiState.selectionStart, uiState.selectionEnd),
    )
    var isCursorVisible by remember { mutableStateOf(false) }
    val currentField by rememberUpdatedState(digitsField)
    LaunchedEffect(digitsField.text.isEmpty()) {
        if (digitsField.text.isEmpty()) {
            isCursorVisible = false
        }
    }
    val onKeyAction: (KeypadAction) -> Unit = { action ->
        val field = currentField
        val isAtEnd = field.selection.collapsed && field.selection.end == field.text.length
        if (action is KeypadAction.KeyPressed && isAtEnd) {
            isCursorVisible = false
        }
        onAction(action)
    }

    Column(
        modifier = Modifier
            .padding(horizontal = SHEET_HORIZONTAL_PADDING, vertical = SHEET_VERTICAL_PADDING)
            .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
    ) {
        KeypadDigitsRow(
            uiState = uiState,
            digitsField = digitsField,
            isCursorVisible = isCursorVisible,
            focusRequests = focusRequests,
            strings = strings,
            onAction = onAction,
            onDigitsTouched = { isCursorVisible = digitsField.text.isNotEmpty() },
        )

        KeypadGrid(
            strings = strings,
            onAction = onKeyAction,
            // Landscape is short: the keys share what height is left, keeping the call button on screen.
            modifier = if (isLandscape) Modifier.weight(1f) else Modifier,
            isCompact = isLandscape,
            entranceState = entranceState,
        )

        // Out of the pin: the icon leads the label in the language's direction.
        CompositionLocalProvider(LocalLayoutDirection provides localeLayoutDirection()) {
            CallButton(
                isPseudoEmergencyNumber = uiState.isPseudoEmergencyNumber,
                strings = strings,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun CallButton(
    isPseudoEmergencyNumber: Boolean,
    strings: KeypadStrings,
    onAction: (KeypadAction) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onAction(KeypadAction.CallClicked)
        },
        modifier = Modifier
            .testTag(KEYPAD_CALL_TEST_TAG)
            .heightIn(min = CALL_BUTTON_MIN_HEIGHT),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = callButtonContainerColor(isPseudoEmergencyNumber),
            contentColor = DialerColors.onCallContainer(),
        ),
        contentPadding = PaddingValues(horizontal = CALL_BUTTON_HORIZONTAL_PADDING),
    ) {
        Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = null,
            modifier = Modifier.size(CALL_ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(CALL_ICON_LABEL_SPACING))
        Text(text = strings.call, style = MaterialTheme.typography.titleMedium)
    }
}
