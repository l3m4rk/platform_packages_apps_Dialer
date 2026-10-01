package com.android.dialer.keypad.ui

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

internal val DIGITS_MIN_FONT_SIZE = 24.sp

/** Scales down in proportion to fit one line, never below [minFontSize]. */
internal fun digitsFontSize(
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
    measurer: TextMeasurer,
    minFontSize: TextUnit = DIGITS_MIN_FONT_SIZE,
): TextUnit {
    if (text.isEmpty() || maxWidthPx <= 0) {
        return style.fontSize
    }
    val width = measurer.measure(text = text, style = style, maxLines = 1, softWrap = false)
        .size
        .width
    return when {
        width <= maxWidthPx -> style.fontSize
        else -> maxOf(style.fontSize.value * maxWidthPx / width, minFontSize.value).sp
    }
}
