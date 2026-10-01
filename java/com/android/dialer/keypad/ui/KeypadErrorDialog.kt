package com.android.dialer.keypad.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.dialer.keypad.model.KeypadError
import com.android.dialer.theme.compose.DialerPreviewTheme

/**
 * The keypad's error, a message and OK, as `DialpadFragment.ErrorDialogFragment` showed it; a
 * Material 3 dialog now, drawn from state rather than shown by the fragment.
 */
@Composable
internal fun KeypadErrorDialog(
    error: KeypadError,
    strings: KeypadStrings,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(KEYPAD_ERROR_DIALOG_OK_TEST_TAG),
            ) {
                Text(text = strings.ok)
            }
        },
        modifier = modifier.testTag(KEYPAD_ERROR_DIALOG_TEST_TAG),
        text = { Text(text = strings.message(error)) },
    )
}

private fun KeypadStrings.message(error: KeypadError): String = when (error) {
    KeypadError.VOICEMAIL_AIRPLANE_MODE -> voicemailAirplaneModeError
    KeypadError.VOICEMAIL_NOT_READY -> voicemailNotReadyError
    KeypadError.PROHIBITED_NUMBER -> prohibitedNumberError
}

@PreviewLightDark
@Composable
private fun KeypadErrorDialogPreview() {
    DialerPreviewTheme {
        KeypadErrorDialog(
            error = KeypadError.VOICEMAIL_NOT_READY,
            strings = previewKeypadStrings(),
            onDismiss = {},
        )
    }
}
