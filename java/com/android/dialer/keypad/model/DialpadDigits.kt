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

/** The pause character, inserted by the keypad's "Add 2-sec pause" overflow item. */
internal const val PAUSE = ','

/** The wait character, inserted by the keypad's "Add wait" overflow item. */
internal const val WAIT = ';'

/**
 * The dialed-number buffer behind the keypad's digits field.
 *
 * This is deliberately an [Editable] rather than a plain [String]. The View dialpad got three
 * behaviors for free from its `EditText` that the Compose keypad must reproduce exactly:
 *
 *  * `UnicodeDialerKeyListener` as an `InputFilter`, which normalizes Unicode (e.g. Arabic-Indic)
 *    digits to ASCII and maps pasted letters onto keypad digits;
 *  * `DialerPhoneNumberFormattingTextWatcher` as a `TextWatcher`, which formats as you type;
 *  * a selection that survives edits, which backspace and pause/wait insertion both depend on.
 *
 * A [SpannableStringBuilder] runs the filters on every `replace` and dispatches to any `TextWatcher`
 * attached to it as a span, so keeping the real buffer keeps the real behavior instead of
 * reimplementing it. Everything here is pure state: no Android views, no telephony calls.
 */
internal class DialpadDigits {

    private val buffer: Editable = SpannableStringBuilder()

    private val _text = MutableStateFlow("")

    /**
     * The current number, republished on every edit.
     *
     * No mutator has to remember to publish. [TextEmitter] is attached to [buffer] as a
     * `TextWatcher` span, and `SpannableStringBuilder` dispatches to those on every `replace` —
     * the same mechanism the formatter runs on, so a reformat republishes too.
     */
    val text: StateFlow<String> = _text.asStateFlow()

    private val _value = MutableStateFlow(DigitsValue())

    /**
     * The number with its cursor or selection, for the digits field. Published by the same
     * [TextEmitter], which also watches the selection, so no mutator has to remember to update it.
     */
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

    /**
     * Attaches the as-you-type formatter.
     *
     * Mirrors `DialpadFragment`, which builds the watcher on a background thread because
     * libphonenumber cannot initialize on the main thread, and only then attaches it. The keypad
     * accepts input before formatting becomes available.
     */
    fun addFormattingWatcher(watcher: TextWatcher) {
        buffer.setSpan(watcher, 0, buffer.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }

    fun setSelection(start: Int, end: Int = start) {
        Selection.setSelection(buffer, start, end)
    }

    fun append(key: KeypadKey) {
        append(key.char)
    }

    /** Types [char] over the current selection, as pressing a key on the dialpad does. */
    fun append(char: Char) {
        val start = selectionStart
        val end = selectionEnd
        if (start < 0 || end < 0) {
            buffer.append(char)
            return
        }
        buffer.replace(minOf(start, end), maxOf(start, end), char.toString())
    }

    /** Backspace: deletes the selection, or the character before the cursor when there is none. */
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
     * Applies an edit made in the digits text field: typing or deleting at the cursor, pasting,
     * or just moving the cursor.
     *
     * Only the changed range is replaced, so the input filter sees just what was typed or pasted,
     * the formatter reformats as it would for any edit, and the cursor ends up after the new text
     * wherever the formatter moves it, as it did in the `EditText`. The field's own selection is
     * used only when the text is unchanged.
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

    /** Inserts [value] before everything already typed; the cursor keeps its place after it. */
    fun insertAtStart(value: String) {
        buffer.replace(0, 0, value)
    }

    /**
     * Inserts [PAUSE] or [WAIT] if the current selection allows it, returning whether it did.
     *
     * Port of `DialpadFragment.updateDialString`.
     */
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
            // Unselect: back to a regular cursor, just past the character inserted.
            setSelection(start + 1)
        }
        return true
    }

    /**
     * Removes the character before the cursor when it is [digit].
     *
     * Long-pressing 0 or 1 has to undo the digit the press already typed. Port of
     * `DialpadFragment.removePreviousDigitIfPossible`.
     */
    fun removePreviousDigitIfPossible(digit: Char) {
        val position = selectionStart
        if (position > 0 && buffer[position - 1] == digit) {
            setSelection(position)
            buffer.delete(position - 1, position)
        }
    }

    /**
     * Whether [newDigit] can be added at the current selection. Port of
     * `DialpadFragment.canAddDigit`, which is only ever called for [PAUSE] and [WAIT].
     */
    private fun canAddDigit(digits: CharSequence, start: Int, end: Int, newDigit: Char): Boolean {
        // The 1 lower bound is the "special digit cannot be the first digit" rule; it also rejects
        // the no-selection (-1) case. end >= start rejects a reversed selection.
        val withinBounds = start in 1..digits.length && end in start..digits.length
        return withinBounds && isWaitAllowedAt(digits, start, end, newDigit)
    }

    /** A [WAIT] may not sit directly next to another [WAIT]; a [PAUSE] has no such rule. */
    private fun isWaitAllowedAt(
        digits: CharSequence,
        start: Int,
        end: Int,
        newDigit: Char,
    ): Boolean =
        newDigit != WAIT ||
            (digits[start - 1] != WAIT && (digits.length <= end || digits[end] != WAIT))

    /**
     * Publishes [text] and [value] on every change of text, and [value] on every move of the
     * cursor, which a `TextWatcher` alone would miss: the formatter places the cursor after its
     * rewrite, and a tap moves it without any text changing.
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
