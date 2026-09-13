package com.android.dialer.keypad.model

import android.text.Editable
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import com.android.dialer.dialpadview.UnicodeDialerKeyListener

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

    init {
        buffer.filters = arrayOf(UnicodeDialerKeyListener.INSTANCE)
        Selection.setSelection(buffer, 0)
    }

    val text: String
        get() = buffer.toString()

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
}
