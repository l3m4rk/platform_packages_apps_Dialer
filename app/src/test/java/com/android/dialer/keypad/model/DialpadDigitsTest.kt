package com.android.dialer.keypad.model

import android.os.Build
import com.android.dialer.dialpadview.DialerPhoneNumberFormattingTextWatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
class DialpadDigitsTest {

    @Test
    fun startsEmptyWithCursorAtZero() {
        val digits = DialpadDigits()

        assertEquals("", digits.text)
        assertTrue(digits.isEmpty)
        assertEquals(0, digits.selectionStart)
    }

    @Test
    fun appendTypesKeysInOrder() {
        val digits = DialpadDigits()

        digits.append(KeypadKey.FIVE)
        digits.append(KeypadKey.STAR)
        digits.append(KeypadKey.POUND)

        assertEquals("5*#", digits.text)
        assertEquals(3, digits.selectionStart)
    }

    @Test
    fun appendInsertsAtTheCursor() {
        val digits = DialpadDigits()
        digits.setText("13")

        digits.setSelection(1)
        digits.append('2')

        assertEquals("123", digits.text)
    }

    @Test
    fun appendReplacesTheSelection() {
        val digits = DialpadDigits()
        digits.setText("199")

        digits.setSelection(1, 3)
        digits.append('2')

        assertEquals("12", digits.text)
    }

    @Test
    fun deleteRemovesTheCharacterBeforeTheCursor() {
        val digits = DialpadDigits()
        digits.setText("123")

        digits.delete()

        assertEquals("12", digits.text)
    }

    @Test
    fun deleteRemovesTheSelection() {
        val digits = DialpadDigits()
        digits.setText("12345")

        digits.setSelection(1, 4)
        digits.delete()

        assertEquals("15", digits.text)
    }

    @Test
    fun deleteAtTheStartDoesNothing() {
        val digits = DialpadDigits()
        digits.setText("123")

        digits.setSelection(0)
        digits.delete()

        assertEquals("123", digits.text)
    }

    @Test
    fun clearEmptiesTheBuffer() {
        val digits = DialpadDigits()
        digits.setText("5551234")

        digits.clear()

        assertEquals("", digits.text)
        assertTrue(digits.isEmpty)
    }

    @Test
    fun unicodeDigitsAreNormalisedToAscii() {
        val digits = DialpadDigits()

        // Arabic-Indic digits, as produced by pasting from an Arabic keyboard.
        digits.setText("١٢٣")

        assertEquals("123", digits.text)
    }

    @Test
    fun pastedLettersAreMappedOntoKeypadDigits() {
        val digits = DialpadDigits()

        digits.setText("ABC")

        assertEquals("222", digits.text)
    }

    @Test
    fun pauseCannotBeTheFirstCharacter() {
        val digits = DialpadDigits()

        assertFalse(digits.insertDialStringChar(PAUSE))
        assertEquals("", digits.text)
    }

    @Test
    fun pauseIsInsertedAtTheCursor() {
        val digits = DialpadDigits()
        digits.setText("123")

        assertTrue(digits.insertDialStringChar(PAUSE))

        assertEquals("123,", digits.text)
    }

    @Test
    fun waitCannotFollowAnotherWait() {
        val digits = DialpadDigits()
        digits.setText("123")
        assertTrue(digits.insertDialStringChar(WAIT))

        assertFalse(digits.insertDialStringChar(WAIT))

        assertEquals("123;", digits.text)
    }

    @Test
    fun waitCannotPrecedeAnotherWait() {
        val digits = DialpadDigits()
        digits.setText("123;")

        digits.setSelection(3)

        assertFalse(digits.insertDialStringChar(WAIT))
        assertEquals("123;", digits.text)
    }

    @Test
    fun pauseMayFollowAWait() {
        val digits = DialpadDigits()
        digits.setText("123")
        assertTrue(digits.insertDialStringChar(WAIT))

        assertTrue(digits.insertDialStringChar(PAUSE))

        assertEquals("123;,", digits.text)
    }

    @Test
    fun insertDialStringCharRejectsOtherCharacters() {
        val digits = DialpadDigits()
        digits.setText("123")

        val failure = runCatching { digits.insertDialStringChar('4') }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun removePreviousDigitIfPossibleRemovesAMatchingDigit() {
        val digits = DialpadDigits()
        digits.setText("11")

        digits.removePreviousDigitIfPossible('1')

        assertEquals("1", digits.text)
    }

    @Test
    fun removePreviousDigitIfPossibleLeavesANonMatchingDigit() {
        val digits = DialpadDigits()
        digits.setText("12")

        digits.removePreviousDigitIfPossible('1')

        assertEquals("12", digits.text)
    }

    @Test
    fun formattingWatcherFormatsAsYouType() {
        val digits = DialpadDigits()
        digits.addFormattingWatcher(DialerPhoneNumberFormattingTextWatcher("US"))

        "6505551212".forEach { digits.append(it) }

        assertEquals("(650) 555-1212", digits.text)
    }

    @Test
    fun formattingWatcherSkipsArgentinaDomesticMobileNumbers() {
        val digits = DialpadDigits()
        digits.addFormattingWatcher(DialerPhoneNumberFormattingTextWatcher("AR"))

        // Area code 11 followed by the 15 prefix marks a domestic call to a mobile, which
        // libphonenumber formats incorrectly, so the watcher deliberately leaves it raw.
        "1115678901".forEach { digits.append(it) }

        assertEquals("1115678901", digits.text)
    }

    @Test
    fun formattingWatcherStillFormatsOtherArgentinaNumbers() {
        val digits = DialpadDigits()
        digits.addFormattingWatcher(DialerPhoneNumberFormattingTextWatcher("AR"))

        // No 15 prefix, so the Argentina bypass must not swallow the normal formatting.
        "1156789012".forEach { digits.append(it) }

        assertEquals("11 5678-9012", digits.text)
    }
}
