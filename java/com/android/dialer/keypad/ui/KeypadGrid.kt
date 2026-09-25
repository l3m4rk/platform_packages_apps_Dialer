package com.android.dialer.keypad.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadKey

private val KEY_SPACING = 8.dp

/** The twelve keys, laid out as a phone always has laid them out. */
private val KEY_ROWS = listOf(
    listOf(KeypadKey.ONE, KeypadKey.TWO, KeypadKey.THREE),
    listOf(KeypadKey.FOUR, KeypadKey.FIVE, KeypadKey.SIX),
    listOf(KeypadKey.SEVEN, KeypadKey.EIGHT, KeypadKey.NINE),
    listOf(KeypadKey.STAR, KeypadKey.ZERO, KeypadKey.POUND),
)

@Composable
internal fun KeypadGrid(
    strings: KeypadStrings,
    onAction: (KeypadAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KEY_SPACING),
    ) {
        KEY_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KEY_SPACING),
            ) {
                row.forEach { key ->
                    KeypadKeyButton(
                        key = key,
                        onPress = { onAction(KeypadAction.KeyPressed(key)) },
                        onRelease = { onAction(KeypadAction.KeyReleased(key)) },
                        modifier = Modifier.weight(1f),
                        longPressLabel = longPressLabel(key = key, strings = strings),
                        onLongPress = longPressAction(key = key, onAction = onAction),
                    )
                }
            }
        }
    }
}

/** Only 1 and 0 have a long press, and each announces what it will do. */
private fun longPressLabel(key: KeypadKey, strings: KeypadStrings): String? = when (key) {
    KeypadKey.ONE -> strings.voicemailKeyAction
    KeypadKey.ZERO -> strings.plusKeyAction
    else -> null
}

private fun longPressAction(
    key: KeypadKey,
    onAction: (KeypadAction) -> Unit,
): (() -> Unit)? = when (key) {
    KeypadKey.ONE -> {
        { onAction(KeypadAction.VoicemailKeyLongPressed) }
    }
    KeypadKey.ZERO -> {
        { onAction(KeypadAction.PlusKeyLongPressed) }
    }
    else -> null
}
