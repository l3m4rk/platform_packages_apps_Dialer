package com.android.dialer.keypad.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.theme.compose.DialerColors

private val SHEET_CORNER = 28.dp
private val SHEET_HORIZONTAL_PADDING = 8.dp
private val SHEET_VERTICAL_PADDING = 12.dp
private val SECTION_SPACING = 12.dp
private val CALL_BUTTON_MIN_HEIGHT = 56.dp
private val CALL_BUTTON_HORIZONTAL_PADDING = 28.dp
private val CALL_ICON_SIZE = 24.dp
private val CALL_ICON_LABEL_SPACING = 12.dp

/**
 * The keypad, with no state of its own, laid out after Google's Phone app.
 *
 * Everything it renders arrives in [uiState], every user gesture leaves through [onAction], and
 * every string arrives in [strings]. That keeps it drivable by a fake screen model in tests and by
 * fixed values in previews.
 */
@Composable
internal fun KeypadScreen(
    uiState: KeypadUiState,
    strings: KeypadStrings,
    onAction: (KeypadAction) -> Unit,
    modifier: Modifier = Modifier,
    entranceState: KeypadEntranceState = rememberKeypadEntranceState(),
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = SHEET_CORNER, topEnd = SHEET_CORNER),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = SHEET_HORIZONTAL_PADDING,
                vertical = SHEET_VERTICAL_PADDING,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        ) {
            KeypadDigitsRow(uiState = uiState, strings = strings, onAction = onAction)

            KeypadGrid(strings = strings, onAction = onAction, entranceState = entranceState)

            CallButton(
                isPseudoEmergencyNumber = uiState.isPseudoEmergencyNumber,
                strings = strings,
                onAction = onAction,
            )
        }
    }
}

/** A labeled green pill, centered under the keys, sized to its content rather than the row. */
@Composable
private fun CallButton(
    isPseudoEmergencyNumber: Boolean,
    strings: KeypadStrings,
    onAction: (KeypadAction) -> Unit,
) {
    Button(
        onClick = { onAction(KeypadAction.CallClicked) },
        modifier = Modifier
            .testTag(KEYPAD_CALL_TEST_TAG)
            .heightIn(min = CALL_BUTTON_MIN_HEIGHT),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = callButtonContainerColor(isPseudoEmergencyNumber),
            contentColor = DialerColors.OnCallContainer,
        ),
        contentPadding = PaddingValues(horizontal = CALL_BUTTON_HORIZONTAL_PADDING),
    ) {
        Icon(
            imageVector = Icons.Rounded.Call,
            // The label beside it names the button.
            contentDescription = null,
            modifier = Modifier.size(CALL_ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(CALL_ICON_LABEL_SPACING))
        Text(text = strings.call, style = MaterialTheme.typography.titleMedium)
    }
}
