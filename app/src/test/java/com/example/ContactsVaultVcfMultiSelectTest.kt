/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.BackupRestoreManager
import com.example.model.Contact
import com.example.model.LabeledAddress
import com.example.model.LabeledEmail
import com.example.model.LabeledNumber
import com.example.model.PRIVATE_VAULT_ACCOUNT_NAME
import com.example.model.PRIVATE_VAULT_ACCOUNT_TYPE
import com.example.model.PrivateContact
import com.example.ui.components.AddContactDialog
import com.example.ui.components.ContactsTabContent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsVaultVcfMultiSelectTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        val db = AppDatabase.getDatabase(app)
        db.clearAllTables()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `vcf parser extracts full fields including quoted printable utf8 notes and email-only cards`() {
        val sampleVcf = "\uFEFF" + """
            BEGIN:VCARD
            VERSION:3.0
            N:Doe;Jane;Q;;
            FN:Jane Q. Doe
            item1.TEL;TYPE=CELL:+1 555 234 5678
            item1.X-ABLabel:Signal
            EMAIL;TYPE=WORK:jane.doe@example.org
            ADR;TYPE=HOME:;;742 Evergreen Terrace;Springfield;OR;97477;USA
            ORG:Privacy Labs;Engineering
            TITLE:Chief Architect
            ROLE:Lead Auditor
            BDAY:1992-08-14
            URL:https://privacylabs.example
            NICKNAME:Janie
            IMPP:xmpp:jane@jabber.example
            CATEGORIES:VIP,Security
            NOTE;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:Met at M=C3=BCnchen conference=0A=
            Security badge #42=3B verify PGP key.
            END:VCARD
            BEGIN:VCARD
            VERSION:2.1
            FN:Email Only Partner
            EMAIL;HOME:partner@onionmail.example
            NOTE:Reached only via encrypted mail\, never phone.
            END:VCARD
        """.trimIndent()

        val parsed = BackupRestoreManager.parseVcfRecords(sampleVcf)
        assertEquals(2, parsed.size)

        val first = parsed[0]
        assertEquals("Jane Q. Doe", first.name)
        assertEquals(1, first.numbers.size)
        assertEquals("+1 555 234 5678", first.numbers[0].number)
        assertEquals("Signal", first.numbers[0].label)
        assertEquals("jane.doe@example.org", first.emails[0].email)
        assertTrue(first.addresses[0].address.contains("742 Evergreen Terrace"))
        assertTrue(first.note.contains("Met at München conference"))
        assertTrue(first.note.contains("Security badge #42; verify PGP key."))
        assertTrue(first.note.contains("Company: Privacy Labs"))
        assertTrue(first.note.contains("Title: Chief Architect"))
        assertTrue(first.note.contains("Birthday: 1992-08-14"))
        assertTrue(first.note.contains("Website: https://privacylabs.example"))
        assertEquals("Privacy Labs", first.organization)
        assertEquals("Chief Architect", first.title)

        val second = parsed[1]
        assertEquals("Email Only Partner", second.name)
        assertTrue(second.numbers.isEmpty())
        assertEquals("partner@onionmail.example", second.emails[0].email)
        assertTrue(second.note.contains("Reached only via encrypted mail, never phone."))
    }

    @Test
    fun `vcf export round trip preserves notes, emails, addresses and private contacts`() = runBlocking {
        val contacts = listOf(
            Contact(
                id = -10L,
                number = "+1 555 888 0001",
                name = "Vault Operative",
                label = "Mobile",
                favorite = true,
                avatarText = "VO",
                avatarBgValue = 0xFF225588L,
                avatarTextColorValue = 0xFFFFFFFFL,
                email = "op@vault.local",
                accountName = PRIVATE_VAULT_ACCOUNT_NAME,
                accountType = PRIVATE_VAULT_ACCOUNT_TYPE,
                numbers = listOf(LabeledNumber("+1 555 888 0001", "Mobile", true)),
                emails = listOf(LabeledEmail("op@vault.local", "Work")),
                addresses = listOf(LabeledAddress("123 Hidden Ave, Zurich", "Home")),
                note = "Use Signal only; code red\nSecond line note"
            )
        )

        val vcf = BackupRestoreManager.exportContactsToVcf(app, contacts)
        assertTrue(vcf.contains("NOTE:Use Signal only\\; code red\\nSecond line note"))
        assertTrue(vcf.contains("X-SECUREDIALER-STORAGE:PRIVATE_VAULT"))

        val reParsed = BackupRestoreManager.parseVcfRecords(vcf)
        assertEquals(1, reParsed.size)
        assertEquals("Vault Operative", reParsed[0].name)
        assertEquals("+1 555 888 0001", reParsed[0].numbers.first().number)
        assertEquals("op@vault.local", reParsed[0].emails.first().email)
        assertTrue(reParsed[0].addresses.first().address.contains("123 Hidden Ave"))
        assertEquals("Use Signal only; code red\nSecond line note", reParsed[0].note)
    }

    @Test
    fun `private vault contacts isolate from system table, resolve caller id, and favorite properly`() = runBlocking {
        val db = AppDatabase.getDatabase(app)
        val dao = db.dialerDao()
        val repo = DialerRepository(app)

        repo.addContactWithDetails(
            name = "Dr. Ghost",
            numbers = listOf(LabeledNumber("+1 555 901 2233", "Work", true)),
            emails = listOf(LabeledEmail("ghost@clinic.local", "Work")),
            addresses = listOf(LabeledAddress("Floor 4, Bunker B", "Work")),
            note = "VIP confidentiality protocol",
            saveToPrivateVault = true,
            syncAfter = false
        )

        // Must stay isolated from system cache table
        assertEquals(0, dao.getContactsCount())
        assertEquals(1, dao.getPrivateContactsCount())

        // Merged stream exposes negative ID + isPrivateVault = true + note
        val allMerged = dao.getAllContactsList()
        assertEquals(1, allMerged.size)
        val ghost = allMerged.first()
        assertTrue(ghost.id < 0L)
        assertTrue(ghost.isPrivateVault)
        assertEquals("VIP confidentiality protocol", ghost.note)
        assertEquals("Dr. Ghost", ghost.name)

        // Caller ID resolution finds Private Vault contact
        val callerIdMatch = dao.getContactByNumber("5559012233")
        assertNotNull(callerIdMatch)
        assertEquals("Dr. Ghost", callerIdMatch?.name)
        assertEquals("VIP confidentiality protocol", callerIdMatch?.note)

        // Favorite toggle routes cleanly to private_contacts
        repo.toggleFavorite(ghost, true)
        val favs = dao.getFavoriteContacts().first()
        assertEquals(1, favs.size)
        assertTrue(favs.first().isPrivateVault)
    }

    @Test
    fun `encrypted json backup v3 preserves private contacts end to end`() = runBlocking {
        val db = AppDatabase.getDatabase(app)
        val dao = db.dialerDao()

        dao.insertPrivateContact(
            PrivateContact(
                name = "Encrypted Alice",
                number = "+44 20 7946 0011",
                label = "Mobile",
                favorite = true,
                avatarText = "EA",
                avatarBgValue = 0xFF334455L,
                avatarTextColorValue = 0xFFFFFFFFL,
                t9Mapping = "36279783325423",
                email = "alice@proton.local",
                numbers = listOf(LabeledNumber("+44 20 7946 0011", "Mobile", true)),
                emails = listOf(LabeledEmail("alice@proton.local", "Home")),
                addresses = emptyList(),
                note = "Passphrase hint: alpine cedar"
            )
        )

        val secret = "RazrFoldAndroid16Vault!"
        val payload = BackupRestoreManager.exportBackup(app, password = secret)
        assertFalse(payload.contains("Encrypted Alice"))
        assertFalse(payload.contains("alpine cedar"))

        db.clearAllTables()
        assertEquals(0, dao.getPrivateContactsCount())

        val restored = BackupRestoreManager.importBackup(app, payload, password = secret)
        assertTrue(restored)

        val privateList = dao.getAllPrivateContactsList()
        assertEquals(1, privateList.size)
        assertEquals("Encrypted Alice", privateList.first().name)
        assertEquals("Passphrase hint: alpine cedar", privateList.first().note)
        assertTrue(privateList.first().favorite)
    }

    @Test
    fun `batch delete operations skip private vault and bulk delete removes private contacts cleanly`() = runBlocking {
        val db = AppDatabase.getDatabase(app)
        val dao = db.dialerDao()
        val repo = DialerRepository(app)

        val id1 = dao.insertPrivateContact(
            PrivateContact(
                name = "Vault One",
                number = "5550001",
                label = "Mobile",
                avatarText = "V1",
                avatarBgValue = 0L,
                avatarTextColorValue = 0L,
                note = "Note 1"
            )
        )
        val id2 = dao.insertPrivateContact(
            PrivateContact(
                name = "Vault Two",
                number = "5550002",
                label = "Work",
                avatarText = "V2",
                avatarBgValue = 0L,
                avatarTextColorValue = 0L,
                note = "Note 2"
            )
        )
        assertEquals(2, dao.getPrivateContactsCount())

        val contactsToDelete = dao.getAllContactsList()
        val fakeSystemContact = Contact(
            id = 500L,
            rawContactId = 9001L,
            contactId = 8001L,
            number = "5550003",
            name = "System Contact",
            label = "Mobile",
            avatarText = "SC",
            avatarBgValue = 0L,
            avatarTextColorValue = 0L
        )
        val ops = repo.buildBatchDeleteOperations(contactsToDelete + fakeSystemContact)
        // Only the 1 system contact should generate a ContentProviderOperation
        assertEquals(1, ops.size)

        val deletedCount = repo.deleteContacts(contactsToDelete)
        assertEquals(2, deletedCount)
        assertEquals(0, dao.getPrivateContactsCount())
        assertTrue(id1 > 0L && id2 > 0L)
    }

    @Test
    fun `room migration 9 to 10 creates private_contacts table without data loss`() {
        val dbName = "migration_9_10_test.db"
        app.deleteDatabase(dbName)

        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(app)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS call_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, number TEXT NOT NULL, note TEXT NOT NULL, lastUpdated INTEGER NOT NULL)")
                    db.execSQL("INSERT INTO call_notes (number, note, lastUpdated) VALUES ('5551234', 'Existing v9 note', 1000)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val sqliteDb = helperFactory.create(config).writableDatabase
        AppDatabase.MIGRATION_9_10.migrate(sqliteDb)

        // Verify private_contacts exists and accepts inserts + note query succeeds
        sqliteDb.execSQL(
            "INSERT INTO private_contacts (number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note) VALUES ('5559999', 'Post Migration', 'Mobile', 0, 'PM', 0, 0, '', '', '', '__secure_private_vault__', 'com.securedialer.privatevault', '[]', '[]', '[]', 'Migrated Note')"
        )
        sqliteDb.query("SELECT note FROM call_notes WHERE number = '5551234'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Existing v9 note", cursor.getString(0))
        }
        sqliteDb.query("SELECT name, note FROM private_contacts").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Post Migration", cursor.getString(0))
            assertEquals("Migrated Note", cursor.getString(1))
        }
        sqliteDb.close()
        app.deleteDatabase(dbName)
    }

    @Test
    fun `add contact dialog saves private vault storage and note on compact 320dp cover screen`() {
        var capturedAccountType = ""
        var capturedNote = ""
        var capturedName = ""

        composeRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.requiredSize(320.dp, 640.dp)) {
                    AddContactDialog(
                        initialName = "",
                        initialNumber = "",
                        initialLabel = "Mobile",
                        selectedAccountFilter = PRIVATE_VAULT_ACCOUNT_NAME,
                        onDismiss = {},
                        onConfirmWithFullDetails = { name, _, _, _, _, accType, note ->
                            capturedName = name
                            capturedAccountType = accType
                            capturedNote = note
                        }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("dialog_account_selector").assertIsDisplayed()
        composeRule.onNodeWithTag("dialog_fields_list").performScrollToNode(hasTestTag("dialog_first_name_input"))
        composeRule.onNodeWithTag("dialog_first_name_input").performTextInput("Clara")
        composeRule.onNodeWithTag("dialog_fields_list").performScrollToNode(hasTestTag("dialog_phone_input_0"))
        composeRule.onNodeWithTag("dialog_phone_input_0").performTextInput("5557778888")
        composeRule.onNodeWithTag("dialog_fields_list").performScrollToNode(hasTestTag("dialog_add_notes_btn"))
        composeRule.onNodeWithTag("dialog_add_notes_btn").performClick()
        composeRule.onNodeWithTag("dialog_fields_list").performScrollToNode(hasTestTag("dialog_note_input"))
        composeRule.onNodeWithTag("dialog_note_input").performTextInput("Meet at Gate 7")
        composeRule.onNodeWithTag("dialog_save_contact_button").performClick()

        assertEquals("Clara", capturedName)
        assertEquals(PRIVATE_VAULT_ACCOUNT_TYPE, capturedAccountType)
        assertEquals("Meet at Gate 7", capturedNote)
    }

    @Test
    fun `contacts tab supports zero permission private vault and multi-select bulk delete`() {
        // Explicitly revoke READ_CONTACTS to verify privacy zero-permission workflow
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

        val db = AppDatabase.getDatabase(app)
        val dao = db.dialerDao()
        runBlocking {
            dao.insertPrivateContact(
                PrivateContact(
                    name = "Alpha Vault",
                    number = "5551110001",
                    label = "Mobile",
                    avatarText = "AV",
                    avatarBgValue = 0xFF336699L,
                    avatarTextColorValue = 0xFFFFFFFFL,
                    note = "Alpha secret note"
                )
            )
            dao.insertPrivateContact(
                PrivateContact(
                    name = "Beta Vault",
                    number = "5551110002",
                    label = "Work",
                    avatarText = "BV",
                    avatarBgValue = 0xFF663399L,
                    avatarTextColorValue = 0xFFFFFFFFL,
                    note = "Beta secret note"
                )
            )
        }
        shadowOf(Looper.getMainLooper()).idle()

        val viewModel = DialerViewModel(app)
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.requiredSize(360.dp, 800.dp)) {
                    val paged = viewModel.contactsPaged.collectAsLazyPagingItems()
                    val favs = emptyList<Contact>()
                    ContactsTabContent(
                        viewModel = viewModel,
                        contactsPaged = paged,
                        favoriteContacts = favs,
                        onCallClick = {},
                        onAddContactClick = {},
                        onToggleFavorite = {},
                        hasPermission = false,
                        isLoading = false,
                        onRequestPermission = {},
                        onEditContact = {},
                        onDeleteContact = {}
                    )
                }
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithText("Alpha Vault").fetchSemanticsNodes().isNotEmpty()
        }
        // Even with READ_CONTACTS denied, permission banner AND private vault contacts + select button render cleanly
        composeRule.onNodeWithTag("contacts_permission_card").assertIsDisplayed()
        composeRule.onNodeWithTag("add_contact_fab").assertIsDisplayed()
        composeRule.onNodeWithTag("multi_select_toggle_btn").assertIsDisplayed()
        composeRule.onNodeWithText("Alpha Vault").assertIsDisplayed()
        composeRule.onNodeWithText("Beta Vault").assertIsDisplayed()

        // Enter Multi-Select, Select All, Bulk Delete, Confirm
        composeRule.onNodeWithTag("multi_select_toggle_btn").performClick()
        composeRule.onNodeWithTag("multi_select_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("multi_select_all_btn").performClick()
        composeRule.onNodeWithTag("multi_select_delete_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("confirm_delete_contacts_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("confirm_delete_contacts_btn").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            runBlocking { dao.getPrivateContactsCount() == 0 }
        }

        assertEquals(0, runBlocking { dao.getPrivateContactsCount() })
    }
}
