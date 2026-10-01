package com.android.dialer.keypad.model

import android.text.Editable
import android.text.Selection
import android.text.SpanWatcher
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import com.android.dialer.dialpadview.UnicodeDialerKeyListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val PAUSE = ','

internal const val WAIT = ';'

/**
 * The dialed number, kept as an [Editable] so the existing input filter, as-you-type formatter and
 * selection handling run unchanged instead of being reimplemented.
 */
internal class DialpadDigits {

    private val buffer: Editable = SpannableStringBuilder()

    private val _text = MutableStateFlow("")

    val text: StateFlow<String> = _text.asStateFlow()

    private val _value = MutableStateFlow(DigitsValue())

    val value: StateFlow<DigitsValue> = _value.asStateFlow()

    init {
        buffer.filters = arrayOf(UnicodeDialerKeyListener.INSTANCE)
        // Attached before any formatter, so the formatter's own rewrite re-enters the watchers and
        // the last value published is the formatted one.
        buffer.setSpan(TextEmitter(), 0, 0, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        Selection.setSelection(buffer, 0)
    }

    val length: Int
        get() = buffer.length

    val isEmpty: Boolean
        get() = buffer.isEmpty()

    val selectionStart: Int
        get() = Selection.getSelectionStart(buffer)

    val selectionEnd: Int
        get() = Selection.getSelectionEnd(buffer)

    fun addFormattingWatcher(watcher: TextWatcher) {
        buffer.setSpan(watcher, 0, buffer.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }

    fun setSelection(start: Int, end: Int = start) {
        Selection.setSelection(buffer, start, end)
    }

    fun append(key: KeypadKey) {
        append(key.char)
    }

    fun append(char: Char) {
        val start = selectionStart
        val end = selectionEnd
        if (start < 0 || end < 0) {
            buffer.append(char)
            return
        }
        buffer.replace(minOf(start, end), maxOf(start, end), char.toString())
    }

    fun delete() {
        val start = selectionStart
        val end = selectionEnd
        when {
            start < 0 || end < 0 -> Unit
            start != end -> buffer.delete(minOf(start, end), maxOf(start, end))
            start > 0 -> buffer.delete(start - 1, start)
            else -> Unit
        }
    }

    fun clear() {
        buffer.clear()
    }

    fun setText(value: String) {
        buffer.replace(0, buffer.length, value)
    }

    /**
     * Replaces only the changed range, so the filter sees just what was typed or pasted and the
     * cursor lands after it wherever the formatter moves the text.
     */
    fun applyEdit(value: String, selectionStart: Int, selectionEnd: Int) {
        val current = buffer.toString()
        if (value == current) {
            val start = minOf(selectionStart, selectionEnd).coerceIn(0, length)
            val end = maxOf(selectionStart, selectionEnd).coerceIn(0, length)
            setSelection(start, end)
            return
        }
        val prefix = current.commonPrefixWith(value).length
        val suffix = current.substring(prefix).commonSuffixWith(value.substring(prefix)).length
        val oldEnd = current.length - suffix
        val newEnd = value.length - suffix
        // A cursor at the end of the replaced range stays after the replacement.
        setSelection(oldEnd)
        buffer.replace(prefix, oldEnd, value, prefix, newEnd)
    }

    fun insertAtStart(value: String) {
        buffer.replace(0, 0, value)
    }

    fun insertDialStringChar(char: Char): Boolean {
        require(char == WAIT || char == PAUSE) {
            "Not expected for anything other than PAUSE & WAIT"
        }

        var start = minOf(selectionStart, selectionEnd)
        var end = maxOf(selectionStart, selectionEnd)
        if (start == -1) {
            start = buffer.length
            end = buffer.length
        }

        if (!canAddDigit(buffer, start, end, char)) {
            return false
        }

        buffer.replace(start, end, char.toString())
        if (start != end) {
            setSelection(start + 1)
        }
        return true
    }

    fun removePreviousDigitIfPossible(digit: Char) {
        val position = selectionStart
        if (position > 0 && buffer[position - 1] == digit) {
            setSelection(position)
            buffer.delete(position - 1, position)
        }
    }

    private fun canAddDigit(digits: CharSequence, start: Int, end: Int, newDigit: Char): Boolean {
        // Not first in the number, and not with a reversed or missing selection.
        val withinBounds = start in 1..digits.length && end in start..digits.length
        return withinBounds && isWaitAllowedAt(digits, start, end, newDigit)
    }

    private fun isWaitAllowedAt(
        digits: CharSequence,
        start: Int,
        end: Int,
        newDigit: Char,
    ): Boolean =
        newDigit != WAIT ||
            (digits[start - 1] != WAIT && (digits.length <= end || digits[end] != WAIT))

    /**
     * Publishes on every edit, so no mutator has to. Also a [SpanWatcher], as cursor moves change
     * no text.
     */
    private inner class TextEmitter :
        TextWatcher,
        SpanWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable) {
            _text.value = s.toString()
            publishValue()
        }

        override fun onSpanAdded(text: Spannable, what: Any, start: Int, end: Int) =
            onSpan(what)

        override fun onSpanRemoved(text: Spannable, what: Any, start: Int, end: Int) =
            onSpan(what)

        override fun onSpanChanged(
            text: Spannable,
            what: Any,
            ostart: Int,
            oend: Int,
            nstart: Int,
            nend: Int,
        ) = onSpan(what)

        private fun onSpan(what: Any) {
            if (what === Selection.SELECTION_START || what === Selection.SELECTION_END) {
                publishValue()
            }
        }

        private fun publishValue() {
            _value.value = DigitsValue(
                text = buffer.toString(),
                selectionStart = selectionStart.coerceIn(0, buffer.length),
                selectionEnd = selectionEnd.coerceIn(0, buffer.length),
            )
        }
    }
}
