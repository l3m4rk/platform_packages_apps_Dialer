package com.android.dialer.keypad.domain

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import com.android.dialer.location.GeoUtil
import com.android.dialer.oem.MotorolaUtils
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.BAKLAVA])
@OptIn(ExperimentalCoroutinesApi::class)
class SystemDialIntentNumberTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        mockkStatic(MotorolaUtils::class)
        mockkStatic(GeoUtil::class)
        every { MotorolaUtils.shouldDisablePhoneNumberFormatting(any()) } returns false
        every { GeoUtil.getCurrentCountryIso(any()) } returns "US"
        shadowOf(application).grantPermissions(Manifest.permission.READ_CONTACTS)
    }

    @After
    fun tearDown() {
        unmockkStatic(MotorolaUtils::class)
        unmockkStatic(GeoUtil::class)
    }

    // region tel: links

    @Test
    fun formatsTheNumberInATelLink() = runTest {
        assertEquals("(650) 253-0000", numberFrom(dial("tel:6502530000")))
    }

    @Test
    fun viewingATelLinkFillsTheKeypadToo() = runTest {
        assertEquals(
            "(650) 253-0000",
            numberFrom(Intent(Intent.ACTION_VIEW, Uri.parse("tel:6502530000"))),
        )
    }

    @Test
    fun keepsThePauseAndWaitSuffixAsWritten() = runTest {
        assertEquals("(650) 253-0000,1234;9", numberFrom(dial("tel:6502530000,1234;9")))
    }

    @Test
    fun turnsLettersIntoTheirKeypadDigits() = runTest {
        assertEquals("1 800-356-9377", numberFrom(dial("tel:1-800-FLOWERS")))
    }

    @Test
    fun turnsUnicodeDigitsIntoAscii() = runTest {
        // Arabic-Indic digits, as a link written in Arabic would carry them.
        assertEquals("(650) 253-0000", numberFrom(dial("tel:٦٥٠٢٥٣٠٠٠٠")))
    }

    @Test
    fun anEmptyTelLinkCarriesNoNumber() = runTest {
        assertNull(numberFrom(dial("tel:")))
    }

    @Test
    fun ignoresAnIntentThatIsNotADialOrView() = runTest {
        assertNull(numberFrom(Intent(Intent.ACTION_CALL, Uri.parse("tel:6502530000"))))
    }

    @Test
    fun ignoresADialIntentWithoutData() = runTest {
        assertNull(numberFrom(Intent(Intent.ACTION_DIAL)))
    }

    @Test
    fun ignoresOtherSchemes() = runTest {
        assertNull(numberFrom(dial("sip:alice@example.com")))
    }

    // endregion

    // region legacy contact items

    @Test
    fun readsTheNumberOfAContactItem() = runTest {
        registerContacts(number = "6502530000")

        assertEquals("(650) 253-0000", numberFrom(contact(PERSON_ITEM_TYPE)))
    }

    @Test
    fun readsTheNumberOfAPhoneItem() = runTest {
        registerContacts(number = "6502530000")

        assertEquals("(650) 253-0000", numberFrom(contact(PHONE_ITEM_TYPE)))
    }

    @Test
    fun aContactItemWithNoRowCarriesNoNumber() = runTest {
        registerContacts(number = null)

        assertNull(numberFrom(contact(PERSON_ITEM_TYPE)))
    }

    @Test
    fun doesNotReadContactsWithoutPermission() = runTest {
        registerContacts(number = "6502530000")
        shadowOf(application).denyPermissions(Manifest.permission.READ_CONTACTS)

        assertNull(numberFrom(contact(PERSON_ITEM_TYPE)))
        assertEquals(0, FakeContactsProvider.queries)
    }

    @Test
    fun ignoresContentOfAnyOtherType() = runTest {
        registerContacts(number = "6502530000")

        assertNull(numberFrom(contact("vnd.android.cursor.item/contact")))
        assertEquals(0, FakeContactsProvider.queries)
    }

    @Test
    fun aContactUriThisAppMayNotReadCarriesNoNumber() = runTest {
        registerContacts(number = "6502530000", failure = SecurityException("not exported"))

        assertNull(numberFrom(contact(PERSON_ITEM_TYPE)))
    }

    @Test
    fun aContactUriNoProviderUnderstandsCarriesNoNumber() = runTest {
        registerContacts(number = "6502530000", failure = IllegalArgumentException("Unknown URI"))

        assertNull(numberFrom(contact(PERSON_ITEM_TYPE)))
    }

    // endregion

    private suspend fun numberFrom(intent: Intent): String? = SystemDialIntentNumber(
        context = application,
        ioDispatcher = UnconfinedTestDispatcher(),
    ).invoke(intent)

    private fun dial(uri: String) = Intent(Intent.ACTION_DIAL, Uri.parse(uri))

    private fun contact(type: String) =
        Intent(Intent.ACTION_DIAL).setDataAndType(Uri.parse("content://contacts/people/1"), type)

    private fun registerContacts(number: String?, failure: RuntimeException? = null) {
        FakeContactsProvider.number = number
        FakeContactsProvider.failure = failure
        FakeContactsProvider.queries = 0
        Robolectric.setupContentProvider(FakeContactsProvider::class.java, "contacts")
    }

    /** Stands in for the legacy Contacts provider, which Robolectric does not register. */
    class FakeContactsProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            queries++
            failure?.let { failure -> throw failure }
            return MatrixCursor(projection).apply {
                number?.let { number -> addRow(arrayOf(number, number.reversed())) }
            }
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ) = 0

        companion object {
            var number: String? = null
            var failure: RuntimeException? = null
            var queries = 0
        }
    }

    private companion object {
        private const val PERSON_ITEM_TYPE = "vnd.android.cursor.item/person"
        private const val PHONE_ITEM_TYPE = "vnd.android.cursor.item/phone"
    }
}
