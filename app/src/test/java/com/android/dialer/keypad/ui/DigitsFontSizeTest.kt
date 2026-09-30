package com.android.dialer.keypad.ui

import android.os.Build
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
// Real text measurement: Robolectric's default graphics give every glyph a nominal width.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DigitsFontSizeTest {

    private val measurer = TextMeasurer(
        defaultFontFamilyResolver = createFontFamilyResolver(RuntimeEnvironment.getApplication()),
        defaultDensity = Density(density = 1f),
        defaultLayoutDirection = LayoutDirection.Ltr,
    )
    private val style = TextStyle(fontSize = 36.sp)

    @Test
    fun aNumberThatFitsKeepsTheFullSize() {
        val width = widthOf("555", style)

        assertEquals(36.sp, fit("555", width + 10))
    }

    @Test
    fun aLongNumberShrinksJustEnoughToFit() {
        val number = "+372 5123 4567"
        val fullWidth = widthOf(number, style)
        val available = fullWidth * 3 / 4

        val size = fit(number, available)

        // In proportion, as ViewUtil.resizeText scaled it.
        assertEquals(36f * available / fullWidth, size.value, 0.01f)
        val shrunkWidth = widthOf(number, style.copy(fontSize = size))
        assertTrue("$shrunkWidth fits in $available", shrunkWidth <= available + 1)
    }

    @Test
    fun aVeryLongNumberStopsAtTheFloorAndScrollsInstead() {
        val number = "+372 5123 4567,1234;5678"

        assertEquals(DIGITS_MIN_FONT_SIZE, fit(number, widthOf(number, style) / 4))
    }

    @Test
    fun anEmptyFieldKeepsTheFullSize() {
        assertEquals(36.sp, fit("", maxWidthPx = 1))
    }

    private fun fit(text: String, maxWidthPx: Int) =
        digitsFontSize(text = text, style = style, maxWidthPx = maxWidthPx, measurer = measurer)

    private fun widthOf(text: String, style: TextStyle): Int =
        measurer.measure(text = text, style = style, maxLines = 1, softWrap = false).size.width
}
