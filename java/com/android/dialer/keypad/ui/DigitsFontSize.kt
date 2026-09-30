package com.android.dialer.keypad.ui

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** How small the number may get; past it, the field scrolls to the cursor instead. */
internal val DIGITS_MIN_FONT_SIZE = 24.sp

/**
 * The font size that fits [text] into [maxWidthPx] on one line: [style]'s own size when it already
 * fits, otherwise scaled down in proportion, but never below [minFontSize].
 *
 * Port of `ViewUtil.resizeText`, which the legacy `ResizingTextEditText` ran on every change of
 * text or width: the same proportional rule and the same 24sp floor, rather than stepping down.
 */
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
