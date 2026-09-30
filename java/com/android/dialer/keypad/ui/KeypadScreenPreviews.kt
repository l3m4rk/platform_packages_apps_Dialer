package com.android.dialer.keypad.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.theme.compose.DialerPreviewTheme

@PreviewLightDark
@Composable
private fun KeypadScreenEmptyPreview() {
    DialerPreviewTheme {
        PreviewKeypadScreen(uiState = KeypadUiState())
    }
}

@PreviewLightDark
@Composable
private fun KeypadScreenWithNumberPreview() {
    DialerPreviewTheme {
        PreviewKeypadScreen(
            uiState = KeypadUiState(
                digits = "(650) 555-1212",
                isDeleteEnabled = true,
                isOverflowVisible = true,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun KeypadScreenEmergencyWarningPreview() {
    DialerPreviewTheme {
        PreviewKeypadScreen(uiState = KeypadUiState(showsEmergencyCallWarning = true))
    }
}

/** The narrowest phone still supported, where the key letters are most at risk of clipping. */
@Preview(widthDp = 320)
@Composable
private fun KeypadScreenCompactWidthPreview() {
    DialerPreviewTheme {
        PreviewKeypadScreen(
            uiState = KeypadUiState(
                digits = "+372 5123 4567",
                isDeleteEnabled = true,
                isOverflowVisible = true,
            ),
        )
    }
}

/** Largest accessibility font scale, which the View keypad did not survive on the key glyphs. */
@Preview(fontScale = 2f)
@Composable
private fun KeypadScreenLargeFontPreview() {
    DialerPreviewTheme {
        PreviewKeypadScreen(uiState = KeypadUiState(digits = "555"))
    }
}

/** Russian: the Cyrillic letters under the Latin ones, as the legacy keypad showed them. */
@Preview(locale = "ru")
@Composable
private fun KeypadScreenSecondAlphabetPreview() {
    DialerPreviewTheme {
        KeypadScreen(
            uiState = KeypadUiState(),
            strings = previewKeypadStrings().copy(keyLabels = keypadKeyLabels()),
            onAction = {},
        )
    }
}

/** Persian: Persian digits on the keys. */
@Preview(locale = "fa")
@Composable
private fun KeypadScreenPersianDigitsPreview() {
    DialerPreviewTheme {
        KeypadScreen(
            uiState = KeypadUiState(),
            strings = previewKeypadStrings().copy(keyLabels = keypadKeyLabels()),
            onAction = {},
        )
    }
}

/** A landscape phone: the keypad beside the search results, its keys compact. */
@Preview(device = "spec:width=891dp,height=411dp,orientation=landscape", showBackground = true)
@Composable
private fun KeypadScreenLandscapePreview() {
    DialerPreviewTheme {
        KeypadPlacement { placement ->
            KeypadScreen(
                uiState = KeypadUiState(
                    digits = "(650) 555-1212",
                    isDeleteEnabled = true,
                    isOverflowVisible = true,
                ),
                strings = previewKeypadStrings(),
                onAction = {},
                modifier = placement,
            )
        }
    }
}

@Composable
private fun PreviewKeypadScreen(uiState: KeypadUiState) {
    KeypadScreen(
        uiState = uiState,
        strings = previewKeypadStrings(),
        onAction = {},
    )
}

private fun previewKeypadStrings() = KeypadStrings(
    voicemailKeyAction = "call voicemail",
    plusKeyAction = "dial plus",
    deleteButton = "backspace",
    overflowButton = "More options",
    call = "Call",
    emergencyCallWarning = "Can't make emergency calls over Wi-Fi",
    addPause = "Add 2-sec pause",
    addWait = "Add wait",
    callWithNote = "Call with a note",
)
