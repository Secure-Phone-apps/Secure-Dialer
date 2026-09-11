package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.BackupRestoreManager
import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialerBackupIntegrityTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testUnencryptedBackupAndRestoreIntegrity() = runBlocking {
        val db = AppDatabase.getDatabase(context)
        val dao = db.dialerDao()

        // 1. Clean database state before test
        db.clearAllTables()
        shadowOf(Looper.getMainLooper()).idle()

        // 2. Insert dummy records representing rich user configuration state
        dao.insertBlockedNumber(BlockedNumber("555123456"))
        dao.insertSpeedDial(SpeedDial(2, "555987654", "Emergency Mom"))
        dao.insertQuickResponse(QuickResponse(message = "Can't talk now, in a secure meeting."))
        dao.insertSetting(AppSetting("theme_preference", "dark_mode"))
        dao.insertCallNote(CallNote(number = "555111222", note = "Architectural Review Completed", lastUpdated = System.currentTimeMillis()))

        shadowOf(Looper.getMainLooper()).idle()

        // 3. Export unencrypted backup JSON representation
        val backupPayload = BackupRestoreManager.exportBackup(context, password = "")
        assertNotNull(backupPayload)
        assertTrue(backupPayload.contains("555123456"))
        assertTrue(backupPayload.contains("Emergency Mom"))
        assertTrue(backupPayload.contains("secure meeting"))
        assertTrue(backupPayload.contains("dark_mode"))

        // 4. Wipe/purge database to simulate complete storage wipe or device migration
        db.clearAllTables()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert empty state
        assertTrue(dao.getBlockedNumbersList().isEmpty())
        assertTrue(dao.getSpeedDialList().isEmpty())
        assertTrue(dao.getQuickResponsesList().isEmpty())
        assertTrue(dao.getAllSettingsList().isEmpty())
        assertTrue(dao.getAllCallNotesList().isEmpty())

        // 5. Restore the unencrypted backup payload
        val importSuccess = BackupRestoreManager.importBackup(context, backupPayload, password = "")
        assertTrue(importSuccess)
        shadowOf(Looper.getMainLooper()).idle()

        // 6. Assert all recovered values match original specifications precisely (Zero-loss guarantee)
        val recoveredBlocked = dao.getBlockedNumbersList()
        assertEquals(1, recoveredBlocked.size)
        assertEquals("555123456", recoveredBlocked[0].number)

        val recoveredSpeed = dao.getSpeedDialList()
        assertEquals(1, recoveredSpeed.size)
        assertEquals(2, recoveredSpeed[0].key)
        assertEquals("Emergency Mom", recoveredSpeed[0].name)
        assertEquals("555987654", recoveredSpeed[0].number)

        val recoveredResponses = dao.getQuickResponsesList()
        assertTrue(recoveredResponses.any { it.message.contains("secure meeting") })

        val recoveredSettings = dao.getAllSettingsList()
        assertEquals("dark_mode", recoveredSettings.find { it.key == "theme_preference" }?.value)

        val recoveredNotes = dao.getAllCallNotesList()
        assertEquals("Architectural Review Completed", recoveredNotes.find { it.number == "555111222" }?.note)
    }

    @Test
    fun testEncryptedBackupAndRestoreIntegrity() = runBlocking {
        val db = AppDatabase.getDatabase(context)
        val dao = db.dialerDao()

        // 1. Reset state
        db.clearAllTables()
        shadowOf(Looper.getMainLooper()).idle()

        // 2. Insert secure dummy details
        dao.insertBlockedNumber(BlockedNumber("555999888"))
        dao.insertSpeedDial(SpeedDial(3, "555777666", "Confidential Partner"))
        dao.insertSetting(AppSetting("encryption_flag", "true"))
        shadowOf(Looper.getMainLooper()).idle()

        // 3. Export encrypted backup JSON representation
        val securePassword = "ChiefSecurityAuditorPassword2026"
        val encryptedPayload = BackupRestoreManager.exportBackup(context, password = securePassword)
        assertNotNull(encryptedPayload)

        // Ensure payload is encrypted (does not expose plain-text fields/numbers)
        assertFalse(encryptedPayload.contains("555999888"))
        assertFalse(encryptedPayload.contains("Confidential Partner"))
        assertFalse(encryptedPayload.contains("encryption_flag"))

        // 4. Wipe database
        db.clearAllTables()
        shadowOf(Looper.getMainLooper()).idle()

        // 5. Attempt restore with INCORRECT decryption key (should return false and block restore)
        val incorrectRestoreResult = BackupRestoreManager.importBackup(context, encryptedPayload, password = "WrongDecryptionPassword")
        assertFalse(incorrectRestoreResult)

        // Ensure database remains pristine and uncompromised
        assertTrue(dao.getBlockedNumbersList().isEmpty())
        assertTrue(dao.getSpeedDialList().isEmpty())

        // 6. Restore with the CORRECT decryption key
        val correctRestoreResult = BackupRestoreManager.importBackup(context, encryptedPayload, password = securePassword)
        assertTrue(correctRestoreResult)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert full recovered state matches
        val recoveredBlocked = dao.getBlockedNumbersList()
        assertEquals("555999888", recoveredBlocked[0].number)

        val recoveredSpeed = dao.getSpeedDialList()
        assertEquals("Confidential Partner", recoveredSpeed[0].name)
    }
}
