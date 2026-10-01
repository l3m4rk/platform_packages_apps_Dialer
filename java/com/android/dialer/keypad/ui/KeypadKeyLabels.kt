package com.android.dialer.keypad.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.android.dialer.dialpadview.DialpadCharMappings
import com.android.dialer.i18n.LocaleUtils
import com.android.dialer.keypad.model.KeypadKey
import java.text.NumberFormat
import java.util.Locale

/** Persian digits, and Cyrillic letters under the Latin ones for Bulgarian, Russian and Ukrainian. */
@Immutable
internal data class KeypadKeyLabels(
    private val digits: Map<KeypadKey, String> = emptyMap(),
    private val secondaryLetters: Map<KeypadKey, String> = emptyMap(),
) {

    fun digit(key: KeypadKey): String = digits[key] ?: key.char.toString()

    fun secondaryLetters(key: KeypadKey): String? = secondaryLetters[key]

    companion object {
        // ISO 639-2, so Persian only, not Arabic or Urdu.
        private const val PERSIAN = "fas"

        /** [secondaryKeyToChars] is in `DialpadCharMappings`' layout: 0 to 9, `*`, `#`. */
        fun of(locale: Locale, secondaryKeyToChars: Array<String>?): KeypadKeyLabels {
            val format = NumberFormat.getInstance(locale).takeIf { locale.isO3Language == PERSIAN }
            val digits = format?.let {
                KeypadKey.entries
                    .filter { key -> key.char.isDigit() }
                    .associateWith { key -> format.format(key.char.digitToInt()) }
            }
            val secondary = secondaryKeyToChars?.let { chars ->
                KeypadKey.entries
                    .associateWith { key -> chars[key.mappingIndex] }
                    .filterValues { letters -> letters.isNotEmpty() }
            }
            return KeypadKeyLabels(
                digits = digits.orEmpty(),
                secondaryLetters = secondary.orEmpty(),
            )
        }

        private val KeypadKey.mappingIndex: Int
            get() = when (this) {
                KeypadKey.STAR -> STAR_INDEX
                KeypadKey.POUND -> POUND_INDEX
                else -> char.digitToInt()
            }

        private const val STAR_INDEX = 10
        private const val POUND_INDEX = 11
    }
}

@Composable
internal fun keypadKeyLabels(): KeypadKeyLabels {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        KeypadKeyLabels.of(
            locale = LocaleUtils.getLocale(context),
            secondaryKeyToChars = DialpadCharMappings.getKeyToCharsMap(context),
        )
    }
}
