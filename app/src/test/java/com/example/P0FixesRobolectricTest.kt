/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.Manifest
import android.app.Application
import android.os.Build
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DatabaseKeyManager
import com.example.model.BlockedNumber
import com.example.model.LabeledAddress
import com.example.model.LabeledEmail
import com.example.model.LabeledNumber
import com.example.model.SpamNumber
import com.example.util.BlockedNumberContractManager
import com.example.util.CallAudioRecorder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.security.ProviderException
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class P0FixesRobolectricTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `call blocking matches formatted numbers, suffixes, and wildcards cleanly`() {
        assertTrue(BlockedNumberContractManager.matchesRule("+1 (555) 123-4567", "5551234567"))
        assertTrue(BlockedNumberContractManager.matchesRule("+44 20 7946 0958", "02079460958"))
        assertTrue(BlockedNumberContractManager.matchesRule("+18005550199", "+1800*"))
        assertTrue(BlockedNumberContractManager.matchesRule("18005550199", "*800*"))
        assertFalse(BlockedNumberContractManager.matchesRule("+15551234567", "+1800*"))
    }

    @Test
    fun `matchesAnyRule bypasses emergency numbers even with broad wildcard rules`() {
        val rules = listOf("*911*", "112", "5551234567")
        assertFalse(BlockedNumberContractManager.matchesAnyRule("911", rules, context))
        assertFalse(BlockedNumberContractManager.matchesAnyRule("112", rules, context))
        assertTrue(BlockedNumberContractManager.matchesAnyRule("+1-555-123-4567", rules, context))
    }

    @Test
    fun `room dao isBlockedSql escapes literal SQL wildcards and screens spam`() = runBlocking {
        val db = AppDatabase.getDatabase(context)
        val dao = db.dialerDao()

        dao.insertBlockedNumber(BlockedNumber(number = "+1800*"))
        dao.insertBlockedNumber(BlockedNumber(number = "100% legit"))
        dao.insertSpamNumber(SpamNumber(number = "+18889990000", label = "Telemarketer"))

        assertTrue(dao.isBlockedSql("+18005551234"))
        assertFalse(dao.isBlockedSql("100999 legit"))
        assertTrue(BlockedNumberContractManager.shouldBlockIncomingCall(context, "+18889990000", dao))
        assertTrue(BlockedNumberContractManager.shouldBlockIncomingCall(context, "+1 (800) 555-1234", dao))
        assertFalse(BlockedNumberContractManager.shouldBlockIncomingCall(context, "911", dao))
    }

    @Test
    fun `database key manager distinguishes transient errors from permanent invalidation`() {
        assertFalse(
            DatabaseKeyManager.isPermanentKeyInvalidation(
                ProviderException("TEE busy"),
                keyExistsInStore = true
            )
        )
        assertTrue(
            DatabaseKeyManager.isPermanentKeyInvalidation(
                AEADBadTagException("Tag mismatch"),
                keyExistsInStore = true
            )
        )
        assertTrue(
            DatabaseKeyManager.isPermanentKeyInvalidation(
                ProviderException("Alias gone"),
                keyExistsInStore = false
            )
        )
    }

    @Test
    fun `quarantineDatabaseFiles preserves corrupted db and sidecars as backup instead of deleting`() {
        val tempDir = File(context.cacheDir, "quarantine_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            val dbFile = File(tempDir, "dialer_database")
            val walFile = File(tempDir, "dialer_database-wal")
            val shmFile = File(tempDir, "dialer_database-shm")
            dbFile.writeText("corrupt-db-bytes")
            walFile.writeText("corrupt-wal-bytes")
            shmFile.writeText("corrupt-shm-bytes")

            val backupName = AppDatabase.quarantineDatabaseFiles(dbFile)

            assertFalse("Original unreadable DB should be moved out", dbFile.exists())
            assertFalse("Original WAL should be moved out", walFile.exists())
            assertTrue("Quarantined backup DB must exist", File(tempDir, backupName).exists())
            assertTrue("Quarantined backup WAL must exist", File(tempDir, "$backupName-wal").exists())
            assertTrue("Quarantined backup SHM must exist", File(tempDir, "$backupName-shm").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `structured name splitting and in-place contact update ops preserve non-edited rows`() {
        val repo = DialerRepository(context)

        assertEquals("Ada" to "Lovelace", repo.splitFullNameParts("Ada Lovelace"))
        assertEquals("Ada Augusta" to "Lovelace", repo.splitFullNameParts("Ada Augusta Lovelace"))
        assertEquals("Ada" to "Lovelace", repo.splitFullNameParts("Lovelace, Ada"))
        assertEquals("Cher" to "", repo.splitFullNameParts("Cher"))

        val ops = repo.buildUpdateContactOps(
            rawContactId = 42L,
            existingNameDataId = 101L,
            displayName = "Ada Lovelace",
            givenName = "Ada",
            familyName = "Lovelace",
            numbers = listOf(LabeledNumber("+15551234567", "Mobile", isPrimary = true)),
            emails = listOf(LabeledEmail("ada@example.com", "Work")),
            addresses = listOf(LabeledAddress("12 Analytical Engine Way", "Work"))
        )

        // Must never delete the RawContact itself
        assertTrue(ops.none { it.uri == ContactsContract.RawContacts.CONTENT_URI && it.isDelete })
        // First operation updates existing StructuredName Data row in place
        assertTrue(ops[0].isUpdate)
        // Scoped delete only targets Phone, Email, StructuredPostal
        val deleteOp = ops[1]
        assertTrue(deleteOp.isDelete)
        assertEquals(ContactsContract.Data.CONTENT_URI, deleteOp.uri)
        // Subsequent inserts correspond to the 1 phone + 1 email + 1 address
        assertEquals(5, ops.size)
        assertTrue(ops.drop(2).all { it.isInsert })
    }

    @Test
    fun `recording timestamp formatter converts legacy epoch strings and keeps formatted strings`() {
        val epochString = "1704067200000"
        val formatted = CallAudioRecorder.formatStoredTimestampForDisplay(context, epochString)
        assertFalse("Epoch string must be converted to human-readable date", formatted.all { it.isDigit() })
        assertTrue("Formatted timestamp should contain time separator", formatted.contains(":"))

        val alreadyFormatted = "Jan 1, 14:30"
        assertEquals(alreadyFormatted, CallAudioRecorder.formatStoredTimestampForDisplay(context, alreadyFormatted))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `android 9 requires runtime WRITE_EXTERNAL_STORAGE check before Downloads export`() {
        val shadowApp = Shadows.shadowOf(context)
        shadowApp.denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        assertFalse(CallAudioRecorder.hasLegacyStorageWritePermission(context))

        shadowApp.grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        assertTrue(CallAudioRecorder.hasLegacyStorageWritePermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `android 14+ uses scoped MediaStore without legacy WRITE_EXTERNAL_STORAGE permission`() {
        assertTrue(CallAudioRecorder.hasLegacyStorageWritePermission(context))
    }
}
