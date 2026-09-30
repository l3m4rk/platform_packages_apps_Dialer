package com.android.dialer.keypad.ui

import android.app.Application
import android.os.Build
import com.android.dialer.dialpadview.DialpadCharMappings
import com.android.dialer.i18n.LocaleUtils
import com.android.dialer.keypad.model.KeypadKey
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
class KeypadKeyLabelsTest {

    @Test
    fun englishShowsPlainDigitsAndNoSecondAlphabet() {
        val labels = KeypadKeyLabels.of(Locale.ENGLISH, secondaryKeyToChars = null)

        assertEquals("2", labels.digit(KeypadKey.TWO))
        assertNull(labels.secondaryLetters(KeypadKey.TWO))
    }

    @Test
    fun persianShowsPersianDigits() {
        val labels = KeypadKeyLabels.of(Locale.forLanguageTag("fa"), secondaryKeyToChars = null)

        assertEquals("۰", labels.digit(KeypadKey.ZERO))
        assertEquals("۲", labels.digit(KeypadKey.TWO))
        assertEquals("۹", labels.digit(KeypadKey.NINE))
        assertEquals("*", labels.digit(KeypadKey.STAR))
        assertEquals("#", labels.digit(KeypadKey.POUND))
    }

    @Test
    fun arabicKeepsPlainDigitsAsTheLegacyKeypadDid() {
        // DialpadView localized the digits for Persian alone.
        val labels = KeypadKeyLabels.of(Locale.forLanguageTag("ar"), secondaryKeyToChars = null)

        assertEquals("2", labels.digit(KeypadKey.TWO))
    }

    @Test
    fun aSecondAlphabetLandsOnItsKeys() {
        val keyToChars = arrayOf("", "", "АБВГ", "ДЕЁЖЗ", "", "", "", "", "", "ЬЭЮЯ", "", "")

        val labels = KeypadKeyLabels.of(Locale.ENGLISH, keyToChars)

        assertEquals("АБВГ", labels.secondaryLetters(KeypadKey.TWO))
        assertEquals("ЬЭЮЯ", labels.secondaryLetters(KeypadKey.NINE))
        // Empty entries mean no second row, not an empty one.
        assertNull(labels.secondaryLetters(KeypadKey.ONE))
        assertNull(labels.secondaryLetters(KeypadKey.STAR))
    }

    @Test
    @Config(qualifiers = "ru")
    fun russianGetsItsLettersFromTheSharedMappings() {
        val labels = forCurrentLanguage()

        assertEquals("АБВГ", labels.secondaryLetters(KeypadKey.TWO))
        assertEquals("2", labels.digit(KeypadKey.TWO))
    }

    @Test
    @Config(qualifiers = "fa")
    fun persianIsPickedUpFromTheConfiguration() {
        assertEquals("۵", forCurrentLanguage().digit(KeypadKey.FIVE))
    }

    @Test
    @Config(qualifiers = "de")
    fun otherLanguagesGetTheLatinLettersAlone() {
        assertNull(forCurrentLanguage().secondaryLetters(KeypadKey.TWO))
    }

    /** What keypadKeyLabels() builds, without a composition around it. */
    private fun forCurrentLanguage(): KeypadKeyLabels {
        val context: Application = RuntimeEnvironment.getApplication()
        return KeypadKeyLabels.of(
            locale = LocaleUtils.getLocale(context),
            secondaryKeyToChars = DialpadCharMappings.getKeyToCharsMap(context),
        )
    }
}
