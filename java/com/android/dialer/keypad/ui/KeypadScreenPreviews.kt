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
)
