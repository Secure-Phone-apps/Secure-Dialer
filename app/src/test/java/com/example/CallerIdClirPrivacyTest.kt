package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.DialerViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallerIdClirPrivacyTest {

    private lateinit var context: Context
    private lateinit var application: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun testClirNumberFormattingStandardGsmPrefix() {
        val rawNumber = "+1 (555) 019-2834"
        val formatted = CallManager.formatOutgoingNumberWithClir(rawNumber, isHideCallerId = true, clirPrefix = "#31#")
        assertEquals("#31#+1 (555) 019-2834", formatted)
    }

    @Test
    fun testClirNumberFormattingNorthAmericaPrefix() {
        val rawNumber = "5551234"
        val formatted = CallManager.formatOutgoingNumberWithClir(rawNumber, isHideCallerId = true, clirPrefix = "*67")
        assertEquals("*675551234", formatted)
    }

    @Test
    fun testClirNumberFormattingUkPrefix() {
        val rawNumber = "07123456789"
        val formatted = CallManager.formatOutgoingNumberWithClir(rawNumber, isHideCallerId = true, clirPrefix = "141")
        assertEquals("14107123456789", formatted)
    }

    @Test
    fun testClirNumberFormattingJapanPrefix() {
        val rawNumber = "09012345678"
        val formatted = CallManager.formatOutgoingNumberWithClir(rawNumber, isHideCallerId = true, clirPrefix = "1831")
        assertEquals("183109012345678", formatted)
    }

    @Test
    fun testClirDisabledLeavesNumberUntouched() {
        val rawNumber = "+442079460912"
        val result = CallManager.formatOutgoingNumberWithClir(rawNumber, isHideCallerId = false, clirPrefix = "#31#")
        assertEquals(rawNumber, result)
    }

    @Test
    fun testAlreadyPrefixedNumberAvoidsDoublePrefixing() {
        val alreadyPrefixed = "#31#+15550192834"
        val result = CallManager.formatOutgoingNumberWithClir(alreadyPrefixed, isHideCallerId = true, clirPrefix = "#31#")
        assertEquals("#31#+15550192834", result)

        val usPrefixed = "*675551234"
        val usResult = CallManager.formatOutgoingNumberWithClir(usPrefixed, isHideCallerId = true, clirPrefix = "*67")
        assertEquals("*675551234", usResult)
    }

    @Test
    fun testEmergencyNumbersNeverPrefixed() {
        // Critical safety verification: emergency numbers must never have Caller ID suppression prepended
        val emergency911 = CallManager.formatOutgoingNumberWithClir("911", isHideCallerId = true, clirPrefix = "#31#")
        assertEquals("911", emergency911)

        val emergency112 = CallManager.formatOutgoingNumberWithClir("112", isHideCallerId = true, clirPrefix = "#31#")
        assertEquals("112", emergency112)
    }

    @Test
    fun testEmptyAndBlankNumbersHandledSafely() {
        val emptyResult = CallManager.formatOutgoingNumberWithClir("", isHideCallerId = true, clirPrefix = "#31#")
        assertEquals("", emptyResult)

        val blankResult = CallManager.formatOutgoingNumberWithClir("   ", isHideCallerId = true, clirPrefix = "#31#")
        assertEquals("   ", blankResult)
    }

    @Test
    fun testViewModelClirDefaultsAndToggles() {
        val viewModel = DialerViewModel(application)
        
        // Defaults check
        assertFalse(viewModel.isHideCallerIdEnabled.value)
        assertEquals("#31#", viewModel.clirPrefix.value)

        // Toggle enable
        viewModel.updateHideCallerIdEnabled(true)
        assertTrue(viewModel.isHideCallerIdEnabled.value)

        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("is_hide_caller_id_enabled", false))

        // Change prefix
        viewModel.updateClirPrefix("*67")
        assertEquals("*67", viewModel.clirPrefix.value)
        assertEquals("*67", prefs.getString("clir_prefix", null))

        // Toggle back off
        viewModel.updateHideCallerIdEnabled(false)
        assertFalse(viewModel.isHideCallerIdEnabled.value)
        assertFalse(prefs.getBoolean("is_hide_caller_id_enabled", true))
    }

    @Test
    fun testCustomPrefixSanitization() {
        val viewModel = DialerViewModel(application)
        
        // Blank input should fallback safely to #31#
        viewModel.updateClirPrefix("   ")
        assertEquals("#31#", viewModel.clirPrefix.value)

        // Valid custom carrier code
        viewModel.updateClirPrefix("  #31#  ")
        assertEquals("#31#", viewModel.clirPrefix.value)
    }
}
