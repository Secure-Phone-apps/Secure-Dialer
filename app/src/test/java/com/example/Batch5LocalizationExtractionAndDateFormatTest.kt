package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.util.CallAudioRecorder
import com.example.util.MissedCallNotificationReceiver
import com.example.util.RecordingCompressionProfile
import com.example.util.UssdManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Batch5LocalizationExtractionAndDateFormatTest {

    private fun localizedContext(base: Context, langTag: String): Context {
        val locale = Locale.forLanguageTag(langTag)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    @After
    fun tearDown() {
        UssdManager.dismissDialog()
    }

    @Test
    fun batch5ExtractedStringsResolveAndFormatAcrossLocales() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val enCtx = localizedContext(base, "en")

        val testLocales = listOf("de", "ar", "ja", "tr", "fr", "es", "hi", "pl", "pt", "zh")
        for (lang in testLocales) {
            val ctx = localizedContext(base, lang)

            // Task 5.1: Settings search categories & items resolve cleanly
            assertNotEquals(
                "search_cat_appearance should be localized for $lang",
                enCtx.getString(R.string.search_cat_appearance),
                ctx.getString(R.string.search_cat_appearance)
            )
            assertTrue(ctx.getString(R.string.search_item_biometric_app_lock).isNotBlank())
            assertTrue(ctx.getString(R.string.search_desc_biometric_app_lock).isNotBlank())

            // Task 5.2: Biometric, Privacy Tabs, Post-Call, USSD & Encrypted Backup
            assertTrue(ctx.getString(R.string.biometric_enable_title).isNotBlank())
            assertTrue(ctx.getString(R.string.biometric_disable_title).isNotBlank())
            assertTrue(ctx.getString(R.string.privacy_tab_security).isNotBlank())
            assertTrue(ctx.getString(R.string.privacy_tab_contacts_source).isNotBlank())
            assertTrue(ctx.getString(R.string.backup_encrypted_prompt).isNotBlank())

            val postCallMinSec = ctx.getString(R.string.post_call_ended_min_sec, 2, 15)
            assertTrue("post_call_ended_min_sec missing values in $lang: $postCallMinSec", postCallMinSec.contains(Regex("[2٢]")) && postCallMinSec.contains(Regex("(15|١٥)")))
            val postCallSec = ctx.getString(R.string.post_call_ended_sec, 42)
            assertTrue("post_call_ended_sec missing value in $lang: $postCallSec", postCallSec.contains(Regex("(42|٤٢)")))
            assertTrue(ctx.getString(R.string.post_call_sms_chip).isNotBlank())

            assertTrue(ctx.getString(R.string.ussd_device_info_title).isNotBlank())
            assertTrue(ctx.getString(R.string.ussd_imei_line, 1, "123456789012345").contains("123456789012345"))
            assertTrue(ctx.getString(R.string.ussd_failed_message, "Error", "*123#").contains("*123#"))

            // Task 5.3: Notifications, Compression Profiles, Badge & Toasts
            assertTrue(ctx.getString(R.string.notif_incoming_call_from, "Alice").contains("Alice"))
            assertTrue(ctx.getString(R.string.notif_title_recording_suffix, "Bob").contains("Bob"))
            assertTrue(ctx.getString(R.string.notif_conference_active_text, 3).contains(Regex("[3٣]")))
            assertTrue(ctx.getString(R.string.notif_channel_incoming_name).isNotBlank())
            assertTrue(ctx.getString(R.string.badge_rec).isNotBlank())
            assertTrue(ctx.getString(R.string.toast_mic_permission_granted).isNotBlank())
            assertTrue(ctx.getString(R.string.toast_mic_permission_required).isNotBlank())
        }
    }

    @Test
    fun ussdManagerProducesLocalizedDeviceInfoTitle() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val enCtx = localizedContext(base, "en")
        assertTrue(UssdManager.handleUssdOrMmi(enCtx, "*#06#"))
        assertEquals("Device Information", UssdManager.ussdDialogData.value?.title)
        UssdManager.dismissDialog()

        val deCtx = localizedContext(base, "de")
        assertTrue(UssdManager.handleUssdOrMmi(deCtx, "*#06#"))
        val deTitle = UssdManager.ussdDialogData.value?.title
        assertNotNull(deTitle)
        assertNotEquals("Device Information", deTitle)
    }

    @Test
    fun compressionProfilesExposeLocalizedStringResources() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val enCtx = localizedContext(base, "en")
        val frCtx = localizedContext(base, "fr")

        for (profile in RecordingCompressionProfile.entries) {
            val enTitle = enCtx.getString(profile.titleRes)
            val frTitle = frCtx.getString(profile.titleRes)
            val enDesc = enCtx.getString(profile.descriptionRes)
            val frDesc = frCtx.getString(profile.descriptionRes)

            assertTrue(enTitle.isNotBlank())
            assertTrue(frTitle.isNotBlank())
            assertTrue(enDesc.isNotBlank())
            assertTrue(frDesc.isNotBlank())
            assertNotEquals("French compression title should differ for ${profile.key}", enTitle, frTitle)
        }
    }

    @Test
    fun formatShortDateTimeRespectsSystem12hAnd24hSettings() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val enCtx = localizedContext(base, "en-US")

        val cal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.MAY, 15, 17, 5, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val testTimeMs = cal.timeInMillis

        Settings.System.putString(enCtx.contentResolver, Settings.System.TIME_12_24, "24")
        val formatted24 = CallAudioRecorder.formatShortDateTime(enCtx, testTimeMs)
        assertTrue("Expected 24-hour '17:05' pattern in '$formatted24'", formatted24.contains("17"))
        assertFalse("Did not expect AM/PM marker in 24-hour '$formatted24'", formatted24.contains("PM", ignoreCase = true))

        Settings.System.putString(enCtx.contentResolver, Settings.System.TIME_12_24, "12")
        val formatted12 = CallAudioRecorder.formatShortDateTime(enCtx, testTimeMs)
        assertTrue("Expected 12-hour '5:05' pattern in '$formatted12'", formatted12.contains("5:05"))
        assertTrue("Expected PM marker in 12-hour '$formatted12'", formatted12.contains("PM", ignoreCase = true))
    }

    @Test
    fun missedCallNotificationChannelUsesLocalizedStrings() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val esCtx = localizedContext(base, "es")
        val nm = esCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        MissedCallNotificationReceiver.ensureMissedCallChannel(esCtx, nm)
        val channel = nm.getNotificationChannel(MyInCallService.CHANNEL_MISSED)
        assertNotNull(channel)
        assertEquals(esCtx.getString(R.string.notif_channel_missed_name), channel?.name?.toString())
        assertEquals(esCtx.getString(R.string.notif_channel_missed_desc), channel?.description)
    }
}
