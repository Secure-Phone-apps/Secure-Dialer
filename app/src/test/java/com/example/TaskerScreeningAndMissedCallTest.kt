package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.components.RecentsFilter
import com.example.ui.components.groupCallRecords
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.MissedCallNotificationReceiver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskerScreeningAndMissedCallTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
    }

    private fun makeRecord(
        id: Int,
        number: String,
        name: String = number.ifEmpty { "Unknown" },
        type: CallType = CallType.INCOMING,
        timestampMs: Long = 1_700_000_000_000L + id * 1000L,
        numberPresentation: Int = CallLog.Calls.PRESENTATION_ALLOWED,
        blockReason: Int = 0,
        callScreeningAppName: String = ""
    ) = CallRecord(
        id = id,
        name = name,
        number = number,
        label = "Mobile",
        timestamp = "12:00",
        type = type,
        avatarText = "T",
        avatarBgValue = Color.Gray.value.toLong(),
        avatarTextColorValue = Color.White.value.toLong(),
        duration = 0L,
        hasVoicemail = false,
        timestampMs = timestampMs,
        numberPresentation = numberPresentation,
        blockReason = blockReason,
        callScreeningAppName = callScreeningAppName
    )

    @Test
    fun verifySystemCallTypeMappingIncludesRejectedAndBlocked() {
        assertEquals(CallType.REJECTED, mapSystemCallType(CallLog.Calls.REJECTED_TYPE))
        assertEquals(CallType.BLOCKED, mapSystemCallType(CallLog.Calls.BLOCKED_TYPE))
        assertEquals(CallType.MISSED, mapSystemCallType(CallLog.Calls.MISSED_TYPE))
        assertEquals(CallType.OUTGOING, mapSystemCallType(CallLog.Calls.OUTGOING_TYPE))
        assertEquals(CallType.INCOMING, mapSystemCallType(CallLog.Calls.INCOMING_TYPE))
    }

    @Test
    fun verifyConsecutiveGroupingAndUnknownNonCollapse() {
        // Unknown / Tasker-screened blank numbers must NEVER collapse into a single "Unknown (N)" group
        val records = listOf(
            makeRecord(1, "", "Unknown", CallType.REJECTED, timestampMs = 5000L, numberPresentation = CallLog.Calls.PRESENTATION_RESTRICTED, callScreeningAppName = "Tasker"),
            makeRecord(2, "-1", "Unknown", CallType.REJECTED, timestampMs = 4000L, numberPresentation = CallLog.Calls.PRESENTATION_UNKNOWN, callScreeningAppName = "Tasker"),
            makeRecord(3, "", "Unknown", CallType.BLOCKED, timestampMs = 3000L, callScreeningAppName = "Tasker"),
            makeRecord(4, "-2", "Unknown", CallType.MISSED, timestampMs = 2000L)
        )

        val groupedAll = groupCallRecords(records, RecentsFilter.ALL, "")
        assertEquals(4, groupedAll.size)
        groupedAll.forEach { group ->
            assertEquals(1, group.calls.size)
        }

        val groupedBlocked = groupCallRecords(records, RecentsFilter.BLOCKED, "")
        assertEquals(3, groupedBlocked.size)

        val groupedReceived = groupCallRecords(records, RecentsFilter.RECEIVED, "")
        assertTrue(groupedReceived.isEmpty())

        val searchedByScreener = groupCallRecords(records, RecentsFilter.ALL, "Tasker")
        assertEquals(3, searchedByScreener.size)
    }

    @Test
    fun verifyConsecutiveSameNumberSameTypeMergesButInterleavedSplits() {
        val records = listOf(
            // Newest: two consecutive REJECTED calls from +15551234567 (different formatting)
            makeRecord(10, "+15551234567", "Spammer", CallType.REJECTED, timestampMs = 5000L),
            makeRecord(11, "555-123-4567", "Spammer", CallType.REJECTED, timestampMs = 4000L),
            // Same number but INCOMING earlier -> must NOT merge with REJECTED
            makeRecord(12, "+15551234567", "Spammer", CallType.INCOMING, timestampMs = 3000L),
            // Interleaved outgoing call to Bob
            makeRecord(13, "+15559990000", "Bob", CallType.OUTGOING, timestampMs = 2000L),
            // Earlier REJECTED call from Spammer separated by Bob & Incoming -> must NOT merge
            makeRecord(14, "+15551234567", "Spammer", CallType.REJECTED, timestampMs = 1000L)
        )

        val grouped = groupCallRecords(records, RecentsFilter.ALL, "")
        assertEquals(4, grouped.size)
        assertEquals(2, grouped[0].calls.size)
        assertEquals(CallType.REJECTED, grouped[0].primary.type)
        assertEquals(1, grouped[1].calls.size)
        assertEquals(CallType.INCOMING, grouped[1].primary.type)
        assertEquals(1, grouped[2].calls.size)
        assertEquals("Bob", grouped[2].primary.name)
        assertEquals(1, grouped[3].calls.size)
        assertEquals(CallType.REJECTED, grouped[3].primary.type)
    }

    @Test
    fun verifyMissedCallNotificationReceiverPostsAndCancelsCorrectly() {
        val receiver = MissedCallNotificationReceiver()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = shadowOf(nm)

        // Post notification for 2 missed calls
        val showIntent = Intent(TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION).apply {
            putExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 2)
            putExtra(TelecomManager.EXTRA_NOTIFICATION_PHONE_NUMBER, "5550199")
        }
        receiver.onReceive(context, showIntent)
        Thread.sleep(150)
        shadowOf(Looper.getMainLooper()).idle()

        val posted = shadowNm.getNotification(MyInCallService.NOTIFICATION_ID_MISSED)
        assertNotNull("Expected missed call notification to be posted", posted)
        val contentIntent = shadowOf(posted.contentIntent).savedIntent
        assertEquals(Intent.ACTION_VIEW, contentIntent.action)
        assertEquals(CallLog.Calls.CONTENT_TYPE, contentIntent.type)
        assertTrue(contentIntent.getBooleanExtra("SHOW_CALL_LOG", false))

        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getLong(MissedCallNotificationReceiver.PREF_LAST_TELECOM_MISSED_BROADCAST_MS, 0L) > 0L)

        // Receiving count = 0 must cancel notification cleanly
        val clearIntent = Intent(TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION).apply {
            putExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 0)
        }
        receiver.onReceive(context, clearIntent)
        assertNull("Expected missed call notification to be cancelled when count=0", shadowNm.getNotification(MyInCallService.NOTIFICATION_ID_MISSED))
    }

    @Test
    fun verifyManifestResolvesCallLogViewIntentAndTelecomBroadcast() {
        val pm = context.packageManager

        val viewDirIntent = Intent(Intent.ACTION_VIEW).apply {
            type = CallLog.Calls.CONTENT_TYPE
        }
        val activities = pm.queryIntentActivities(viewDirIntent, 0)
        assertTrue(
            "MainActivity must resolve ACTION_VIEW vnd.android.cursor.dir/calls",
            activities.any { it.activityInfo.name == MainActivity::class.java.name }
        )

        val missedBroadcastIntent = Intent(TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION)
        val receivers = pm.queryBroadcastReceivers(missedBroadcastIntent, 0)
        assertTrue(
            "MissedCallNotificationReceiver must resolve ACTION_SHOW_MISSED_CALLS_NOTIFICATION",
            receivers.any { it.activityInfo.name == MissedCallNotificationReceiver::class.java.name }
        )
    }

    @Test
    fun verifyMainActivityOpensRecentsTabForActionViewCallLog() {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            type = CallLog.Calls.CONTENT_TYPE
        }
        val controller = Robolectric.buildActivity(MainActivity::class.java, launchIntent).setup()
        val activity = controller.get()
        shadowOf(Looper.getMainLooper()).idle()

        val vm = ViewModelProvider(activity)[DialerViewModel::class.java]
        val recentsTabIndex = listOf(
            vm.tabSlotLeft.value,
            vm.tabSlotMiddle.value,
            vm.tabSlotRight.value
        ).indexOf("RECENTS").coerceAtLeast(0)

        assertEquals(recentsTabIndex, vm.selectedTab.value)
        controller.pause().stop().destroy()
    }

    @Test
    fun verifyDatabaseMigration10To11AddsScreeningColumnsSafely() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val callback = object : SupportSQLiteOpenHelper.Callback(10) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS call_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        number TEXT NOT NULL,
                        label TEXT NOT NULL,
                        timestamp TEXT NOT NULL,
                        type TEXT NOT NULL,
                        avatarText TEXT NOT NULL,
                        avatarBgValue INTEGER NOT NULL,
                        avatarTextColorValue INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        hasVoicemail INTEGER NOT NULL,
                        photoUri TEXT NOT NULL,
                        timestampMs INTEGER NOT NULL,
                        isVerified INTEGER NOT NULL,
                        simSlot INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO call_history (name, number, label, timestamp, type, avatarText, avatarBgValue, avatarTextColorValue, duration, hasVoicemail, photoUri, timestampMs, isVerified, simSlot)
                    VALUES ('Alice', '5551234', 'Mobile', '10:00', 'MISSED', 'A', 1, 2, 0, 0, '', 1700000000000, 0, 1)
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = helperFactory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null) // in-memory SQLite DB
                .callback(callback)
                .build()
        )
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_10_11.migrate(db)

        db.query("SELECT numberPresentation, blockReason, callScreeningAppName FROM call_history WHERE name = 'Alice'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals("", cursor.getString(2))
        }
        helper.close()
    }
}
